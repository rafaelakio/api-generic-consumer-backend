package com.apiconsumer.infrastructure.adapter.output.http

import com.apiconsumer.domain.model.ApiRequest
import com.apiconsumer.domain.model.ApiResponse
import com.apiconsumer.domain.model.ContentType
import com.apiconsumer.domain.model.HttpMethod
import com.apiconsumer.domain.port.output.HttpClientPort
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.EMPTY_REQUEST
import org.springframework.stereotype.Component
import java.util.Base64
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory

@Component
class OkHttpClientAdapter(
    private val defaultClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
) : HttpClientPort {

    override fun execute(request: ApiRequest, authorizationHeader: String?): ApiResponse {
        val start = System.currentTimeMillis()

        return try {
            val client = buildClient(request)
            val okRequest = buildRequest(request, authorizationHeader)
            val response = client.newCall(okRequest).execute()

            val responseHeaders: Map<String, String> = response.headers.names()
                .associateWith { response.headers(it).joinToString(", ") }
            val bodyStr = response.body?.string() ?: ""
            val parsedBody: Any? = try {
                objectMapper.readValue(bodyStr, Any::class.java)
            } catch (_: Exception) {
                bodyStr
            }

            ApiResponse(
                statusCode = response.code,
                statusText = response.message,
                headers = responseHeaders,
                body = parsedBody,
                responseTimeMs = System.currentTimeMillis() - start,
            )
        } catch (ex: Exception) {
            ApiResponse(
                statusCode = 0,
                statusText = "Request Failed",
                headers = emptyMap(),
                body = null,
                responseTimeMs = System.currentTimeMillis() - start,
                error = ex.message ?: "Unknown error",
            )
        }
    }

    private fun buildClient(request: ApiRequest): OkHttpClient {
        if (!request.bypassSsl && request.certificate == null) return defaultClient

        return if (request.bypassSsl) {
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
            })
            val sslContext = SSLContext.getInstance("TLS").also { it.init(null, trustAll, null) }
            defaultClient.newBuilder()
                .sslSocketFactory(sslContext.socketFactory, trustAll[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } else {
            val certBytes = request.certificate!!.toByteArray()
            val cf = CertificateFactory.getInstance("X.509")
            val cert = cf.generateCertificate(ByteArrayInputStream(certBytes))
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).also {
                it.load(null, null)
                it.setCertificateEntry("custom-ca", cert)
            }
            val tmf = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(keyStore)
            val sslContext = SSLContext.getInstance("TLS").also { it.init(null, tmf.trustManagers, null) }
            defaultClient.newBuilder()
                .sslSocketFactory(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
                .build()
        }
    }

    private fun buildRequest(apiRequest: ApiRequest, authorizationHeader: String?): Request {
        val builder = Request.Builder().url(apiRequest.url)

        apiRequest.headers
            .filter { it.enabled && it.key.isNotBlank() }
            .forEach { builder.addHeader(it.key.trim(), it.value) }

        authorizationHeader?.let { builder.header("Authorization", it) }

        val body = buildBody(apiRequest)
        when (apiRequest.method) {
            HttpMethod.GET    -> builder.get()
            HttpMethod.DELETE -> builder.delete(body)
            HttpMethod.POST   -> builder.post(body ?: EMPTY_REQUEST)
            HttpMethod.PUT    -> builder.put(body ?: EMPTY_REQUEST)
            HttpMethod.PATCH  -> builder.patch(body ?: EMPTY_REQUEST)
        }

        return builder.build()
    }

    private fun buildBody(request: ApiRequest): RequestBody? {
        if (request.method == HttpMethod.GET) return null

        return when (request.contentType) {
            ContentType.JSON -> {
                val raw = request.body?.trim()
                if (raw.isNullOrEmpty()) null
                else raw.toRequestBody("application/json".toMediaType())
            }
            ContentType.RAW -> {
                request.body?.toRequestBody("text/plain".toMediaType())
            }
            ContentType.X_WWW_FORM_URLENCODED -> {
                FormBody.Builder().also { fb ->
                    request.formData?.forEach { (k, v) -> fb.add(k, v) }
                }.build()
            }
            ContentType.FORM_DATA -> {
                MultipartBody.Builder().setType(MultipartBody.FORM).also { mb ->
                    request.formData?.forEach { (k, v) -> mb.addFormDataPart(k, v) }
                    request.files?.forEach { file ->
                        val bytes = Base64.getDecoder().decode(file.content)
                        val mediaType = file.type.toMediaTypeOrNull() ?: "application/octet-stream".toMediaType()
                        mb.addFormDataPart(
                            file.fieldName ?: "file",
                            file.name,
                            bytes.toRequestBody(mediaType),
                        )
                    }
                }.build()
            }
        }
    }
}
