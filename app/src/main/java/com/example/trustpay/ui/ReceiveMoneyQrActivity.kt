package com.example.trustpay.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.trustpay.R
import com.example.trustpay.firebase.FirebaseRepository
import com.example.trustpay.model.QrPaymentRequest
import com.example.trustpay.model.QrStatus
import com.example.trustpay.security.QrProtocolHelper
import kotlinx.coroutines.launch

class ReceiveMoneyQrActivity : AppCompatActivity() {

    private val repository = FirebaseRepository()
    private var currentToken: QrPaymentRequest? = null
    private var tokenListener: FirebaseRepository.ListenerToken? = null
    private var countDownTimer: CountDownTimer? = null

    // UI elements
    private lateinit var etAmount: EditText
    private lateinit var etNote: EditText
    private lateinit var btnGenerate: Button
    private lateinit var ivQrCode: ImageView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvTimer: TextView
    private lateinit var progressBarTimer: ProgressBar
    private lateinit var tvTokenId: TextView
    private lateinit var tvReceiverInfo: TextView
    private lateinit var tvPaymentAlert: TextView
    private lateinit var tvDisclaimer: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Note: For ViewBinding or standard setContentView
        setContentView(R.layout.activity_receive_money_qr)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etAmount = findViewById(R.id.etAmount)
        etNote = findViewById(R.id.etNote)
        btnGenerate = findViewById(R.id.btnGenerate)
        ivQrCode = findViewById(R.id.ivQrCode)
        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        tvTimer = findViewById(R.id.tvTimer)
        progressBarTimer = findViewById(R.id.progressBarTimer)
        tvTokenId = findViewById(R.id.tvTokenId)
        tvReceiverInfo = findViewById(R.id.tvReceiverInfo)
        tvPaymentAlert = findViewById(R.id.tvPaymentAlert)
        tvDisclaimer = findViewById(R.id.tvDisclaimer)

        tvDisclaimer.text = "Simulation only - no real money transfer"
    }

    private fun setupListeners() {
        btnGenerate.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()
            val amount = if (amountStr.isNotEmpty()) amountStr.toDoubleOrNull() else null
            val note = etNote.text.toString().trim()

            generateAndRotateQr(amount, note)
        }
    }

    private fun generateAndRotateQr(amount: Double?, note: String) {
        countDownTimer?.cancel()
        tokenListener?.remove()

        updateStatus(QrStatus.REFRESHING)

        lifecycleScope.launch {
            try {
                val token = repository.createQrPaymentRequest(amount, note)
                currentToken = token
                displayQrToken(token)
                startRotationCountdown(token, amount, note)
                listenForPayment(token.tokenId)
            } catch (e: Exception) {
                Toast.makeText(this@ReceiveMoneyQrActivity, "Failed to generate QR: ${e.message}", Toast.LENGTH_SHORT).show()
                updateStatus(QrStatus.EXPIRED)
            }
        }
    }

    private fun displayQrToken(token: QrPaymentRequest) {
        val bitmap = QrProtocolHelper.generateQrBitmap(token.payloadUri, 600)
        ivQrCode.setImageBitmap(bitmap)
        ivQrCode.visibility = View.VISIBLE

        tvTokenId.text = "Token: ${token.tokenId}"
        tvReceiverInfo.text = "Pay to: ${token.receiverName} (${if (token.amount != null) QrProtocolHelper.formatCurrency(token.amount) else "Any Amount"})"

        updateStatus(QrStatus.ACTIVE)
        tvPaymentAlert.visibility = View.GONE
    }

    private fun startRotationCountdown(token: QrPaymentRequest, amount: Double?, note: String) {
        val ttlMillis = 30_000L
        progressBarTimer.max = 30

        countDownTimer = object : CountDownTimer(ttlMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                tvTimer.text = "Rotates in: ${seconds}s"
                progressBarTimer.progress = seconds
            }

            override fun onFinish() {
                tvTimer.text = "Rotating new QR..."
                updateStatus(QrStatus.EXPIRED)
                // Auto-rotate automatically every 30 seconds
                generateAndRotateQr(amount, note)
            }
        }.start()
    }

    private fun listenForPayment(tokenId: String) {
        tokenListener = repository.listenToTokenUpdates(tokenId) { updatedToken ->
            if (updatedToken.status == QrStatus.USED.name) {
                countDownTimer?.cancel()
                updateStatus(QrStatus.USED)
                tvPaymentAlert.text = "Payment Received! ${if (updatedToken.amount != null) QrProtocolHelper.formatCurrency(updatedToken.amount) else ""} from ${updatedToken.usedBySenderName ?: "Sender"}"
                tvPaymentAlert.visibility = View.VISIBLE
            }
        }
    }

    private fun updateStatus(status: QrStatus) {
        tvStatusBadge.text = status.name
        when (status) {
            QrStatus.ACTIVE -> tvStatusBadge.setBackgroundColor(0xFF10B981.toInt()) // Emerald
            QrStatus.EXPIRED -> tvStatusBadge.setBackgroundColor(0xFFEF4444.toInt()) // Red
            QrStatus.USED -> tvStatusBadge.setBackgroundColor(0xFF8B5CF6.toInt()) // Purple
            QrStatus.REFRESHING -> tvStatusBadge.setBackgroundColor(0xFF06B6D4.toInt()) // Cyan
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        tokenListener?.remove()
    }
}
