package nl.smeh.vpnman

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.web.util.DefaultUriBuilderFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@JsonIgnoreProperties(ignoreUnknown = true)
data class Country(val id: Int, val code: String, val name: String)

object MapDeserializer : JsonDeserializer<Map<String, String>>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Map<String, String> {
        val node = p.readValueAsTree<JsonNode>()
        if (node.isArray) {
            return node.associate { it["name"].asText() to it["value"].asText() }
        }
        return mapOf()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServerTechnology(
    val id: Int,
    val name: String,
    @JsonDeserialize(using = MapDeserializer::class) val metadata: Map<String, String>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServerGroup(
    val id: Int,
    val identifier: String,
    val name: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Server(
    val id: Int,
    val name: String,
    val hostname: String,
    val station: String,
    val ipv6_station: String,
    val load: Int,
    val cpt: Int,
    val status: String,
    val technologies: List<ServerTechnology>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Filter(
    val country_id: Int? = null, val servers_technologies: List<Int>? = null, val servers_groups: List<Int>? = null
)

const val ENDPOINT_URL = "https://nordvpn.com/wp-admin/admin-ajax.php"

class NordVpnServers(
    private val restTemplate: RestTemplate
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val jsonMapper = jacksonObjectMapper()

    val countries: Collection<Country> by lazy { fetchCountries() }
    val technologies: Collection<ServerTechnology> by lazy { fetchTechnologies() }
    val groups: Collection<ServerGroup> by lazy { fetchGroups() }

    val countriesByCode: Map<String, Country> by lazy { countries.associateBy { it.code } }
    val technologiesByName: Map<String, ServerTechnology> by lazy { technologies.associateBy { it.name } }

    fun getServers(filter: Filter): Collection<Server> = fetchList("servers_recommendations", filter)

    private final fun fetchCountries(): Collection<Country> = fetchList("servers_countries")

    private final fun fetchGroups(): Collection<ServerGroup> = fetchList("servers_grouos")

    private final fun fetchTechnologies(): Collection<ServerTechnology> =
        fetchList("servers_technologies")

    private final inline fun <reified T> fetchList(
        name: String, filters: Filter? = null
    ): Collection<T> {
        logger.info("Fetching $name from NordVPN")

        val uri = DefaultUriBuilderFactory().uriString(ENDPOINT_URL).queryParam("action", name)
        val uriParams = mutableMapOf<String, String>()
        if (filters != null) {
            println("Searchign with filters ${filters}")
            uri.queryParam("filters", "{filters}")
            uriParams["filters"] = jsonMapper.writeValueAsString(filters)
        }

        return restTemplate.exchange(
            uri.build(uriParams),
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<Collection<T>>() {}).body
            ?: throw java.lang.RuntimeException("Error fetching servers")
    }
}
