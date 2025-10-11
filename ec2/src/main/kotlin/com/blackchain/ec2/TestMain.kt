package com.blackchain.ec2

import com.blackchain.com.blackchain.core.adapters.BinanceService
import org.http4k.client.JavaHttpClient
import org.http4k.client.JavaHttpClient.invoke
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters

private const val BINANCE_BASE_URL = "https://api.binance.com"

fun main() {
    val binanceClient = ClientFilters.SetBaseUriFrom(Uri.of(BINANCE_BASE_URL)).then(JavaHttpClient())
    val binanceService = BinanceService(binanceClient)

    val pair = "BTCEUR"
    println(binanceService.getOrdersSummary(pair))

}