package com.blackchain.adapters

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Request.Companion.invoke
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Constants
private const val HMAC_SHA_256 = "HmacSHA256"
private const val EQUALS_SIGN_STRING = "?"
private const val EQUALS_STRING = "="
private const val AND_STRING = "&"

private const val BINANCE_API_KEY = "BINANCE_API_KEY"
private const val BINANCE_API_SECRET = "BINANCE_API_SECRET"
private const val BINANCE_BASE_URL = "https://api.binance.com"

private val binanceApiSecret = loadProperty(BINANCE_API_SECRET)
private val binanceApiKey = loadProperty(BINANCE_API_KEY)

fun addQueryParams(queryParams: Map<String, String>?): String {
    val sb = StringBuffer()
    if (!queryParams.isNullOrEmpty()) {
        sb.append(EQUALS_SIGN_STRING)
        sb.append(queryParams.entries.stream()
            .map { (key, value): Map.Entry<String, String> -> key + EQUALS_STRING + value }
            .collect(Collectors.joining(AND_STRING)))
    }
    return sb.toString()
}

fun buildSignature(secret: String, message: String): String {
    val secretKey: ByteArray = secret.toByteArray(StandardCharsets.UTF_8)
    return try {
        val mac = Mac.getInstance(HMAC_SHA_256)
        val secretKeySpec = SecretKeySpec(secretKey, HMAC_SHA_256)
        mac.init(secretKeySpec)
        bytesToHex(mac.doFinal(message.toByteArray(StandardCharsets.UTF_8)))
    } catch (e: Exception) {
        throw RuntimeException("Failed to calculate hmac-sha256", e)
    }
}


fun loadProperty(propertyName: String): String {
    val envVars = loadEnvironmentVariables()
    return envVars[propertyName] ?: throw Exception("Missing variable $propertyName")

}

fun loadEnvironmentVariables(): Map<String, String> {
    val envVars = System.getenv().toMutableMap()

    // Check if we are running locally (check for .env file)
    val envFile = File(".env")
    if (envFile.exists()) {
        envFile.forEachLine { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                val (key, value) = trimmedLine.split("=")
                envVars[key.trim()] = value.trim()
            }
        }
    }

    return envVars
}

// Helper function to convert bytes to hex (replace your Hex.hex() function)
fun bytesToHex(bytes: ByteArray): String {
    return bytes.joinToString("") { "%02x".format(it) }
}

fun buildRequest(
    method: Method,
    requestPath: String,
    requestParams: MutableMap<String, String>
): Request {
    val timestamp = System.currentTimeMillis().toString()
    requestParams["timestamp"] = timestamp

    val message = addQueryParams(requestParams).replace("?", "")
    val signature = buildSignature(binanceApiSecret, message)
    requestParams["signature"] = signature

    val requestUrl = BINANCE_BASE_URL + requestPath + addQueryParams(requestParams)

    return Request(method, requestUrl)
        .header("Content-Type", "application/json; charset=utf-8")
        .header("X-MBX-APIKEY", binanceApiKey)
}

fun buildPublicRequest(method: Method, fullRequestURL: String, requestParams: MutableMap<String, String>): Request {
    val requestUrl = fullRequestURL + addQueryParams(requestParams)

    return Request(method, requestUrl)
        .header("Content-Type", "application/json; charset=utf-8")
        .header("X-MBX-APIKEY", binanceApiKey)
}