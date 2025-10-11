package com.blackchain.com.blackchain.core.application

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatNumber(number: BigDecimal, precision: Int): Double {
    return number.setScale(precision, RoundingMode.HALF_UP).toDouble()
}

fun convertToReadableDate(timestamp: Long): String {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") // or any desired format
    return dateTime.format(formatter)
}