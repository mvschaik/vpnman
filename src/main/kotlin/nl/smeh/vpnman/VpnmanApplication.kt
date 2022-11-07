package nl.smeh.vpnman

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.support.beans
import org.springframework.core.env.Environment
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory
import org.springframework.web.servlet.function.router

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
                val htmlController = HtmlController(ref(), ref())
                val apiController = ApiController(ref(), ref())
                router {

                    GET("", htmlController::blog)

                }
            }
        })
    }
}
