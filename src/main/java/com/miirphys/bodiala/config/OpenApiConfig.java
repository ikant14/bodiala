package com.miirphys.bodiala.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the Swagger UI (served at {@code /swagger-ui.html};
 * spec at {@code /v3/api-docs}).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bodialaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("bodiala — Hotelbeds integration API")
                        .version("0.0.1")
                        .description("""
                                Endpoints for the Hotelbeds (APItude) integration.

                                - **/api/static-data/**** — sync the Hotelbeds catalog into the local cache \
                                and query countries / cities / hotels.
                                - **/api/search/**** — live availability search (aggregated, provider-tagged).
                                - **/api/bookings/**** — the prebook / book / cancel chain.
                                """)
                        .license(new License().name("Proprietary")));
    }
}
