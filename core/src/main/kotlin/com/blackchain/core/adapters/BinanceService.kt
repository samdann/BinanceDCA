package com.blackchain.com.blackchain.core.adapters

import com.blackchain.com.blackchain.core.adapters.domain.BinanceOrder
import com.blackchain.com.blackchain.core.adapters.domain.CreateOrderRequest
import com.blackchain.com.blackchain.core.adapters.domain.CreateOrderResponse
import com.blackchain.com.blackchain.core.adapters.domain.CryptoTrackerError
import com.blackchain.com.blackchain.core.adapters.domain.Order
import com.blackchain.com.blackchain.core.adapters.domain.SpotPrice
import com.blackchain.com.blackchain.core.adapters.domain.toOrders
import com.blackchain.com.blackchain.core.port.Binance
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.format.Jackson.auto
import java.math.BigDecimal


// Constants
private const val BINANCE_ORDER_PATH = "/api/v3/order"
private const val BINANCE_ORDERS_PATH = "/api/v3/allOrders"
private const val BINANCE_SPOT_PRICE = "/api/v3/ticker/price"

//Lenses
val binanceOrdersLens = Body.auto<List<BinanceOrder>>().toLens()
val binanceSpotPriceLens = Body.auto<SpotPrice>().toLens()
val binanceCreateOrderLens = Body.auto<CreateOrderResponse>().toLens()

// Binance Service
class BinanceService(private val binanceClient: HttpHandler) : Binance {

    override fun createOrder(orderRequest: CreateOrderRequest): Result4k<CreateOrderResponse, CryptoTrackerError> {
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

        val request = buildRequest(method, BINANCE_ORDER_PATH, params, true)
        println(request)
        val response = binanceClient(request)
        //println(response)

        return when (response.status) {
            Status.OK -> {
                try {
                    Success(binanceCreateOrderLens(response))
                } catch (e: Exception) {
                    Failure(CryptoTrackerError.BinanceError("Failed to parse response: ${e.message}"))
                }
            }
            else -> Failure(CryptoTrackerError.BinanceError(response.bodyString()))
        }
    }

    override fun getOrders(symbol: String): Result4k<List<Order>, CryptoTrackerError> {
        val method = Method.GET
        val params = mutableMapOf<String, String>()
        params["symbol"] = symbol

        val request = buildRequest(method, BINANCE_ORDERS_PATH, params, true)
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

    override fun getSpotPrice(ticker: String): Result4k<BigDecimal, CryptoTrackerError> {
        val method = Method.GET
        val params = mutableMapOf<String, String>()
        params["symbol"] = ticker
        val request = buildRequest(method, BINANCE_SPOT_PRICE, params, false)
        val response = binanceClient(request)
        return when (response.status) {
            Status.OK -> try {
                Success(binanceSpotPriceLens(response).price)
            } catch (e: Exception) {
                Failure(CryptoTrackerError.BinanceError("Failed to parse response: ${e.message}"))
            }
            else -> Failure(CryptoTrackerError.BinanceError(response.bodyString()))
        }
    }
}