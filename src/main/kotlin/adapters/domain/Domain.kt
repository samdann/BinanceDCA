package com.blackchain.adapters.domain

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal

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
    @JsonAlias("origQty") val quantity: BigDecimal,
    @JsonAlias("executedQty") val executedQty: BigDecimal,
    @JsonAlias("cummulativeQuoteQty") val totalValue: BigDecimal,
    val type: String,
    val side: String,
    val status: String,
    @JsonAlias("time") val creationTime: Long,
    @JsonAlias("updateTime") val executionTime: Long,
    val price: BigDecimal
)

data class SpotPrice(
    val symbol: String,
    val price: BigDecimal
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
        //val time = if (input.executionTime != input.executionTime) input.executionTime else input.creationTime
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
            mutableListOf(),
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