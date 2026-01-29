package com.example.myapplication.zoom

import android.content.Context
import android.util.Base64
import android.util.Log
import us.zoom.sdk.ZoomVideoSDK
import us.zoom.sdk.ZoomVideoSDKInitParams
import us.zoom.sdk.ZoomVideoSDKSession
import us.zoom.sdk.ZoomVideoSDKSessionContext
import us.zoom.sdk.ZoomVideoSDKAudioOption
import us.zoom.sdk.ZoomVideoSDKVideoOption
import us.zoom.sdk.ZoomVideoSDKErrors
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

object ZoomSDKHelper {
    private const val TAG = "ZoomSDKHelper"

    private var isInitialized = false

    /**
     * Initialize the Zoom Video SDK
     */
    fun initializeSDK(context: Context, domain: String = "zoom.us"): Boolean {
        if (isInitialized) {
            Log.d(TAG, "SDK already initialized")
            return true
        }

        val initParams = ZoomVideoSDKInitParams().apply {
            this.domain = domain
            this.logFilePrefix = "ZoomVideoSDK"
            this.enableLog = true
        }

        val sdk = ZoomVideoSDK.getInstance()
        val result = sdk.initialize(context, initParams)

        isInitialized = result == ZoomVideoSDKErrors.Errors_Success

        if (isInitialized) {
            Log.d(TAG, "Zoom SDK initialized successfully")
        } else {
            Log.e(TAG, "Failed to initialize Zoom SDK. Error code: $result")
        }

        return isInitialized
    }

    /**
     * Join a Zoom Video SDK session
     */
    fun joinSession(
        sessionName: String,
        displayName: String,
        sessionPassword: String,
        jwtToken: String,
        onSuccess: (ZoomVideoSDKSession) -> Unit,
        onError: (Int, String) -> Unit
    ) {
        if (!isInitialized) {
            onError(-1, "SDK not initialized")
            return
        }

        val audioOptions = ZoomVideoSDKAudioOption().apply {
            connect = false  // Don't auto-connect audio - user must manually enable
            mute = true  // Start muted when user does connect
        }

        val videoOptions = ZoomVideoSDKVideoOption().apply {
            localVideoOn = false  // Start with video off - user can enable when ready
        }

        val sessionContext = ZoomVideoSDKSessionContext().apply {
            this.sessionName = sessionName
            this.userName = displayName
            this.sessionPassword = sessionPassword
            this.token = jwtToken
            this.audioOption = audioOptions
            this.videoOption = videoOptions
        }

        val sdk = ZoomVideoSDK.getInstance()
        val session = sdk.joinSession(sessionContext)

        if (session != null) {
            Log.d(TAG, "Successfully joined session: $sessionName")
            onSuccess(session)
        } else {
            Log.e(TAG, "Failed to join session")
            onError(-2, "Failed to join session")
        }
    }

    /**
     * Leave the current session
     */
    fun leaveSession(shouldEndSession: Boolean = false) {
        val sdk = ZoomVideoSDK.getInstance()
        sdk.leaveSession(shouldEndSession)
        Log.d(TAG, "Left session")
    }

    /**
     * Check if SDK is initialized
     */
    fun isSDKInitialized(): Boolean = isInitialized

    /**
     * Get the current session
     */
    fun getCurrentSession(): ZoomVideoSDKSession? {
        return ZoomVideoSDK.getInstance().session
    }
}

/**
 * JWT Token Generator for Zoom Video SDK
 *
 * Note: In production, JWT tokens should be generated on a secure backend server.
 * This client-side implementation is for testing/development purposes only.
 */
object JWTGenerator {
    private const val TAG = "JWTGenerator"

    /**
     * Generate a JWT token for Zoom Video SDK
     *
     * @param sdkKey Your Zoom Video SDK Key
     * @param sdkSecret Your Zoom Video SDK Secret
     * @param sessionName The name of the session to join
     * @param roleType 1 for host, 0 for participant
     * @param expirationSeconds Token expiration time in seconds (default 2 hours)
     */
    fun generateJWT(
        sdkKey: String,
        sdkSecret: String,
        sessionName: String,
        roleType: Int = 1,
        expirationSeconds: Long = 7200
    ): String {
        try {
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            val expirationTime = currentTimeSeconds + expirationSeconds

            // JWT Header
            val header = JSONObject().apply {
                put("alg", "HS256")
                put("typ", "JWT")
            }

            // JWT Payload
            val payload = JSONObject().apply {
                put("app_key", sdkKey)
                put("tpc", sessionName)
                put("role_type", roleType)
                put("version", 1)
                put("iat", currentTimeSeconds)
                put("exp", expirationTime)
            }

            val headerBase64 = base64UrlEncode(header.toString().toByteArray())
            val payloadBase64 = base64UrlEncode(payload.toString().toByteArray())

            val signatureInput = "$headerBase64.$payloadBase64"
            val signature = hmacSha256(signatureInput, sdkSecret)
            val signatureBase64 = base64UrlEncode(signature)

            val jwt = "$headerBase64.$payloadBase64.$signatureBase64"
            Log.d(TAG, "JWT generated successfully")
            return jwt

        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate JWT: ${e.message}")
            throw e
        }
    }

    private fun base64UrlEncode(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun hmacSha256(data: String, secret: String): ByteArray {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val secretKeySpec = SecretKeySpec(secret.toByteArray(), algorithm)
        mac.init(secretKeySpec)
        return mac.doFinal(data.toByteArray())
    }
}
