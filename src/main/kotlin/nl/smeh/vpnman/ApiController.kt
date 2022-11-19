package nl.smeh.vpnman

import kotlinx.coroutines.flow.flow
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.web.reactive.function.server.*

private const val routingTableName = "to-vpn"
private const val vpnGatewayAddress = "10.5.0.1"
private const val vpnLocalAddress = "10.5.0.2/29"
private const val country = "NL"
private const val technologyName = "Wireguard"
private const val vpnManComment = "Managed by VPNMAN"
private const val nordLynxInterface = "nordlynx1"
private const val nordLynxPort = 51820

enum class ConnectionStatus(val value: String) {
    UNKNOWN("unknown"),
    DISCONNECTED("disconnected"),
    CONNECTED("connected"),
    CONNECTING("connecting"),
    DISCONNECTING("disconnecting"),
}

data class ResponseMessage(val status: Status, val message: String)

data class Status(val connection: ConnectionStatus, val server: String?, val hosts: List<String>)

class ApiController(private val nordVpnServers: NordVpnServers, private val routerOsClient: RouterOsClient) {

    private var connectionStatus = ConnectionStatus.UNKNOWN
    private var hosts = listOf<String>()
    private var server: String? = null

    private fun message(message: String) = ResponseMessage(Status(connectionStatus, server, hosts), message)

    suspend fun getState(req: ServerRequest): ServerResponse {
        server = getWireguardPeerName()
        hosts = getRoutedHosts()
        connectionStatus =
            if (server != null && !hosts.isEmpty()) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValueAndAwait(
            message(if (connectionStatus == ConnectionStatus.CONNECTED) "Connected to $server" else "Disconnected")
        )
    }

    suspend fun connect(req: ServerRequest) =
        ServerResponse.ok().contentType(MediaType.APPLICATION_NDJSON).bodyAndAwait(flow {
            connectionStatus = ConnectionStatus.CONNECTING

            val data = req.awaitMultipartData()
            hosts = data.getOrDefault("host", listOf()).filterIsInstance<FormFieldPart>().map { it.value() }
            if (hosts.isEmpty()) {
                connectionStatus = ConnectionStatus.DISCONNECTED
                emit(message("Error! No hosts selected for VPN"))
                return@flow
            }

            emit(message("Checking optimal server for country $country"))
            val servers = nordVpnServers.getServers(
                Filter(
                    country_id = nordVpnServers.countriesByCode[country]?.id,
                    servers_technologies = listOf(nordVpnServers.technologiesByName[technologyName]!!.id)
                )
            )

            if (servers.isEmpty()) {
                connectionStatus = ConnectionStatus.DISCONNECTED
                emit(message("No VPN servers found"))
                return@flow
            }

            val newServer = servers.first()
            server = newServer.hostname

            emit(message("Setting up VPN client interface"))
            ensureWireguardInterface()

            emit(message("Configuring IP address for VPN interface"))
            ensureWireguardIpAddress()

            emit(message("Configuring NAT for VPN interface"))
            ensureFirewallNat()

            emit(message("Setting up VPN connection"))
            ensureWireguardPeer(newServer)

            emit(message("Setting up routing table for VPN"))
            ensureRoutingTable()

            emit(message("Setting up route to VPN"))
            ensureRouteToVpn()

            emit(message("Setting up routing rules"))
            clearRoutingRules()
            hosts.forEach { ensureRoutingRule(it) }

            connectionStatus = ConnectionStatus.CONNECTED
            emit(message("Connected to $server!"))
        })

    suspend fun disconnect(@Suppress("UNUSED_PARAMETER") req: ServerRequest) =
        ServerResponse.ok().contentType(MediaType.APPLICATION_NDJSON).bodyAndAwait(flow {
            connectionStatus = ConnectionStatus.DISCONNECTING

            emit(message("Closing VPN connection"))
            clearWireguardPeer()
            server = null

            emit(message("Clearing routing rule"))
            clearRoutingRules()
            hosts = listOf()

            connectionStatus = ConnectionStatus.DISCONNECTED
            emit(message("Disconnected!"))
        })


    // /routing/table/add fib name=to-vpn
    private fun ensureRoutingTable() {
        val routingTables = routerOsClient.list(RoutingTable(name = routingTableName))
        if (routingTables.isEmpty()) {
            routerOsClient.add(RoutingTable(name = routingTableName, fib = true, comment = vpnManComment))
        }
    }

    // /ip/route/add dst-address=0.0.0.0/0 gateway=10.5.0.1 routing-table=to-vpn
    private fun ensureRouteToVpn() {
        val routes = routerOsClient.list<IpRoute>()
        if (routes.none { it.routingTable == routingTableName && it.gateway == vpnGatewayAddress }) {
            routerOsClient.add(
                IpRoute(
                    dstAddress = "0.0.0.0/0",
                    gateway = vpnGatewayAddress,
                    routingTable = routingTableName,
                    comment = vpnManComment
                )
            )
        }
    }

    // /interface/wireguard/add listen-port=51820 name=nordlynx
    private fun ensureWireguardInterface() {
        val wireguardInterfaces = routerOsClient.list(InterfaceWireguard(name = nordLynxInterface, listenPort = 51820))
        if (wireguardInterfaces.isEmpty()) {
            routerOsClient.add(
                InterfaceWireguard(
                    name = nordLynxInterface,
                    listenPort = nordLynxPort,
                    privateKey = "TODO PRIVATE KEY",
                    comment = vpnManComment
                )
            )
        }
    }

    private fun ensureWireguardIpAddress() {
        val addresses = routerOsClient.list(IpAddress(iface = nordLynxInterface))
        if (addresses.isEmpty()) {
            routerOsClient.add(IpAddress(iface = nordLynxInterface, address = vpnLocalAddress, comment = vpnManComment))
        }
    }

    // /interface/wireguard/peers/add allowed-address=0.0.0.0/0 endpoint-address=XXX endpoint-port=51820 interface=nordlynx public-key=xxx
    private fun ensureWireguardPeer(server: Server) {
        val address = server.station
        val key = server.technologies.first { it.name == "Wireguard" }.metadata!!["public_key"]
        val comment = "${server.hostname} - $vpnManComment"

        val peers = routerOsClient.list(InterfaceWireguardPeers(iface = nordLynxInterface))
        if (peers.isEmpty()) {
            routerOsClient.add(
                InterfaceWireguardPeers(
                    allowedAddress = "0.0.0.0/0", iface = nordLynxInterface,
                    endpointAddress = address, endpointPort = nordLynxPort, publicKey = key, comment = comment
                )
            )
        } else if (peers.first().endpointAddress != address || peers.first().publicKey != key) {
            routerOsClient.set(
                peers.first().id!!, InterfaceWireguardPeers(
                    allowedAddress = "0.0.0.0/0", iface = nordLynxInterface,
                    endpointAddress = address, endpointPort = nordLynxPort, publicKey = key, comment = comment
                )
            )
        }
    }

    private fun ensureFirewallNat() {
        val rules = routerOsClient.list(
            IpFirewallNat(
                chain = "srcnat",
                action = IpFirewallNat.Action.MASQUERADE,
                outInterface = nordLynxInterface
            )
        )
        if (rules.isEmpty()) {
            routerOsClient.add(
                IpFirewallNat(
                    chain = "srcnat",
                    action = IpFirewallNat.Action.MASQUERADE,
                    outInterface = nordLynxInterface,
                    comment = vpnManComment
                )
            )
        }
    }

    private fun clearWireguardPeer() {
        val peers = routerOsClient.list(InterfaceWireguardPeers(iface = nordLynxInterface))
        peers.filter { it.comment?.endsWith(vpnManComment) ?: false }
            .forEach { routerOsClient.remove<InterfaceWireguardPeers>(it.id!!) }
    }

    private fun getWireguardPeerName(): String? {
        val peers = routerOsClient.list(InterfaceWireguardPeers(iface = nordLynxInterface))
        return peers.filter { it.comment?.endsWith(vpnManComment) ?: false }
            .map { it.comment!!.split(" - ").first() }
            .firstOrNull()
    }

    // /routing/rule/add action=lookup routing-mark=to-vpn table=to-vpn
    private fun ensureRoutingRule(host: String) {
        routerOsClient.add(
            RoutingRule(
                srcAddress = host,
                action = "lookup",
                table = routingTableName,
                comment = vpnManComment
            )
        )
    }

    private fun clearRoutingRules() {
        val rules = routerOsClient.list<RoutingRule>()
        rules.filter { it.comment == vpnManComment }.forEach { routerOsClient.remove<RoutingRule>(it.id!!) }
    }

    private fun getRoutedHosts(): List<String> {
        return routerOsClient.list(RoutingRule(table = routingTableName)).map { it.srcAddress!! }
    }
}
