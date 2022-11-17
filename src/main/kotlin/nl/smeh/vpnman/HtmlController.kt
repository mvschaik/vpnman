package nl.smeh.vpnman

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.renderAndAwait

class HtmlController(private val vpnServers: NordVpnServers, private val routerOsClient: RouterOsClient) {
    suspend fun blog(req: ServerRequest): ServerResponse {
//        val countryId = vpnServers.countriesByCode["NL"]?.id ?: throw java.lang.RuntimeException("Country not found")
//        val wireguardId = vpnServers.technologiesByName["Wireguard"]?.id ?: throw java.lang.RuntimeException("Wireguard technology not found")
//        val bestServer =
//            vpnServers.getServers(Filter(country_id = countryId, servers_technologies = listOf(wireguardId)))
//                .flatMap { server -> server.technologies.filter { it.id == wireguardId }.map { Pair(server, it) } }
//                .first()
//        println(bestServer.first.station + " " + bestServer.second.metadata)
//        model["title"] = "Blog"
//        return "blog"

        val clientAddr = req.remoteAddress().get().address.hostAddress

        val leases =
            routerOsClient.list<IpDhcpServerLease>().map {
                mapOf(
                    "name" to it.hostName,
                    "address" to it.activeAddress,
                    "checked" to (clientAddr == it.activeAddress)
                )
            }
        return ServerResponse.ok().renderAndAwait("main", mapOf("hosts" to leases))
    }
}
