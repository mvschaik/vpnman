package nl.smeh.vpnman

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.*
import java.net.URLEncoder

annotation class ResourcePath(val value: String)

@ResourcePath("/interface/wireguard")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class InterfaceWireguard(
    @JsonProperty(".id") val id: String? = null,

    @JsonProperty("comment") val comment: String? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,
    @JsonProperty("listen-port") val listenPort: Int? = null,
    @JsonProperty("mtu") val mtu: Int? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("private-key") val privateKey: String? = null,

    // Read-only
    @JsonProperty("public-key") val publicKey: String? = null,
    @JsonProperty("running") val running: Boolean? = null,
)

@ResourcePath("/interface/wireguard/peers")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class InterfaceWireguardPeers(
    @JsonProperty(".id") val id: String? = null,

    @JsonProperty("allowed-address") val allowedAddress: String? = null,
    @JsonProperty("comment") val comment: String? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,
    @JsonProperty("endpoint-address") val endpointAddress: String? = null,
    @JsonProperty("endpoint-port") val endpointPort: Int? = null,
    @JsonProperty("interface") val iface: String? = null,
    @JsonProperty("persistent-keepalive") val persistentKeepalive: Int? = null,
    @JsonProperty("preshared-key") val presharedKey: String? = null,
    @JsonProperty("public-key") val publicKey: String? = null,

    // Read-only
    @JsonProperty("current-endpoint-address") val currentEndpointAddress: String? = null,
    @JsonProperty("current-endpoint-port") val currentEndpointPort: Int? = null,
    @JsonProperty("last-handshake") val lastHandshake: String? = null,
    @JsonProperty("rx") val rx: Int? = null,
    @JsonProperty("tx") val tx: Int? = null,
)

@ResourcePath("/routing/table")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RoutingTable(
    @JsonProperty(".id") val id: String? = null,

    @JsonProperty("name") val name: String? = null,
    @JsonProperty("comment") val comment: String? = null,
    @JsonProperty("fib") val fib: Boolean? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,

    // Read-only
    @JsonProperty("dynamic") val dynamic: Boolean? = null,
    @JsonProperty("invalid") val invalid: Boolean? = null,
)

@ResourcePath("/routing/rule")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RoutingRule(
    @JsonProperty(".id") val id: String? = null,
    @JsonProperty(".nextid") val nextId: String? = null,

    @JsonProperty("action") val action: String? = null,
    @JsonProperty("comment") val comment: String? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,
    @JsonProperty("dst-address") val dstAddress: String? = null,
    @JsonProperty("interface") val iface: String? = null,
    @JsonProperty("min-prefix") val minPrefix: String? = null,
    @JsonProperty("routing-mark") val routingMark: String? = null,
    @JsonProperty("src-address") val srcAddress: String? = null,
    @JsonProperty("table") val table: String? = null,

    // Read-only
    @JsonProperty("inactive") val inactive: Boolean? = null,
)

@ResourcePath("/ip/address")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class IpAddress(
    @JsonProperty(".id") val id: String? = null,

    @JsonProperty("comment") val comment: String? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,

    @JsonProperty("address") val address: String? = null,
    @JsonProperty("broadcast") val broadcast: String? = null,
    @JsonProperty("interface") val iface: String? = null,
    @JsonProperty("network") val network: String? = null,

    // Read-only properties
    @JsonProperty("actual-interface") val actualInterface: String? = null
)

@ResourcePath("/ip/firewall/nat")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class IpFirewallNat(
    @JsonProperty(".id") val id: String? = null,

    @JsonProperty("comment") val comment: String? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,

    @JsonProperty("action") val action: Action?,
    @JsonProperty("address-list") val addressList: String? = null,
    @JsonProperty("address-list-timeout") val addressListTimeout: String? = null,
    @JsonProperty("chain") val chain: String? = null,
    @JsonProperty("connection-bytes") val connectionBytes: Int? = null,
    @JsonProperty("connection-limit") val connectionLimit: String? = null,
    @JsonProperty("connection-mark") val connectionMark: String? = null,
    @JsonProperty("connection-rate") val connectionRate: Int? = null,
    @JsonProperty("connection-type") val connectionType: String? = null,
    @JsonProperty("content") val content: String? = null,
    @JsonProperty("dscp") val dscp: Int? = null,
    @JsonProperty("dst-address") val dstAddress: String? = null,
    @JsonProperty("dst-address-list") val dstAddressList: String? = null,
    @JsonProperty("dst-address-type") val dstAddressType: String? = null,
    @JsonProperty("dst-limit") val dstLimit: String? = null,
    @JsonProperty("dst-port") val dstPort: Int? = null,
    @JsonProperty("fragment") val fragment: Boolean? = null,
    @JsonProperty("hotspot") val hotspot: String? = null,
    @JsonProperty("icmp-options") val icmpOptions: Int? = null,
    @JsonProperty("in-bridge-port") val inBridgePort: String? = null,
    @JsonProperty("in-interface") val inInterface: String? = null,
    @JsonProperty("ingress-priority") val ingressPriority: Int? = null,
    @JsonProperty("ipsec-policy") val ipsecPolicy: String? = null,
    @JsonProperty("ipv4-options") val ipv4Options: String? = null,
    @JsonProperty("jump-target") val jumpTarget: String? = null,
    @JsonProperty("layer7-protocol") val layer7Protocol: String? = null,
    @JsonProperty("limit") val limit: String? = null,
    @JsonProperty("log-prefix") val logPrefix: String? = null,
    @JsonProperty("nth") val nth: Int? = null,
    @JsonProperty("out-bridge-port") val outBridgePort: String? = null,
    @JsonProperty("out-interface") val outInterface: String? = null,
    @JsonProperty("packet-mark") val packetMark: String? = null,
    @JsonProperty("packet-size") val packetSize: Int? = null,
    @JsonProperty("per-connection-classifier") val perConnectionClassifier: String? = null,
    @JsonProperty("port") val port: Int? = null,
    @JsonProperty("protocol") val protocol: String? = null,
    @JsonProperty("psd") val psd: String? = null,
    @JsonProperty("random") val randon: Int? = null,
    @JsonProperty("routing-mark") val routingMark: String? = null,
    @JsonProperty("src-address") val srcAddress: String? = null,
    @JsonProperty("src-address-list") val srcAddressList: String? = null,
    @JsonProperty("src-address-type") val srcAddressType: String? = null,
    @JsonProperty("src-port") val srcPort: Int? = null,
    @JsonProperty("src-mac-address") val srcMacAddress: String? = null,
    @JsonProperty("tcp-mss") val tcpMss: Int? = null,
    @JsonProperty("time") val time: String? = null,
    @JsonProperty("to-addresses") val toAddresses: String? = null,
    @JsonProperty("to-port") val toPort: String? = null,
    @JsonProperty("ttl") val ttl: String? = null,

    // Read-only
    @JsonProperty("bytes") val bytes: Int? = null,
    @JsonProperty("dynamic") val dynamic: Boolean? = null,
    @JsonProperty("invalid") val invalid: Boolean? = null,
    @JsonProperty("packets") val packets: Int? = null,
) {
    enum class Action {
        @JsonProperty("accept")
        ACCEPT,

        @JsonProperty("add-dst-to-address-list")
        ADD_DST_TO_ADDRESS_LIST,

        // ...
        @JsonProperty("masquerade")
        MASQUERADE,

        @JsonProperty("src-nat")
        SRC_NAT,

        @JsonProperty("dst-nat")
        DST_NAT,
        // ...

    }
}

@ResourcePath("/ip/firewall/mangle")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class IpFirewallMangle(
    @JsonProperty(".id") val id: String? = null,

    @JsonProperty("comment") val comment: String? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,
    @JsonProperty("action") val action: Action?,
    @JsonProperty("address-list") val addressList: String? = null,
    @JsonProperty("address-list-timeout") val addressListTimeout: Int? = null,
    @JsonProperty("chain") val chain: String? = null,
    @JsonProperty("connection-bytes") val connectionBytes: Int? = null,
    @JsonProperty("connection-limit") val connectionLimit: String? = null,
    @JsonProperty("connection-mark") val connectionMark: String? = null,
    @JsonProperty("connection-nat-state") val connectionNatState: String? = null,
    @JsonProperty("connection-rate") val connectionRate: Int? = null,
    @JsonProperty("connection-state") val connectionState: String? = null,
    @JsonProperty("connection-type") val connectionType: String? = null,
    @JsonProperty("content") val content: String? = null,
    @JsonProperty("dscp") val dscp: Int? = null,
    @JsonProperty("dst-address") val dstAddress: String? = null,
    @JsonProperty("dst-address-list") val dstAddressList: String? = null,
    @JsonProperty("dst-address-type") val dstAddressType: String? = null,
    @JsonProperty("dst-limit") val dstLimit: String? = null,
    @JsonProperty("dst-port") val dstPort: Int? = null,
    @JsonProperty("fragment") val fragment: Boolean? = null,
    @JsonProperty("hotspot") val hotspot: String? = null,
    @JsonProperty("icmp-options") val icmpOptions: Int? = null,
    @JsonProperty("in-bridge-port") val inBridgePort: String? = null,
    @JsonProperty("in-bridge-port-list") val inBridgePortList: String? = null,
    @JsonProperty("in-interface") val inInterface: String? = null,
    @JsonProperty("in-interface-list") val inInterfaceList: String? = null,
    @JsonProperty("ingress-priority") val ingressPriority: Int? = null,
    @JsonProperty("ipsec-policy") val ipsecPolicy: String? = null,
    @JsonProperty("ipv4-options") val ipv4Options: String? = null,
    @JsonProperty("jump-target") val jumpTarget: String? = null,
    @JsonProperty("layer7-protocol") val layer7Protocol: String? = null,
    @JsonProperty("limit") val limit: String? = null,
    @JsonProperty("log") val log: Boolean? = null,
    @JsonProperty("log-prefix") val logPrefix: String? = null,
    @JsonProperty("new-dscp") val newDscp: Int? = null,
    @JsonProperty("new-mss") val newMss: String? = null,
    @JsonProperty("new-packet-mark") val newPacketMark: String? = null,
    @JsonProperty("new-priority") val newPriority: Int? = null,
    @JsonProperty("new-routing-mark") val newRoutingMark: String? = null,
//    @JsonProperty("new-connection-mark") val newConnectionMark: String? = null,
    @JsonProperty("new-ttl") val newTtl: Int? = null,
    @JsonProperty("nth") val nth: Int? = null,
    @JsonProperty("out-bridge-port") val outBridgePort: String? = null,
    @JsonProperty("out-bridge-port-list") val outBridgePortList: String? = null,
    @JsonProperty("out-interface") val outInterface: String? = null,
    @JsonProperty("out-interface-list") val outInterfaceList: String? = null,
//    @JsonProperty("p2p") val p2p: String? = null,
    @JsonProperty("packet-mark") val packetMark: String? = null,
    @JsonProperty("packet-size") val packetSize: Int? = null,
    @JsonProperty("passthrough") val passthrough: Boolean? = null,
    @JsonProperty("per-connection-classifier") val perConnectionClassifier: String? = null,
    @JsonProperty("port") val port: Int? = null,
    @JsonProperty("protocol") val protocol: String? = null,
    @JsonProperty("psd") val psd: String? = null,
    @JsonProperty("random") val randon: Int? = null,
    @JsonProperty("routing-mark") val routingMark: String? = null,
    @JsonProperty("route-dst") val routeDst: String? = null,
    @JsonProperty("priority") val priority: Int? = null,
    @JsonProperty("src-address") val srcAddress: String? = null,
    @JsonProperty("src-address-list") val srcAddressList: String? = null,
    @JsonProperty("src-address-type") val srcAddressType: String? = null,
    @JsonProperty("src-port") val srcPort: Int? = null,
    @JsonProperty("src-mac-address") val srcMacAddress: String? = null,
    @JsonProperty("tcp-flags") val tcpFlags: Int? = null,
    @JsonProperty("tcp-mss") val tcpMss: Int? = null,
    @JsonProperty("time") val time: String? = null,
    @JsonProperty("tls-host") val tlsHost: String? = null,
    @JsonProperty("ttl") val ttl: String? = null,


    // Read-only
    @JsonProperty("bytes") val bytes: Int? = null,
    @JsonProperty("dynamic") val dynamic: Boolean? = null,
    @JsonProperty("invalid") val invalid: Boolean? = null,
    @JsonProperty("packets") val packets: Int? = null,
) {
    enum class Action {
        @JsonProperty("accept")
        ACCEPT,

        @JsonProperty("add-dst-to-address-list")
        ADD_DST_TO_ADDRESS_LIST,

        // ...
        @JsonProperty("mark-routing")
        MARK_ROUTING,
        // ...
    }
}

@ResourcePath("/ip/firewall/address-list")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class IpFirewallAddressList(
    @JsonProperty(".id") val id: String? = null,
    @JsonProperty("comment") val comment: String? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,

    @JsonProperty("address") val address: String? = null,
    @JsonProperty("list") val list: String? = null,
    @JsonProperty("timeout") val timeout: Int? = null,

    // Read-only
    @JsonProperty("creation-time") val creationTime: String? = null,
    @JsonProperty("dynamic") val dynamic: Boolean? = null,
)

@ResourcePath("/ip/dhcp-server")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class IpDhcpServer(
    @JsonProperty(".id") val id: String? = null,

    @JsonProperty("add-arp") val addArp: Boolean? = null,
    @JsonProperty("address-pool") val addressPool: String? = null,
    @JsonProperty("allow-dual-stack-queue") val allowDualStackQueue: Boolean? = null,
    @JsonProperty("authoritative") val authoritative: Boolean? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,
    @JsonProperty("dynamic") val dynamic: Boolean? = null,
    @JsonProperty("interface") val iface: String? = null,
    @JsonProperty("invalid") val invalid: Boolean? = null,
    @JsonProperty("lease-script") val leaseScript: String? = null,
    @JsonProperty("lease-time") val leaseTime: String? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("use-radius") val useRadius: Boolean? = null,
)

@ResourcePath("/ip/dhcp-server/lease")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class IpDhcpServerLease(
    @JsonProperty(".id") val id: String? = null,

    @JsonProperty("active-address") val activeAddress: String? = null,
    @JsonProperty("active-mac-address") val activeMacAddress: String? = null,
    @JsonProperty("active-server") val activeServer: String? = null,
    @JsonProperty("address") val address: String? = null,
    @JsonProperty("address-lists") val addressLists: String? = null,
    @JsonProperty("blocked") val blocked: Boolean? = null,
    @JsonProperty("dhcp-option") val dhcpOption: String? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,
    @JsonProperty("dynamic") val dynamic: Boolean? = null,
    @JsonProperty("expires-after") val expiresAfter: String? = null,
    @JsonProperty("host-name") val hostName: String? = null,
    @JsonProperty("last-seen") val lastSeen: String? = null,
    @JsonProperty("mac-address") val macAddress: String? = null,
    @JsonProperty("radius") val radius: Boolean? = null,
    @JsonProperty("server") val server: String? = null,
    @JsonProperty("status") val status: String? = null,
)

@ResourcePath("/ip/route")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class IpRoute(
    @JsonProperty(".id") val id: String? = null,

    @JsonProperty("dst-address") val dstAddress: String? = null,
    @JsonProperty("routing-table") val routingTable: String? = null,
    @JsonProperty("pref-src") val prefSrc: String? = null,
    @JsonProperty("gateway") val gateway: String? = null,
    @JsonProperty("immediate-gw") val immediateGw: String? = null,
    @JsonProperty("distance") val distance: Int? = null,
    @JsonProperty("scope") val scope: Int? = null,
    @JsonProperty("target-scope") val targetScope: Int? = null,
    @JsonProperty("vrf-interface") val vrfInterface: String? = null,
    @JsonProperty("suppress-hw-offload") val suppressHwOffload: Boolean? = null,
    @JsonProperty("local-address") val localAddress: String? = null,
    @JsonProperty("blackhole") val blackhole: Boolean? = null,
    @JsonProperty("check-gateway") val checkGateway: String? = null,
    @JsonProperty("comment") val comment: String? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,

    // Read-only
    @JsonProperty("dynamic") val dynamic: Boolean? = null,
    @JsonProperty("ecmp") val ecmp: Boolean? = null,
    @JsonProperty("hw-offloaded") val hwOffloaded: Boolean? = null,
    @JsonProperty("inactive") val inactive: Boolean? = null,
    @JsonProperty("static") val static: Boolean? = null,
)

@ResourcePath("/system/resource")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SystemResource(
    @JsonProperty("architecture-name") val architectureName: String? = null,
    @JsonProperty("board-name") val boardName: String? = null,
    @JsonProperty("build-time") val buildTime: String? = null,
    @JsonProperty("cpu") val cpu: String? = null,
    @JsonProperty("cpu-count") val cpuCount: Int? = null,
    @JsonProperty("cpu-frequency") val cpuFrequency: Int? = null,
    @JsonProperty("cpu-load") val cpuLoad: Int? = null,
    @JsonProperty("factory-software") val factorySoftware: String? = null,
    @JsonProperty("free-hdd-space") val freeHddSpace: Int? = null,
    @JsonProperty("free-memory") val freeMemory: Int? = null,
    @JsonProperty("platform") val platform: String? = null,
    @JsonProperty("total-hdd-space") val totalHddSpace: Int? = null,
    @JsonProperty("total-memory") val totalMemory: Int? = null,
    @JsonProperty("uptime") val uptime: String? = null,
    @JsonProperty("version") val version: String? = null,
    @JsonProperty("write-sect-since-reboot") val writeSectSinceReboot: Int? = null,
    @JsonProperty("write-sect-total") val writeSectTotal: Int? = null,
)

class RouterOsClient(
    private val baseUrl: String, private val restTemplate: RestTemplate
) {
    private val objectMapper = ObjectMapper().registerKotlinModule()

    inline fun <reified T : Any> add(newResource: T): T =
        add(getPath(newResource::class.java), newResource, T::class.java)

    fun <T : Any> add(path: String, newResource: T, type: Class<T>): T {
        val response = restTemplate.exchange("$baseUrl/rest$path", HttpMethod.PUT, HttpEntity(newResource), type)
        return response.body ?: throw RuntimeException("Error with POST: $response")
    }

    inline fun <reified T : Any> get(id: String): T = get(id, getPath(T::class.java), T::class.java)

    fun <T : Any> get(id: String, path: String, type: Class<T>): T {
        val response = restTemplate.getForEntity("$baseUrl/rest$path/$id}", type)
        return response.body ?: throw RuntimeException("Error with GET: $response")
    }

    inline fun <reified T : Any> list(filters: T? = null): Collection<T> =
        list(getPath(T::class.java), filters, object : ParameterizedTypeReference<Collection<T>>() {})


    fun <T : Any> list(path: String, filters: T?, type: ParameterizedTypeReference<Collection<T>>): Collection<T> {
        val response = restTemplate.exchange(
            "$baseUrl/rest$path${queryStringFromFilters(filters)}", HttpMethod.GET, null, type
        )
        return response.body ?: throw RuntimeException("Error fetching list at $path: $response")
    }

    inline fun <reified T : Any> set(id: String, newResource: T): T? =
        set(id, getPath(newResource::class.java), newResource, T::class.java)

    fun <T : Any> set(id: String, path: String, newResource: T, type: Class<T>): T? =
        restTemplate.patchForObject("$baseUrl/rest$path/$id", newResource, type)

    inline fun <reified T : Any> remove(id: String): Unit = remove(id, getPath(T::class.java))

    fun remove(id: String, path: String): Unit = restTemplate.delete("$baseUrl/rest$path/$id")

    fun <T : Any> getPath(resourceClass: Class<T>): String {
        if (!resourceClass.isAnnotationPresent(ResourcePath::class.java)) {
            throw RuntimeException("Can't access resources without @ResourcePath annotation.")
        }
        return resourceClass.getAnnotation(ResourcePath::class.java).value
    }

    fun <T> queryStringFromFilters(filters: T?): String {
        if (filters == null) return ""
        val tree: ObjectNode = objectMapper.valueToTree(filters)
        val pairs = mutableMapOf<String, String>()
        tree.fields().forEach { pairs[it.key] = it.value.asText() }
        return "?" + pairs.toList().joinToString("&") { "${it.first}=${URLEncoder.encode(it.second, "utf-8")}" }
    }
}
