package com.blackchain

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.http4k.client.OkHttp
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.http4k.core.*
import dev.forkhandles.result4k.*
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Constants
private const val BINANCE_API_KEY = "BINANCE_API_KEY"
private const val BINANCE_API_SECRET = "BINANCE_API_SECRET"
private const val BINANCE_BASE_URL = "https://api.binance.com"
private const val BINANCE_ORDER_PATH = "/api/v3/order"
private const val BINANCE_ORDERS_PATH = "/api/v3/allOrders"

private const val HMAC_SHA_256 = "HmacSHA256"
private const val EQUALS_SIGN_STRING = "?"
private const val EQUALS_STRING = "="
private const val AND_STRING = "&"

// Data classes
data class CreateOrderRequest(
    val symbol: String,
    val side: String, // BUY or SELL
    val type: String, // MARKET, LIMIT, etc.
    val timeInForce: String? = null, // GTC, IOC, FOK
    val quantity: String? = null,
    val quoteOrderQty: String? = null,
    val price: String? = null,
    val newClientOrderId: String? = null
)

data class BinanceOrderResponse(
    val symbol: String,
    val orderId: Long,
    val clientOrderId: String,
    val transactTime: Long,
    val price: String,
    val origQty: String,
    val executedQty: String,
    val status: String,
    val timeInForce: String?,
    val type: String,
    val side: String
)

sealed class CryptoTrackerError {
    data class BinanceError(val message: String) : CryptoTrackerError()
    data class ValidationError(val message: String) : CryptoTrackerError()
}

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

// Main Lambda Handler
class BinanceOrderHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val binanceClient = OkHttp()
    private val binanceService = BinanceService(binanceClient)

    override fun handleRequest(
        input: APIGatewayProxyRequestEvent,
        context: Context
    ): APIGatewayProxyResponseEvent {

        return try {
            when (input.httpMethod) {
                "POST" -> handleCreateOrder(input)
                "GET" -> handleGetOrders(input)
                else -> createErrorResponse(405, "Method not allowed")
            }
        } catch (e: Exception) {
            context.logger.log("Error: ${e.message}")
            createErrorResponse(500, "Internal server error: ${e.message}")
        }
    }

    private fun handleCreateOrder(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        val requestBody = input.body ?: return createErrorResponse(400, "Request body is required")

        val orderRequest = try {
            objectMapper.readValue(requestBody, CreateOrderRequest::class.java)
        } catch (e: Exception) {
            return createErrorResponse(400, "Invalid request format: ${e.message}")
        }

        return when (val result = binanceService.createOrder(orderRequest)) {
            is Success -> createSuccessResponse(result.value)
            is Failure -> createErrorResponse(400, result.reason.toString())
        }
    }

    private fun handleGetOrders(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        val symbol = input.queryStringParameters?.get("symbol")
            ?: return createErrorResponse(400, "Symbol parameter is required")

        return when (val result = binanceService.getOrdersBySymbol(symbol)) {
            is Success -> createSuccessResponse(result.value)
            is Failure -> createErrorResponse(400, result.reason.toString())
        }
    }

    private fun <T> createSuccessResponse(data: T): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent().apply {
            statusCode = 200
            headers = mapOf("Content-Type" to "application/json")
            body = objectMapper.writeValueAsString(ApiResponse(success = true, data = data))
        }
    }

    private fun createErrorResponse(statusCode: Int, message: String): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent().apply {
            this.statusCode = statusCode
            headers = mapOf("Content-Type" to "application/json")
            body = objectMapper.writeValueAsString(ApiResponse<Nothing>(success = false, error = message))
        }
    }
}

// Binance Service
class BinanceService(private val binanceClient: HttpHandler) {

    private val binanceApiSecret = loadProperty(BINANCE_API_SECRET)
    private val binanceApiKey = loadProperty(BINANCE_API_KEY)

    fun createOrder(orderRequest: CreateOrderRequest): Result4k<BinanceOrderResponse, CryptoTrackerError> {
        val method = Method.POST
        val params = mutableMapOf<String, String>()

        // Add required parameters
        params["symbol"] = orderRequest.symbol
        params["side"] = orderRequest.side
        params["type"] = orderRequest.type

        // Add optional parameters
        orderRequest.timeInForce?.let { params["timeInForce"] = it }
        orderRequest.quantity?.let { params["quantity"] = it }
        orderRequest.quoteOrderQty?.let { params["quoteOrderQty"] = it }
        orderRequest.price?.let { params["price"] = it }
        orderRequest.newClientOrderId?.let { params["newClientOrderId"] = it }

        val request = buildRequest(method, BINANCE_ORDER_PATH, params)
        val response = binanceClient(request)

        return when (response.status) {
            Status.OK -> {
                try {
                    val orderResponse = ObjectMapper().registerKotlinModule()
                        .readValue(response.bodyString(), BinanceOrderResponse::class.java)
                    Success(orderResponse)
                } catch (e: Exception) {
                    Failure(CryptoTrackerError.BinanceError("Failed to parse response: ${e.message}"))
                }
            }
            else -> Failure(CryptoTrackerError.BinanceError(response.bodyString()))
        }
    }

    fun getOrdersBySymbol(symbol: String): Result4k<List<BinanceOrderResponse>, CryptoTrackerError> {
        val method = Method.GET
        val params = mutableMapOf<String, String>()
        params["symbol"] = symbol

        val request = buildRequest(method, BINANCE_ORDERS_PATH, params)
        val response = binanceClient(request)

        return when (response.status) {
            Status.OK -> {
                try {
                    val orderResponses = ObjectMapper().registerKotlinModule()
                        .readValue(response.bodyString(), Array<BinanceOrderResponse>::class.java)
                        .toList()
                    Success(orderResponses)
                } catch (e: Exception) {
                    Failure(CryptoTrackerError.BinanceError("Failed to parse response: ${e.message}"))
                }
            }
            else -> Failure(CryptoTrackerError.BinanceError(response.bodyString()))
        }
    }

    private fun buildRequest(
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
}


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
    return System.getenv(propertyName)
        ?: throw RuntimeException("Missing environment variable: $propertyName")
}

// Helper function to convert bytes to hex (replace your Hex.hex() function)
fun bytesToHex(bytes: ByteArray): String {
    return bytes.joinToString("") { "%02x".format(it) }
}