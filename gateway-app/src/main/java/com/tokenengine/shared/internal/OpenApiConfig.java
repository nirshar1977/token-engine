package com.tokenengine.shared.internal;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI for the engine's admin API. Proxied LLM traffic is intentionally not documented. */
@Configuration
class OpenApiConfig {

    private static final String ADMIN_KEY = "adminKey";

    @Bean
    OpenAPI tokenEngineOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Token Engine admin API")
                        .version("0.1.0")
                        .description("Usage and savings reporting. Send the x-admin-key header."))
                .components(new Components().addSecuritySchemes(ADMIN_KEY, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(AdminKeyFilter.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(ADMIN_KEY));
    }
}
