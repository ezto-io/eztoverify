package io.ezto.verify.fido

import android.app.Activity
import android.content.Context
import androidx.credentials.*
import androidx.credentials.exceptions.*
import com.google.android.gms.common.util.Base64Utils
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FidoPlugin : FlutterPlugin, ActivityAware, FidoApi {
    private lateinit var context: Context;
    private var activity: Activity? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        FidoApi.setUp(flutterPluginBinding.binaryMessenger, this)
        context = flutterPluginBinding.applicationContext;
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        FidoApi.setUp(binding.binaryMessenger, null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun registrationFido(
        request: FidoRegRequest,
        callback: (Result<FidoRegResponse>) -> Unit
    ) {
        println("Register fido starting point in native")
        val challengeBase64 = Base64Utils.encodeUrlSafe(request.challenge)
        val idBase64 = Base64Utils.encodeUrlSafe(request.userId)
        val jsonData = """
                {
                  "challenge": "$challengeBase64",
                  "rp": {
                      "name": "${request.rpEntityName}",
                      "id": "${request.rpEntityId}"
                  },
                  "user": {
                      "id": "$idBase64",
                      "name": "${request.userName}",
                      "displayName": "${request.userDisplayName}"
                  },
                  "pubKeyCredParams": ${request.pubKeyCredParams},
                  "timeout": 1800000,
                  "attestation": "none",
                  "excludeCredentials": [],
                  "authenticatorSelection": {
                    "authenticatorAttachment": "platform",
                    "requireResidentKey": true,
                    "residentKey": "required",
                    "userVerification": "required"
                  }
                }
                """
        registerFido(jsonData, callback)
    }

    override fun verifyFido(
        request: FidoVerifyRequest,
        callback: (Result<FidoVerifyResponse>) -> Unit
    ) {
        val challengeBase64 = Base64Utils.encodeUrlSafe(request.challenge)
        val allowed = request.allowCredentials.map {
            val result = """
                {
                    "id":"${it?.id}",
                    "type":"${it?.type}"
                }
            """.trimIndent()
            print(result)
            result
        }.toList()

        val jsonData = """
                {
                  "challenge": "$challengeBase64",
                  "allowCredentials": ${allowed},
                  "timeout": 1800000,
                  "userVerification": "required",
                  "rpId": "${request.rpID}"
                }
                """
        verificationFido(jsonData, callback)
    }


    private fun registerFido(requestJson: String, callback: (Result<FidoRegResponse>) -> Unit) {
        println(requestJson)
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
            requestJson = requestJson,
            preferImmediatelyAvailableCredentials = false,
        )
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentialManager = CredentialManager.create(activity!!)
                val response = credentialManager.createCredential(
                    context = activity!!,
                    request = createPublicKeyCredentialRequest,
                )
                val jsonData = response.data
                val getFinalResponse =
                    jsonData.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")
                val successData = FidoRegResponse(
                    success = true,
                    message = "Registration Success",
                    data = getFinalResponse
                )
                callback(
                    Result.success(successData)
                )
            } catch (e: Exception) {
                val message: String? = if (e is CreateCredentialException) {
                    "${e.errorMessage}"
                } else {
                    e.message
                }
                println(message)
                val failedData = FidoRegResponse(
                    success = false,
                    message = "$message",
                    errorCode = "fidoRegFailed"
                )
                callback(Result.success(failedData))

            }
        }
    }

    private fun verificationFido(
        requestJson: String,
        callback: (Result<FidoVerifyResponse>) -> Unit
    ) {
        println(requestJson)
        val getPasswordOption = GetPasswordOption()
        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
            requestJson = requestJson
        )
        val getCredRequest = GetCredentialRequest(
            listOf(getPasswordOption, getPublicKeyCredentialOption)
        )
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentialManager = CredentialManager.create(activity!!)
                val response = credentialManager.getCredential(
                    context = activity!!,
                    request = getCredRequest
                )
                println(response.credential.data)
                val jsonData = response.credential.data
                val getFinalResponse =
                    jsonData.getString("androidx.credentials.BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON")
                val successData = FidoVerifyResponse(
                    success = true,
                    message = "Verify Success",
                    data = getFinalResponse
                )
                callback(
                    Result.success(successData)
                )

            } catch (e: GetCredentialException) {
                println(e.errorMessage)
                val failedData = FidoVerifyResponse(
                    success = false,
                    message = "${e.errorMessage}",
                    data = null
                )
                callback(
                    Result.success(failedData)
                )
            }
        }
    }


}