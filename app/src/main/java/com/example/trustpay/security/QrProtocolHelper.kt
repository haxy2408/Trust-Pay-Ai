package com.example.trustpay.security

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.Locale

object QrProtocolHelper {
    const val URI_SCHEME = "trustpay"
    const val URI_HOST = "p2p"
    const val URI_PATH = "/request"
    const val CURRENT_VERSION = 1

    /**
     * Builds the standard TrustPay payment-request URI:
     * trustpay://p2p/request?tokenId=...&expiresAt=...&version=1
     */
    fun buildPayloadUri(tokenId: String, expiresAt: Long): String {
        return "$URI_SCHEME://$URI_HOST$URI_PATH?tokenId=$tokenId&expiresAt=$expiresAt&version=$CURRENT_VERSION"
    }

    /**
     * Parses and validates a TrustPay QR payload URI.
     */
    fun parsePayloadUri(uriString: String): Map<String, String>? {
        return try {
            val uri = URI(uriString)
            if (uri.scheme != URI_SCHEME || uri.host != URI_HOST) {
                return null
            }
            val query = uri.query ?: return null
            val params = mutableMapOf<String, String>()
            for (pair in query.split("&")) {
                val parts = pair.split("=")
                if (parts.size == 2) {
                    val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name())
                    val value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
                    params[key] = value
                }
            }
            if (params.containsKey("tokenId")) params else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes the tamper-evident binding hash:
     * SHA-256("$transactionId|$recipient|$amount|$currency")
     */
    fun computeBindingHash(
        transactionId: String,
        recipient: String,
        amount: Double,
        currency: String = "INR"
    ): String {
        val raw = "$transactionId|$recipient|${String.format(Locale.US, "%.2f", amount)}|$currency"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(raw.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Formats Indian Rupee currency.
     */
    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return format.format(amount)
    }

    /**
     * Generates a monochrome QR Bitmap without storing any secrets locally.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name()
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
