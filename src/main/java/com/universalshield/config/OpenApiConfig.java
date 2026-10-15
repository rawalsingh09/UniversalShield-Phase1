package com.universalshield.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata. The spec is served at /api/openapi and the UI at /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI universalShieldOpenApi() {
        return new OpenAPI().info(new Info()
                .title("UniversalShield API")
                .version("1.0.0")
                .description("One shield for your digital life. Session-cookie authentication; state-changing requests "
                        + "require the X-XSRF-TOKEN header matching the XSRF-TOKEN cookie (GET /api/auth/csrf).")
                .license(new License().name("MIT")));
    }
}
