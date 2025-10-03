package com.blackchain.ec2

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
import java.time.Instant

private const val BINANCE_BASE_URL = "https://api.binance.com"


fun main() {
    println("=== Binance DCA Order Execution ===")
    println("Timestamp: ${Instant.now()}")

    val binanceClient = ClientFilters.SetBaseUriFrom(Uri.of(BINANCE_BASE_URL)).then(JavaHttpClient())
    val binanceService = BinanceService(binanceClient)

    val orderRequest = getCreateOrderRequest(binanceService)
    println("Creating DCA order: $orderRequest")

    println("\nCreating order...")

    when (val result = binanceService.createOrder(orderRequest)) {
        is Success -> {
            println("DCA order successful: ${result.value}")
            "SUCCESS: Order ${result.value.orderId} created. Bought ${result.value.executedQty} BTC"
        }
        is Failure -> {
            println("DCA order failed: ${result.reason}")
            "FAILURE: ${result.reason}"
        }
    }
}

fun getCreateOrderRequest(binanceService: BinanceService): CreateOrderRequest {
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