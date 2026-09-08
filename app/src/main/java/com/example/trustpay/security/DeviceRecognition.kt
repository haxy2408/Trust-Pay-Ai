package com.example.trustpay.security

import android.content.Context
import android.os.Build
import java.security.MessageDigest
import java.util.UUID

/**
 * STEP 1 — Device Recognition Security Module.
 *
 * Requirements:
 * - Detect whether current device is previously registered.
 * - Generate & store secure device identifier on the backend/storage.
 * - Associate registered device with user's account.
 * - Detect new / unrecognized device.
 * - If unrecognized, do NOT immediately approve transaction; trigger warnings and step-up auth.
 * - Provide a demo "Register This Device" flow.
 * - Minimize collecting unnecessary device data.
 * - Never use device recognition as the single authentication factor.
 */
object DeviceRecognition {

    enum class DeviceStatus {
        REGISTERED_DEVICE,
        NEW_DEVICE,
        UNKNOWN_DEVICE,
        DEVICE_VERIFICATION_FAILURE
    }

    data class DeviceRecognitionResult(
        val deviceVerified: Boolean,
        val deviceStatus: String, // "REGISTERED", "NEW_DEVICE", "UNKNOWN_DEVICE", or "VERIFICATION_FAILURE"
        val statusEnum: DeviceStatus,
        val deviceId: String,
        val deviceName: String,
        val riskLevel: String, // "LOW", "MEDIUM", "HIGH"
        val registeredDeviceId: String? = null,
        val warningMessage: String? = null
    )

    // Simulated flag allowing the demo tester to toggle "unrecognized device" simulation
    private var simulatedUnrecognizedMode: Boolean = false

    fun setSimulateUnrecognizedDevice(simulate: Boolean) {
        simulatedUnrecognizedMode = simulate
    }

    fun isSimulatingUnrecognizedDevice(): Boolean = simulatedUnrecognizedMode

    /**
     * Derives a deterministic, privacy-preserving device identifier based on hardware signature.
     * Does not collect PII (Personally Identifiable Information).
     */
    fun getHardwareDeviceId(): String {
        return try {
            val raw = "${Build.MANUFACTURER}-${Build.MODEL}-${Build.BOARD}"
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            val shortHash = digest.take(4).joinToString("") { "%02X".format(it) }
            "DEV-${Build.MANUFACTURER.uppercase().take(5)}-$shortHash"
        } catch (e: Exception) {
            "DEV-UNKNOWN-0000"
        }
    }

    fun getDeviceDisplayName(): String {
        return "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }

    /**
     * Evaluates device recognition against the user's registered device account data.
     * Correctly handles REGISTERED DEVICE, NEW DEVICE, UNKNOWN DEVICE, and VERIFICATION FAILURE.
     */
    fun evaluateDevice(userRegisteredDeviceId: String?): DeviceRecognitionResult {
        val currentHardwareId = getHardwareDeviceId()
        val displayName = getDeviceDisplayName()

        if (currentHardwareId == "DEV-UNKNOWN-0000") {
            return DeviceRecognitionResult(
                deviceVerified = false,
                deviceStatus = "VERIFICATION_FAILURE",
                statusEnum = DeviceStatus.DEVICE_VERIFICATION_FAILURE,
                deviceId = currentHardwareId,
                deviceName = displayName,
                riskLevel = "HIGH",
                registeredDeviceId = userRegisteredDeviceId,
                warningMessage = "Device verification failure: unable to reliably compute hardware signature."
            )
        }

        if (simulatedUnrecognizedMode) {
            return DeviceRecognitionResult(
                deviceVerified = false,
                deviceStatus = "NEW_DEVICE",
                statusEnum = DeviceStatus.NEW_DEVICE,
                deviceId = "DEV-ROGUE-ANON-9901",
                deviceName = "Untrusted Mobile Hardware (Simulated Rogue Client)",
                riskLevel = "HIGH",
                registeredDeviceId = userRegisteredDeviceId,
                warningMessage = "SECURITY WARNING: Transaction attempted from an unrecognized new device signature. Additional step-up verification required."
            )
        }

        return when {
            userRegisteredDeviceId == null -> {
                DeviceRecognitionResult(
                    deviceVerified = false,
                    deviceStatus = "UNKNOWN_DEVICE",
                    statusEnum = DeviceStatus.UNKNOWN_DEVICE,
                    deviceId = currentHardwareId,
                    deviceName = displayName,
                    riskLevel = "MEDIUM",
                    registeredDeviceId = null,
                    warningMessage = "Unknown device: No device registered on account. Registration recommended."
                )
            }
            userRegisteredDeviceId == currentHardwareId -> {
                DeviceRecognitionResult(
                    deviceVerified = true,
                    deviceStatus = "REGISTERED",
                    statusEnum = DeviceStatus.REGISTERED_DEVICE,
                    deviceId = currentHardwareId,
                    deviceName = displayName,
                    riskLevel = "LOW",
                    registeredDeviceId = userRegisteredDeviceId,
                    warningMessage = null
                )
            }
            else -> {
                DeviceRecognitionResult(
                    deviceVerified = false,
                    deviceStatus = "NEW_DEVICE",
                    statusEnum = DeviceStatus.NEW_DEVICE,
                    deviceId = currentHardwareId,
                    deviceName = displayName,
                    riskLevel = "HIGH",
                    registeredDeviceId = userRegisteredDeviceId,
                    warningMessage = "SECURITY WARNING: New unrecognized device detected (Registered: $userRegisteredDeviceId, Detected: $currentHardwareId). Step-up verification mandated."
                )
            }
        }
    }
}
