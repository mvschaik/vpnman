package nl.smeh.vpnman

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.renderAndAwait

class HtmlController(private val vpnServers: NordVpnServers, private val routerOsClient: RouterOsClient) {
    suspend fun blog(req: ServerRequest): ServerResponse {
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
