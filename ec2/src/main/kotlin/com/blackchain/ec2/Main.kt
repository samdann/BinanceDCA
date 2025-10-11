package com.blackchain.ec2

import com.blackchain.com.blackchain.core.adapters.BinanceService
import com.blackchain.com.blackchain.core.adapters.domain.CreateOrderRequest
import com.blackchain.com.blackchain.core.adapters.domain.CreateOrderResponse
import com.blackchain.com.blackchain.core.application.convertToReadableDate
import com.blackchain.com.blackchain.core.application.formatNumber
import com.blackchain.com.blackchain.core.application.mail.sendEmail
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


suspend fun main() {
    println("=== Binance DCA Order Execution ===")
    println("Timestamp: ${Instant.now()}")

    val binanceClient = ClientFilters.SetBaseUriFrom(Uri.of(BINANCE_BASE_URL)).then(JavaHttpClient())
    val binanceService = BinanceService(binanceClient)

    val pair = "BTCEUR"

    val orderRequest = getCreateOrderRequest(binanceService)
    println("Creating DCA order: $orderRequest")

    println("\nCreating order...")

    when (val result = binanceService.createOrder(orderRequest)) {
        is Success -> {
            println("DCA order successful: ${result.value}")
            "SUCCESS: Order ${result.value.orderId} created. Bought ${result.value.executedQty} BTC"
            var executionReport =
                generateEmailBody(result)
            var holdingsReport = binanceService.getOrdersSummary(pair)
            sendEmail(executionReport + holdingsReport)
        }
        is Failure -> {
            println("DCA order failed: ${result.reason}")
            "FAILURE: ${result.reason}"
        }
    }
}

private fun generateEmailBody(result: Success<CreateOrderResponse>): String {

    return "Your daily DCA order of Bitcoin has been successfully executed on ${convertToReadableDate(result.value.workingTime)}: \n ${
        formatNumber(result.value.executedQty.toBigDecimal(), 4)
    }BTC bought for ${result.value.cumulativeQuoteQty}€ at a price of ${result.value.price}€ per coin."
}


fun getCreateOrderRequest(binanceService: BinanceService): CreateOrderRequest {
    val pair = "BTCEUR"
    val amount = 20L
    val price = binanceService.getSpotPrice(pair).valueOrNull()

    val dcaAmount = BigDecimal.valueOf(amount)
    val quantity = dcaAmount.divide(price, 8, RoundingMode.HALF_DOWN)
    val minSize = BigDecimal.valueOf(0.0001)
    val stepSize = BigDecimal.valueOf(0.00001)

    val n = (quantity - minSize).divide(stepSize, 8, RoundingMode.DOWN).toInt()
    val adjustedQty = minSize + (stepSize.multiply(n.toBigDecimal()))

    return CreateOrderRequest(pair, "BUY", "LIMIT", "GTC", adjustedQty.toString(), null, price.toString(), null)

}
