package com.blackchain.port

import com.blackchain.CreateOrderRequest
import com.blackchain.CryptoTrackerError
import com.blackchain.adapters.domain.CreateOrderResponse
import com.blackchain.adapters.domain.Order
import dev.forkhandles.result4k.Result4k
import java.math.BigDecimal

interface Binance {

    fun createOrder(orderRequest: CreateOrderRequest): Result4k<CreateOrderResponse, CryptoTrackerError>
    fun getOrders(symbol: String): Result4k<List<Order>, CryptoTrackerError>
    fun getSpotPrice(ticker: String): Result4k<BigDecimal, CryptoTrackerError>
}