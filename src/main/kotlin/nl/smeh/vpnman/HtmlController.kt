package nl.smeh.vpnman

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.renderAndAwait

class HtmlController(
    private val vpnServers: NordVpnServers,
    private val routerOsClient: RouterOsClient,
    private val preferredCountryCodes: List<String>
) {

    suspend fun home(req: ServerRequest): ServerResponse {
        val clientAddr = req.remoteAddress().get().address.hostAddress

        val leases =
            routerOsClient.list<IpDhcpServerLease>().map {
                mapOf(
                    "name" to "${it.hostName}${if (clientAddr == it.activeAddress) " (you!)" else ""}",
                    "address" to it.activeAddress,
                    "checked" to (clientAddr == it.activeAddress)
                )
            }

        var countries = vpnServers.countries.map {
            mapOf(
                "name" to "${countryFlag(it.code)} ${it.name}",
                "code" to it.code,
            )
        }

        preferredCountryCodes.reversed().forEach {
            val len = countries.size
            countries = promoteCountry(countries, it)
            if (countries.size != len) throw RuntimeException("Ughh")
        }

        return ServerResponse.ok().renderAndAwait("main", mapOf("hosts" to leases, "countries" to countries))
    }

    private fun promoteCountry(list: List<Map<String, String>>, countryCode: String): List<Map<String, String>> {
        println("Promoting $countryCode")
        val promotedCountryIndex = list.indexOfFirst { it["code"]?.uppercase() == countryCode.uppercase() }
        if (promotedCountryIndex < 0) return list
        return listOf(list[promotedCountryIndex]) +
                list.subList(0, promotedCountryIndex) +
                list.drop(promotedCountryIndex + 1)
    }

    private fun countryFlag(code: String) = code
        .uppercase()
        .split("")
        .filter { it.isNotBlank() }
        .map { it.codePointAt(0) + 0x1F1A5 }
        .joinToString("") { String(Character.toChars(it)) }
}
