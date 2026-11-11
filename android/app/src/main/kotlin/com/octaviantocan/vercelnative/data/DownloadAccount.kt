package com.octaviantocan.vercelnative.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.octaviantocan.vercelnative.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.TimeUnit

val AppJson = Json { ignoreUnknownKeys = true }

/** Existing Inventory account/session protocol, with this app's own encrypted token storage. */
class DownloadAccount(context: Context) {
    val session = Session(KeystoreTokenStore(context))
    private val origin = BuildConfig.DOWNLOADS_ORIGIN.toHttpUrl()
    val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(5, TimeUnit.MINUTES)
        .followRedirects(false).followSslRedirects(false)
        .addInterceptor { chain ->
            val request = chain.request()
            val allowed = request.url.scheme == origin.scheme && request.url.host == origin.host &&
                request.url.port == origin.port && request.url.encodedPath.startsWith("/download/vercel/")
            val token = session.token?.takeIf { allowed }
            val answer = chain.proceed(if (token == null) request else request.newBuilder().header("Authorization", "Bearer $token").build())
            if (answer.code == 401 && token != null) session.invalidate(token)
            answer
        }.build()

    private suspend fun post(path: String, body: JsonObject): JsonObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(BuildConfig.DOWNLOADS_ORIGIN + path)
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException(if (response.code == 401) "Sign-in was not accepted" else "Sign-in server answered ${response.code}")
            AppJson.parseToJsonElement(response.body.string()).jsonObject
        }
    }

    suspend fun google(context: Context) {
        check(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) { "Google sign-in is not configured in this build" }
        val nonce = post("/app/v1/auth/nonce", buildJsonObject {})["nonce"]!!.jsonPrimitive.content
        val option = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID).setNonce(nonce).build()
        val credential = CredentialManager.create(context)
            .getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "Google did not return an account" }
        val id = GoogleIdTokenCredential.createFrom(credential.data)
        val result = post("/app/v1/auth/google", buildJsonObject {
            put("idToken", id.idToken); put("nonce", nonce); put("device", "Vercel Native")
        })
        session.signIn(result["token"]!!.jsonPrimitive.content)
    }

    suspend fun password(username: String, password: String) {
        val result = post("/app/v1/auth/password", buildJsonObject {
            put("username", username.trim()); put("password", password); put("device", "Vercel Native")
        })
        session.signIn(result["token"]!!.jsonPrimitive.content)
    }
}
