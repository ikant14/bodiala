package com.miirphys.bodiala.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the Swagger UI (served at {@code /swagger-ui.html};
 * spec at {@code /v3/api-docs}).
 */
@Configuration
public class OpenApiConfig {

    /** Security-scheme name that staff-only operations reference with {@code @SecurityRequirement}. */
    public static final String STAFF_SCHEME = "staff";

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

                                Operations marked with a lock are staff-only: click **Authorize** and enter \
                                the staff login (BODIALA_STAFF_USERNAME / BODIALA_STAFF_PASSWORD).
                                """)
                        .license(new License().name("Proprietary")))
                .components(new Components().addSecuritySchemes(STAFF_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("Staff login (HTTP Basic)")));
    }
}
