package nl.smeh.vpnman

import kotlinx.coroutines.flow.flow
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait

private const val routingTableName = "to-vpn"
private const val addressListName = "vpn-users"
private const val commentMarker = "vpnman"
private const val vpnGatewayAddress = "10.5.0.1"
private const val vpnLocalAddress = "10.5.0.2/29"
private const val routingMarker = "to-vpn"
private const val country = "NL"
private const val technologyName = "Wireguard"
private const val vpnManComment = "Managed by VPNMAN"
private const val nordLynxInterface = "nordlynx1"
private const val nordLynxPort = 51820

enum class Status(val value: String) {
    UNKNOWN("unknown"),
    DISCONNECTED("disconnected"),
    CONNECTED("connected"),
    CONNECTING("connecting"),
    DISCONNECTING("disconnecting"),
}

data class ResponseMessage(val status: Status, val message: String)

class ApiController(private val nordVpnServers: NordVpnServers, private val routerOsClient: RouterOsClient) {

    private var status = Status.UNKNOWN

    private fun message(message: String) = ResponseMessage(status, message)

    suspend fun connect(req: ServerRequest) =
        ServerResponse.ok().contentType(MediaType.APPLICATION_NDJSON).bodyAndAwait(flow {
            status = Status.CONNECTING
            emit(message("Checking optimal server for country $country"))

            val servers = nordVpnServers.getServers(
                Filter(
                    country_id = nordVpnServers.countriesByCode[country]?.id,
                    servers_technologies = listOf(nordVpnServers.technologiesByName[technologyName]!!.id)
                )
            )

            if (servers.isEmpty()) {
                status = Status.DISCONNECTED
                emit(message("No VPN servers found"))
                return@flow
            }

            val server = servers.first()
            emit(message("Connecting to server ${server.hostname}"))

            emit(message("Setting up routing table for VPN"))
            ensureRoutingTable()

            emit(message("Setting up route to VPN"))
            ensureRouteToVpn()

            emit(message("Setting up VPN client interface"))
            ensureWireguardInterface()

            emit(message("Setting up VPN connection"))
            ensureWireguardPeer(server)

            emit(message("Setting up routing rule"))
            ensureRoutingRule()

            emit(message("Setting up firewall rules"))
            ensureFirewallRule()

            // TODO: NAT

            status = Status.CONNECTED
            emit(message("Connected!"))
        })


        //* /routing/table/add fib name=to-vpn
        // /ip/route/add dst-address=0.0.0.0/0 gateway=10.5.0.1 routing-table=to-vpn
        ///interface/wireguard/add listen-port=51820 name=nordlynx
        ///interface/wireguard/peers/add allowed-address=0.0.0.0/0 endpoint-address=XXX endpoint-port=51820 interface=nordlynx public-key=xxx
        ///routing/rule/add action=lookup routing-mark=to-vpn table=to-vpn
        ///ip/firewall/mangle/add action=mark-routing chain=prerouting dst-address-list=!lan-addr new-routing-mark=to-vpn passthrough=no src-address-list=vpn-users

    suspend fun disconnect(req: ServerRequest) =
        ServerResponse.ok().contentType(MediaType.APPLICATION_NDJSON).bodyAndAwait(flow {
            status = Status.DISCONNECTING

            emit(message("Closing VPN connection"))
            clearWireguardPeer()

            emit(message("Clearing routing rule"))
            clearRoutingRule()

            emit(message("Clearing firewall rules"))
            clearFirewallRule()

            status = Status.DISCONNECTED
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

    // /interface/wireguard/peers/add allowed-address=0.0.0.0/0 endpoint-address=XXX endpoint-port=51820 interface=nordlynx public-key=xxx
    private fun ensureWireguardPeer(server: Server) {
        val address = server.station
        val key = server.technologies.first { it.name == "Wireguard" }.metadata!!["public_key"]

        val peers = routerOsClient.list(InterfaceWireguardPeers(iface = nordLynxInterface))
        if (peers.isEmpty()) {
            routerOsClient.add(
                InterfaceWireguardPeers(
                    allowedAddress = "0.0.0.0/0", iface = nordLynxInterface,
                    endpointAddress = address, endpointPort = nordLynxPort, publicKey = key, comment = vpnManComment
                )
            )
        } else if (peers.first().endpointAddress != address || peers.first().publicKey != key) {
            routerOsClient.set(
                peers.first().id!!, InterfaceWireguardPeers(
                    allowedAddress = "0.0.0.0/0", iface = nordLynxInterface,
                    endpointAddress = address, endpointPort = nordLynxPort, publicKey = key, comment = vpnManComment
                )
            )
        }
    }

    private fun clearWireguardPeer() {
        val peers = routerOsClient.list(InterfaceWireguardPeers(iface = nordLynxInterface))
        peers.filter { it.comment == vpnManComment }.forEach { routerOsClient.remove<InterfaceWireguardPeers>(it.id!!) }
    }

    // /routing/rule/add action=lookup routing-mark=to-vpn table=to-vpn
    private fun ensureRoutingRule() {
        val rules =
            routerOsClient.list(RoutingRule(action = "lookup", routingMark = routingMarker, table = routingTableName))
        if (rules.isEmpty()) {
            routerOsClient.add(
                RoutingRule(
                    action = "lookup",
                    routingMark = routingMarker,
                    table = routingTableName,
                    comment = vpnManComment
                )
            )
        }
    }

    private fun clearRoutingRule() {
        val rules = routerOsClient.list<RoutingRule>()
        rules.filter { it.comment == vpnManComment }.forEach { routerOsClient.remove<RoutingRule>(it.id!!) }
    }

    // /ip/firewall/mangle/add action=mark-routing chain=prerouting dst-address-list=!lan-addr new-routing-mark=to-vpn passthrough=no src-address-list=vpn-users
    private fun ensureFirewallRule() {
        val rules = routerOsClient.list(
            IpFirewallMangle(
                action = IpFirewallMangle.Action.MARK_ROUTING,
                chain = "prerouting",
                newRoutingMark = routingMarker,
                srcAddressList = addressListName
            )
        )
        if (rules.isEmpty()) {
            routerOsClient.add(
                IpFirewallMangle(
                    action = IpFirewallMangle.Action.MARK_ROUTING,
                    chain = "prerouting",
                    dstAddress = "!192.168.42.0/24",
                    newRoutingMark = routingMarker,
                    passthrough = false,
                    srcAddressList = addressListName,
                    comment = vpnManComment
                )
            )
        }
    }

    private fun clearFirewallRule() {
        val rules = routerOsClient.list(
            IpFirewallMangle(
                action = IpFirewallMangle.Action.MARK_ROUTING
            )
        )
        rules.filter { it.comment == vpnManComment }.forEach { routerOsClient.remove<IpFirewallMangle>(it.id!!) }
    }
}
