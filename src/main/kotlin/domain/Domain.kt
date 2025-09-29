package com.blackchain.domain

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

enum class OrderStatus {
    FILLED,
    CANCELED,
    PARTIALLY_FILLED,
    PARTIALLY_CANCELED,
    NEW
}