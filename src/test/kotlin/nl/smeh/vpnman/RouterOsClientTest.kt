package nl.smeh.vpnman

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class RouterOsClientTest {
    private val restTemplate: RestTemplate = mockk()
    private val underTest = RouterOsClient("https://router", restTemplate)

    @Test
    fun testList() {
        val result = listOf(RoutingTable(id = "*1", name = "main", disabled = false, dynamic = true, invalid = false))
        every {
            restTemplate.exchange("https://router/rest/routing/table", HttpMethod.GET, null, object : ParameterizedTypeReference<Collection<RoutingTable>>() {})
        } returns ResponseEntity(result, HttpStatus.OK)

        assertThat(underTest.list<RoutingTable>()).isEqualTo(result)
    }
}
