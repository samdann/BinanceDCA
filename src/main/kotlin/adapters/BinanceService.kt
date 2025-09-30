package com.blackchain.adapters

import com.blackchain.BinanceOrderResponse
import com.blackchain.CreateOrderRequest
import com.blackchain.CryptoTrackerError
import com.blackchain.adapters.domain.BinanceOrder
import com.blackchain.adapters.domain.Order
import com.blackchain.adapters.domain.toOrders
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.format.Jackson.auto

// Constants
private const val BINANCE_ORDER_PATH = "/api/v3/order"
private const val BINANCE_ORDERS_PATH = "/api/v3/allOrders"

//Lenses
val binanceOrdersLens = Body.auto<List<BinanceOrder>>().toLens()

// Binance Service
class BinanceService(private val binanceClient: HttpHandler) {

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

    fun getOrdersBySymbol(symbol: String): Result4k<List<Order>, CryptoTrackerError> {
        val method = Method.GET
        val params = mutableMapOf<String, String>()
        params["symbol"] = symbol

        val request = buildRequest(method, BINANCE_ORDERS_PATH, params)
        val response = binanceClient(request)

        return when (response.status) {
            Status.OK -> {
                try {
                    Success(toOrders(binanceOrdersLens(response)))
                } catch (e: Exception) {
                    Failure(CryptoTrackerError.BinanceError("Failed to parse response: ${e.message}"))
                }
            }
            else -> Failure(CryptoTrackerError.BinanceError(response.bodyString()))
        }
    }

}