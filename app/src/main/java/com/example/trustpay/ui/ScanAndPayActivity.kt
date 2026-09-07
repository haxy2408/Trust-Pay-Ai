package com.example.trustpay.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.trustpay.R
import com.example.trustpay.firebase.FirebaseRepository
import com.example.trustpay.model.QrPaymentRequest
import com.example.trustpay.security.QrProtocolHelper
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class ScanAndPayActivity : AppCompatActivity() {

    private val repository = FirebaseRepository()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var isScanningActive = true

    // UI elements
    private lateinit var previewView: PreviewView
    private lateinit var tvScanStatus: TextView
    private lateinit var layoutPaymentConfirm: View
    private lateinit var tvReceiverName: TextView
    private lateinit var etPayAmount: EditText
    private lateinit var tvNote: TextView
    private lateinit var tvRemainingTtl: TextView
    private lateinit var btnConfirmPay: Button
    private lateinit var btnCancelPay: Button
    private lateinit var tvDisclaimer: TextView

    private var validatedToken: QrPaymentRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_and_pay)

        initViews()
        checkCameraPermissionAndStart()
    }

    private fun initViews() {
        previewView = findViewById(R.id.previewView)
        tvScanStatus = findViewById(R.id.tvScanStatus)
        layoutPaymentConfirm = findViewById(R.id.layoutPaymentConfirm)
        tvReceiverName = findViewById(R.id.tvReceiverName)
        etPayAmount = findViewById(R.id.etPayAmount)
        tvNote = findViewById(R.id.tvNote)
        tvRemainingTtl = findViewById(R.id.tvRemainingTtl)
        btnConfirmPay = findViewById(R.id.btnConfirmPay)
        btnCancelPay = findViewById(R.id.btnCancelPay)
        tvDisclaimer = findViewById(R.id.tvDisclaimer)

        tvDisclaimer.text = "Simulation only - no real money transfer"

        btnConfirmPay.setOnClickListener {
            handleBiometricAndPay()
        }

        btnCancelPay.setOnClickListener {
            resetToScanning()
        }
    }

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val scanner = BarcodeScanning.getClient()

            imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null && isScanningActive) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                    val rawValue = barcode.rawValue
                                    if (rawValue != null && rawValue.startsWith("trustpay://p2p/request")) {
                                        isScanningActive = false
                                        runOnUiThread { handleScannedUri(rawValue) }
                                        break
                                    }
                                }
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to bind camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleScannedUri(uriString: String) {
        val params = QrProtocolHelper.parsePayloadUri(uriString)
        val tokenId = params?.get("tokenId")
        if (tokenId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid TrustPay QR Code format.", Toast.LENGTH_SHORT).show()
            isScanningActive = true
            return
        }

        tvScanStatus.text = "Validating token with Cloud Functions..."

        lifecycleScope.launch {
            val result = repository.validateQrToken(tokenId)
            if (result.valid && result.request != null) {
                validatedToken = result.request
                showPaymentConfirmation(result.request, result.remainingSeconds)
            } else {
                showSecurityAlert("Validation Failed", result.error ?: "QR code validation rejected.")
                resetToScanning()
            }
        }
    }

    private fun showPaymentConfirmation(token: QrPaymentRequest, remainingSeconds: Long) {
        layoutPaymentConfirm.visibility = View.VISIBLE
        tvReceiverName.text = "Recipient: ${token.receiverName} (${token.receiverId})"
        tvNote.text = "Note: ${token.note.ifEmpty { "None" }}"
        tvRemainingTtl.text = "Window: ${remainingSeconds}s remaining"

        if (token.amount != null && token.amount > 0) {
            etPayAmount.setText(token.amount.toString())
            etPayAmount.isEnabled = false
        } else {
            etPayAmount.setText("")
            etPayAmount.isEnabled = true
            etPayAmount.hint = "Enter amount in ₹"
        }

        tvScanStatus.text = "QR Verified. Authorize payment below."
    }

    private fun handleBiometricAndPay() {
        val amount = etPayAmount.text.toString().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("TrustPay Payment Authorization")
            .setSubtitle("Confirm transfer of ${QrProtocolHelper.formatCurrency(amount)}")
            .setDescription("Scan your biometric credential to bind the transaction payload.")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    executeTransfer(amount)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@ScanAndPayActivity, "Biometric failed: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    private fun executeTransfer(amount: Double) {
        val token = validatedToken ?: return
        val txId = "TP_TX_${System.currentTimeMillis()}"
        val bindingHash = QrProtocolHelper.computeBindingHash(txId, token.receiverId, amount, "INR")

        tvScanStatus.text = "Processing transfer via Cloud Functions..."

        lifecycleScope.launch {
            try {
                val transaction = repository.completeP2PTransfer(token.tokenId, amount, bindingHash)
                showSuccessDialog(transaction.recipient, amount, bindingHash, transaction.id)
            } catch (e: Exception) {
                showSecurityAlert("Transfer Blocked", e.message ?: "Transfer execution failed.")
                resetToScanning()
            }
        }
    }

    private fun showSuccessDialog(receiver: String, amount: Double, hash: String, txId: String) {
        AlertDialog.Builder(this)
            .setTitle("Payment Successful")
            .setMessage("Transferred ${QrProtocolHelper.formatCurrency(amount)} to $receiver.\n\nTx ID: $txId\nBinding Hash: ${hash.substring(0, 16)}...\n\nSimulation only - no real money transfer.")
            .setPositiveButton("Done") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showSecurityAlert(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("$message\n\nSimulation only - no real money transfer.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun resetToScanning() {
        layoutPaymentConfirm.visibility = View.GONE
        tvScanStatus.text = "Align TrustPay QR code within the frame"
        validatedToken = null
        isScanningActive = true
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
