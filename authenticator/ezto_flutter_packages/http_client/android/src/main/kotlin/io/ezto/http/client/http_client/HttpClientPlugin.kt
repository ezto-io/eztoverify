package io.ezto.http.client.http_client

import HttpClientResponse
import HttpNativeClientHostApi
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.CoroutineContext

/** HttpClientPlugin */
class HttpClientPlugin : FlutterPlugin, HttpNativeClientHostApi {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private val okHttpClient: OkHttpClient = OkHttpClient()


    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        HttpNativeClientHostApi.setUp(flutterPluginBinding.binaryMessenger, this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        HttpNativeClientHostApi.setUp(binding.binaryMessenger, null)
    }

    override fun getUrl(url: String, callback: (kotlin.Result<HttpClientResponse>) -> Unit) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(url).get().build()
                okHttpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        callback(kotlin.Result.failure(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        callback(
                            kotlin.Result.success(
                                HttpClientResponse(
                                    statusCode = response.code.toLong(),
                                    body = response.body?.string() ?: ""
                                )
                            )
                        )
                    }

                })
            } catch (e: Exception) {
                callback(kotlin.Result.failure(e))
            }

        }
    }


}
