package com.blackchain

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.blackchain.adapters.BinanceService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.forkhandles.result4k.*
import org.http4k.client.OkHttp


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
    private lateinit var context: Context

    override fun handleRequest(
        input: APIGatewayProxyRequestEvent,
        context: Context
    ): APIGatewayProxyResponseEvent {

        this.context = context

        return try {
            context.logger.log("Received ${input.httpMethod} request to ${input.path}")

            when (input.httpMethod) {
                "POST" -> handleCreateOrder(input)
                "GET" -> handleGetOrders(input)
                else -> createErrorResponse(405, "Method not allowed")
            }
        } catch (e: Exception) {
            // Log full stack trace for debugging
            context.logger.log("ERROR: ${e.javaClass.name}: ${e.message}")
            context.logger.log("Stack trace: ${e.stackTraceToString()}")

            // Log the cause if it exists
            e.cause?.let { cause ->
                context.logger.log("Caused by: ${cause.javaClass.name}: ${cause.message}")
                context.logger.log("Cause stack trace: ${cause.stackTraceToString()}")
            }

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

        val result = binanceService.getOrdersBySymbol(symbol)
        println(result.valueOrNull())
        return when (result) {
            is Success -> createSuccessResponse(result.valueOrNull())
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


