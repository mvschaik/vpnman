package nl.smeh.vpnman

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.support.beans
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory
import org.springframework.web.reactive.function.server.coRouter

@SpringBootApplication
class VpnmanApplication

fun main(args: Array<String>) {
    runApplication<VpnmanApplication>(*args) {
        addInitializers(beans {
            bean {
                val properties = ref<Environment>()
                RouterOsClient(
                    properties.getRequiredProperty("routeros.baseUrl"), RestTemplateBuilder().basicAuthentication(
                        properties.getRequiredProperty("routeros.username"),
                        properties.getRequiredProperty("routeros.password")
                    ).requestFactory { OkHttp3ClientHttpRequestFactory() }.build()
                )
            }
            bean {
                NordVpnServers(RestTemplateBuilder().build())
            }
            bean {
                val properties = ref<Environment>()
                val promotedCountryCodes = properties.getProperty("preferredCountries", "")
                        .split(",")
                        .filterNot { it.isEmpty() }

                val htmlController = HtmlController(ref(), ref(), promotedCountryCodes)
                val apiController = ApiController(ref(), ref())
                coRouter {
                    accept(MediaType.TEXT_HTML).nest {
                        GET("/", htmlController::home)
                    }
                    accept(MediaType.APPLICATION_JSON).nest {
                        "/api".nest {
                            GET("/status", apiController::getState)
                            POST("/connect", apiController::connect)
                            POST("/disconnect", apiController::disconnect)
                        }
                    }
                }
            }
        })
    }
}
