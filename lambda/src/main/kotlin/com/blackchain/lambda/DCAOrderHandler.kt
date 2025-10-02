package com.blackchain.lambda


import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.blackchain.com.blackchain.core.adapters.BinanceService
import com.blackchain.com.blackchain.core.adapters.domain.CreateOrderRequest
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import org.http4k.client.JavaHttpClient
import org.http4k.client.JavaHttpClient.invoke
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.minus
import kotlin.plus
import kotlin.stackTraceToString
import kotlin.toBigDecimal
import kotlin.toString

private const val BINANCE_BASE_URL = "https://api.binance.com"

class DCAOrderHandler : RequestHandler<ScheduledEvent, String> {

    private val binanceClient = ClientFilters.SetBaseUriFrom(Uri.of(BINANCE_BASE_URL)).then(JavaHttpClient())
    private val binanceService = BinanceService(binanceClient)

    override fun handleRequest(input: ScheduledEvent, context: Context): String {
        context.logger.log("DCA Order triggered at ${input.time}")

        return try {
            // Configure your DCA order here
            val orderRequest = getCreateOrderRequest()
            context.logger.log("Creating DCA order: $orderRequest")

            when (val result = binanceService.createOrder(orderRequest)) {
                is Success -> {
                    context.logger.log("DCA order successful: ${result.value}")
                    "SUCCESS: Order ${result.value.orderId} created. Bought ${result.value.executedQty} BTC"
                }
                is Failure -> {
                    context.logger.log("DCA order failed: ${result.reason}")
                    "FAILURE: ${result.reason}"
                }
            }
        } catch (e: Exception) {
            context.logger.log("ERROR in DCA order: ${e.message}")
            context.logger.log("Stack trace: ${e.stackTraceToString()}")
            "ERROR: ${e.message}"
        }
    }

    private fun getCreateOrderRequest(): CreateOrderRequest {
        val pair = "BTCEUR"
        val price = binanceService.getSpotPrice(pair).valueOrNull()

        val dcaAmount = BigDecimal.valueOf(10)
        val quantity = dcaAmount.divide(price, 8, RoundingMode.HALF_DOWN)
        val minSize = BigDecimal.valueOf(0.0001)
        val stepSize = BigDecimal.valueOf(0.00001)

        val n = (quantity - minSize).divide(stepSize, 8, RoundingMode.DOWN).toInt()
        val adjustedQty = minSize + (stepSize.multiply(n.toBigDecimal()))

        return CreateOrderRequest(pair, "BUY", "LIMIT", "GTC", adjustedQty.toString(), null, price.toString(), null)

    }
}