package com.blackchain.lambda


import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.blackchain.com.blackchain.core.adapters.BinanceService
import com.blackchain.com.blackchain.core.adapters.domain.CreateOrderRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import kotlin.apply
import kotlin.jvm.java
import kotlin.jvm.javaClass
import kotlin.let
import kotlin.stackTraceToString
import kotlin.to

sealed class CryptoTrackerError {
    data class BinanceError(val message: String) : CryptoTrackerError()
    data class ValidationError(val message: String) : CryptoTrackerError()
}

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

private const val BINANCE_BASE_URL = "https://api.binance.com"

// Main Lambda Handler
class BinanceOrderHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val binanceClient = ClientFilters.SetBaseUriFrom(Uri.of(BINANCE_BASE_URL)).then(JavaHttpClient())
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
                "POST" -> handleCreateDCAOrder(input)
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

    //private fun handleCreateDAC

    private fun handleCreateDCAOrder(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
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

        val result = binanceService.getOrders(symbol)
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