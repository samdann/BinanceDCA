package adapters

import com.blackchain.adapters.BinanceService
import com.blackchain.adapters.buildRequest
import com.blackchain.adapters.domain.CreateOrderRequest
import dev.forkhandles.result4k.valueOrNull
import org.http4k.client.JavaHttpClient
import org.http4k.client.JavaHttpClient.invoke
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.math.BigDecimal
import java.math.RoundingMode

private const val BINANCE_BASE_URL = "https://testnet.binance.vision"

class BinanceServiceTest {

    private val binance = ClientFilters.SetBaseUriFrom(Uri.of(BINANCE_BASE_URL)).then(JavaHttpClient())
    private val binanceService = BinanceService(binance)

    @Test
    fun `can create a test order on the testnet`() {

        println("### PRICE ###")

        val price = binanceService.getSpotPrice("BTCEUR").valueOrNull()
        println("Price is: $price")

        val dcaAmount = BigDecimal.valueOf(10)
        val quantity = dcaAmount.divide(price, 8, RoundingMode.HALF_DOWN)
        val minSize = BigDecimal.valueOf(0.0001)
        val stepSize = BigDecimal.valueOf(0.00001)

        val n = (quantity - minSize).divide(stepSize, 8, RoundingMode.DOWN).toInt()
        val adjustedQty = minSize + (stepSize.multiply(n.toBigDecimal()))

        val orderRequest =
            CreateOrderRequest("BTCEUR", "BUY", "LIMIT", "GTC", adjustedQty.toString(), null, price.toString(), null)
        val response = binanceService.createOrder(orderRequest)
        expectThat(response.valueOrNull()!!.status).isEqualTo("FILLED")

    }

    @Test
    fun `can read exchangeInfo for BTCEUR pair`() {
        val method = Method.GET
        val url = "/api/v3/exchangeInfo?symbol=BTCEUR"

        val request = buildRequest(method, url, mutableMapOf(), false)
        val response = binance(request)
        println(response)
    }

}