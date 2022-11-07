package nl.smeh.vpnman

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream

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

@RestController
@RequestMapping("/api")
class ApiController(private val nordVpnServers: NordVpnServers, private val routerOsClient: RouterOsClient) {

    private var status = Status.UNKNOWN
    private val objectMapper = ObjectMapper().registerKotlinModule()

    @GetMapping("/devices")
    fun getDevices(): Collection<IpDhcpServerLease> = routerOsClient.list()

    @GetMapping("/country")
    fun getCountries(): Collection<Country> = nordVpnServers.countries

    fun checkWireguardConfig() {

    }

    fun cleanup(): Unit {
//        val existingTable = routerOsClient.Routing().Table().retrieve(routingTableName)
//        if (existingTable != null) {
//            if (existingTable.comment?.contains(commentMarker) != true) {
//                TODO("Oops, table with name exists")
//            }
//            routerOsClient.Routing().Table().delete(existingTable.id!!)
//        }
//
//        routerOsClient.Ip().Firewall().AddressList().retrieve().filter { it.list == "vpn-users" }.forEach {
//            if (it.comment?.contains(commentMarker) != true) {
//                TODO("Oops. list with name exists")
//            }
//            routerOsClient.Ip().Firewall().AddressList().delete(it.id!!)
//        }
//
//
    }

    private fun message(response: OutputStream, message: String) {
        response.write(objectMapper.writeValueAsBytes(ResponseMessage(status, message)))
        response.write('\n'.code)
        response.flush()
    }

    @PostMapping("/connect")
    fun connect(): ResponseEntity<StreamingResponseBody> {
        // Find best service
        val body = StreamingResponseBody { response ->

            // TODO: check status
            status = Status.CONNECTING

            message(response, "Checking optimal server for country $country")

            val servers = nordVpnServers.getServers(
                Filter(
                    country_id = nordVpnServers.countriesByCode[country]?.id,
                    servers_technologies = listOf(nordVpnServers.technologiesByName[technologyName]!!.id)
                )
            )

            if (servers.isEmpty()) {
                status = Status.DISCONNECTED
                message(response, "No VPN servers found")
                return@StreamingResponseBody
            }
            val server = servers.first()
            message(response, "Connecting to server ${server.hostname}")

            message(response, "Setting up routing table for VPN")
            ensureRoutingTable()

            message(response, "Setting up route to VPN")
            ensureRouteToVpn()

            message(response, "Setting up VPN client interface")
            ensureWireguardInterface()

            message(response, "Setting up VPN connection")
            ensureWireguardPeer(server)

            message(response, "Setting up routing rule")
            ensureRoutingRule()

            message(response, "Setting up firewall rules")
            ensureFirewallRule()

            // TODO: NAT

            status = Status.CONNECTED
            message(response, "Connected!")
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_NDJSON).body(body)

        // Cleanup existing configuration
//        for (addressList in routerOsClient.Ip().Firewall().AddressList().retrieve()) {
//            if (addressList.list == "vpn-users" && addressList.comment?.contains(commentMarker) != true) {
//                TODO("Oops, list with name exists")
//            }
//        }


        //* /ip/firewall/address-list/add address=xxxx list=vpn-users
        // TODO("Set up VPN")

        // Route VPN users through VPN
//        routerOsClient.Ip().Firewall().AddressList()
//            .create(IpFirewallAddressList(list = addressListName, comment = commentMarker))
//        routerOsClient.Routing().Table()
//            .create(RoutingTable(fib = true, name = routingTableName, comment = commentMarker))
//        routerOsClient.Routing().Rule().create(
//            RoutingRule(
//                action = "lookup",
//                routingMark = routingMarker,
//                table = routingTableName,
//                comment = commentMarker
//            )
//        )
//        routerOsClient.Ip().Firewall().Mangle().create(
//            IpFirewallMangle(
//                action = IpFirewallMangle.Action.MARK_ROUTING,
//                chain = "prerouting",
//                dstAddressList = "!lan-addr",
//                newRoutingMark = routingMarker,
//                passthrough = false,
//                srcAddressList = addressListName,
//                comment = commentMarker
//            )
//        )
//        routerOsClient.Ip().Route().create(
//            IpRoute(
//                dstAddress = "0.0.0.0/0",
//                gateway = vpnGatewayAddress,
//                routingTable = routingTableName,
//                comment = commentMarker
//            )
//        )


        //* /routing/table/add fib name=to-vpn
        // /ip/route/add dst-address=0.0.0.0/0 gateway=10.5.0.1 routing-table=to-vpn
        ///interface/wireguard/add listen-port=51820 name=nordlynx
        ///interface/wireguard/peers/add allowed-address=0.0.0.0/0 endpoint-address=XXX endpoint-port=51820 interface=nordlynx public-key=xxx
        ///routing/rule/add action=lookup routing-mark=to-vpn table=to-vpn
        ///ip/firewall/mangle/add action=mark-routing chain=prerouting dst-address-list=!lan-addr new-routing-mark=to-vpn passthrough=no src-address-list=vpn-users
    }

    @PostMapping("/disconnect")
    fun disconnect(): ResponseEntity<StreamingResponseBody> {
        // Find best service
        val body = StreamingResponseBody { response ->

            // TODO: check status
            status = Status.DISCONNECTING

            message(response, "Closing VPN connection")
            clearWireguardPeer()

            message(response, "Clearing routing rule")
            clearRoutingRule()

            message(response, "Clearing firewall rules")
            clearFirewallRule()

            status = Status.DISCONNECTED
            message(response, "Disconnected!")
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_NDJSON).body(body)
    }


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
            routerOsClient.add(IpFirewallMangle(
                action = IpFirewallMangle.Action.MARK_ROUTING,
                chain = "prerouting",
                dstAddress = "!192.168.42.0/24",
                newRoutingMark = routingMarker,
                passthrough = false,
                srcAddressList = addressListName,
                comment = vpnManComment
            ))
        }
    }

    private fun clearFirewallRule() {
        val rules = routerOsClient.list(
            IpFirewallMangle(
                action = IpFirewallMangle.Action.MARK_ROUTING)
        )
        rules.filter { it.comment == vpnManComment }.forEach { routerOsClient.remove<IpFirewallMangle>(it.id!!) }
    }
}
