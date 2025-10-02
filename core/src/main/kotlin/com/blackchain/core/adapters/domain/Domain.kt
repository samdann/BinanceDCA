package com.blackchain.com.blackchain.core.adapters.domain

import java.math.BigDecimal
import kotlin.collections.map
import kotlin.text.lowercase

data class Order(
    val orderId: String,
    val creationTime: Long,
    val pair: String,
    val type: String,
    val side: String,
    var quantity: BigDecimal,
    var price: BigDecimal,
    val totalValue: BigDecimal,
    var fee: BigDecimal,
    var feeAsset: String,
    var status: OrderStatus,
    val priceFromTrades: Boolean,
    val tradeIds: List<String>?,
    val manualImport: Boolean = false,
)

data class BinanceOrder(
    val symbol: String,
    val orderId: String,
    @com.fasterxml.jackson.annotation.JsonAlias("origQty") val quantity: BigDecimal,
    @com.fasterxml.jackson.annotation.JsonAlias("executedQty") val executedQty: BigDecimal,
    @com.fasterxml.jackson.annotation.JsonAlias("cummulativeQuoteQty") val totalValue: BigDecimal,
    val type: String,
    val side: String,
    val status: String,
    @com.fasterxml.jackson.annotation.JsonAlias("time") val creationTime: Long,
    @com.fasterxml.jackson.annotation.JsonAlias("updateTime") val executionTime: Long,
    val price: BigDecimal
)

data class CreateOrderRequest(
    val symbol: String,
    val side: String, // BUY or SELL
    val type: String, // MARKET, LIMIT, etc.
    val timeInForce: String? = null, // GTC, IOC, FOK
    val quantity: String? = null,
    val quoteOrderQty: String? = null,
    val price: String? = null,
    val newClientOrderId: String? = null
)


data class SpotPrice(
    val symbol: String,
    val price: BigDecimal
)

data class CreateOrderResponse(
    val symbol: String,
    val orderId: Long,
    val price: String,
    val origQty: String,
    val executedQty: String,
    val origQuoteOrderQty: String,
    @com.fasterxml.jackson.annotation.JsonAlias("cummulativeQuoteQty") val cumulativeQuoteQty: String,
    val status: String,
    val timeInForce: String,
    val type: String,
    val side: String,
    val workingTime: Long,
    val fills: List<Fill>
)

data class Fill(
    val price: String,
    val qty: String,
    val commission: String,
    val commissionAsset: String,
    val tradeId: Long
)

enum class OrderStatus {
    FILLED,
    CANCELED,
    PARTIALLY_FILLED,
    PARTIALLY_CANCELED,
    NEW
}

fun toOrders(binanceOrders: List<BinanceOrder>): List<Order> {
    return binanceOrders.map { input ->
        val order = Order(
            input.orderId,
            input.executionTime,
            input.symbol,
            input.type,
            input.side,
            input.quantity,
            input.price,
            input.totalValue,
            BigDecimal.valueOf(0.0),
            feeAsset = "",
            getStatus(input.status.lowercase()),
            priceFromTrades = false,
            kotlin.collections.mutableListOf(),
            manualImport = false
        )
        if (input.status == "PARTIALLY_CANCELED") {
            order.quantity = input.executedQty
        }
        order
    }
}

private fun getStatus(input: String): OrderStatus {
    return when (input) {
        "filled" -> OrderStatus.FILLED
        "new" -> OrderStatus.NEW
        "partially_canceled" -> OrderStatus.PARTIALLY_CANCELED
        else -> OrderStatus.CANCELED
    }
}

sealed class CryptoTrackerError {
    data class BinanceError(val message: String) : CryptoTrackerError()
    data class ValidationError(val message: String) : CryptoTrackerError()
}