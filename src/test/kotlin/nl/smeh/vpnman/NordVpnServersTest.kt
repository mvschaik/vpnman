package nl.smeh.vpnman

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.anyOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI


internal class NordVpnServersTest {
    private val restTemplate: RestTemplate = mockk()
    private val underTest = NordVpnServers(restTemplate)

    @Test
    fun testGetCountry() {
        val us = Country(id = 1, code = "us", name = "USA")
        val nl = Country(id = 2, code = "nl", name = "Netherlands")
        val fr = Country(id = 3, code = "fr", name = "France")
        val countryList = listOf(us, nl, fr)

        every {
            restTemplate.exchange<Collection<Country>>(match<URI> {
                UriComponentsBuilder.fromUri(it).build().queryParams["action"]!!.contains("servers_countries")
            }, HttpMethod.GET, null, object : ParameterizedTypeReference<Collection<Country>>() {})
        } returns ResponseEntity(countryList, HttpStatus.OK)

        assertThat(underTest.countriesByCode["nl"]).isEqualTo(nl)
    }

    @Test
    fun testGetServers() {
        val serverList = listOf(
            Server(
                123, "NL32", "nl32.nordvpn.com", "nl32.nordvpn.com", "nl32.nordvpn.com", 6, 5, "ok", emptyList()
            )
        )

        val uri = CapturingSlot<URI>()
        every {
            restTemplate.exchange(capture(uri),
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<Collection<Server>>() {})
        } returns ResponseEntity(
            serverList, HttpStatus.OK
        )

        assertThat(
            underTest.getServers(
                Filter(
                    country_id = 3, servers_technologies = listOf(35)
                )
            )
        ).isEqualTo(serverList)
        assertThat(uri.captured).hasQuery("action=servers_recommendations&filters={\"country_id\":3,\"servers_technologies\":[35]}")
    }

}
