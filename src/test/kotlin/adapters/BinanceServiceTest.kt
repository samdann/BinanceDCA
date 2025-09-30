package adapters

import com.blackchain.adapters.buildPublicRequest
import com.blackchain.adapters.buildRequest
import org.http4k.client.JavaHttpClient
import org.http4k.client.JavaHttpClient.invoke
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.junit.jupiter.api.Test

private const val BINANCE_BASE_URL = "https://testnet.binance.vision"
private const val BINANCE_ORDER_PATH = "/api/v3/order"


class BinanceServiceTest {

    private val binance = ClientFilters.SetBaseUriFrom(Uri.of(BINANCE_BASE_URL)).then(JavaHttpClient())

    @Test
    fun `can create a test order a`() {

        val method = Method.POST
        val params = mutableMapOf<String, String>()

        // Add required parameters
        params["symbol"] = "BTCEUR"
        params["side"] = "BUY"
        params["type"] = "MARKET"
        params["quoteOrderQty"] = "10"

        println("### PRICE ###")
        getPrice()

        val request = buildRequest(method, BINANCE_ORDER_PATH, params)
        println(request)

        val response = binance(request)
        println(response)
    }

    private fun getPrice() {
        val method = Method.GET
        val params = mutableMapOf<String, String>()
        params["symbol"] = "BTCEUR"
        val request = buildPublicRequest(method, "/api/v3/ticker/price", params)
        println(request)
        val response = binance(request)
        println("PRICE is #: $response")
    }

}