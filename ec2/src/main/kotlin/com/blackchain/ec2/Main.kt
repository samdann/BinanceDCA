package com.blackchain.ec2

import com.blackchain.com.blackchain.core.adapters.BinanceService
import com.blackchain.com.blackchain.core.adapters.domain.CreateOrderRequest
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.client.JavaHttpClient
import org.http4k.client.JavaHttpClient.invoke
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import java.time.Instant

private const val BINANCE_BASE_URL = "https://api.binance.com"


fun main() {
    println("=== Binance DCA Order Execution ===")
    println("Timestamp: ${Instant.now()}")

    val binanceClient = ClientFilters.SetBaseUriFrom(Uri.of(BINANCE_BASE_URL)).then(JavaHttpClient())
    val binanceService = BinanceService(binanceClient)

    // Get configuration from environment
    val symbol = System.getenv("DCA_SYMBOL") ?: "BTCUSDT"
    val amount = System.getenv("DCA_AMOUNT") ?: "10.50"

    println("Symbol: $symbol")
    println("Amount: $amount USDT")

    val orderRequest = CreateOrderRequest(
        symbol = symbol,
        side = "BUY",
        type = "MARKET",
        quoteOrderQty = amount
    )

    println("\nCreating order...")

    when (val result = binanceService.createOrder(orderRequest)) {
        is Success -> {
            println("\n✅ Order created successfully!")
            println("Order ID: ${result.value.orderId}")
            println("Executed Qty: ${result.value.executedQty} $symbol")
            println("Status: ${result.value.status}")
            //println("Transaction Time: ${result.value.transactTime}")
        }
        is Failure -> {
            println("\n❌ Order failed!")
            println("Error: ${result.reason}")
            System.exit(1)
        }
    }
}