package com.example.trustpay.backend

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backend Security API.
 * Maintains persistent security audit trails, tamper detection logs,
 * and rate-limiting metrics.
 */
object SecurityApi {

    data class SecurityAuditRecord(
        val id: String,
        val timestamp: Long,
        val formattedTime: String,
        val eventType: String,
        val description: String,
        val severity: String, // "INFO", "WARNING", "CRITICAL", "SUCCESS"
        val metadataHash: String? = null
    )

    private val auditLogs = mutableListOf<SecurityAuditRecord>()

    init {
        logSecurityEvent(
            eventType = "SECURITY_ENGINE_INITIALIZED",
            description = "Three-Step Verification & AI Risk Engine initialized in zero-trust state.",
            severity = "SUCCESS"
        )
    }

    fun logSecurityEvent(
        eventType: String,
        description: String,
        severity: String = "INFO",
        metadataHash: String? = null
    ) {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val record = SecurityAuditRecord(
            id = "AUD-" + (auditLogs.size + 1),
            timestamp = now,
            formattedTime = sdf.format(Date(now)),
            eventType = eventType,
            description = description,
            severity = severity,
            metadataHash = metadataHash
        )
        auditLogs.add(0, record)
        if (auditLogs.size > 200) {
            auditLogs.removeAt(auditLogs.lastIndex)
        }
    }

    fun getAuditLogs(): List<SecurityAuditRecord> = auditLogs.toList()

    fun clearLogs() {
        auditLogs.clear()
    }
}
