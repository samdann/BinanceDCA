package com.blackchain.com.blackchain.core.port

import com.blackchain.com.blackchain.core.adapters.domain.CreateOrderRequest
import com.blackchain.com.blackchain.core.adapters.domain.CreateOrderResponse
import com.blackchain.com.blackchain.core.adapters.domain.CryptoTrackerError
import com.blackchain.com.blackchain.core.adapters.domain.Order
import dev.forkhandles.result4k.Result4k
import java.math.BigDecimal

interface Binance {

    fun createOrder(orderRequest: CreateOrderRequest): Result4k<CreateOrderResponse, CryptoTrackerError>
    fun getOrders(pair: String): Result4k<List<Order>, CryptoTrackerError>
    fun getSpotPrice(pair: String): Result4k<BigDecimal, CryptoTrackerError>
    fun getOrdersSummary(pair: String): String
}