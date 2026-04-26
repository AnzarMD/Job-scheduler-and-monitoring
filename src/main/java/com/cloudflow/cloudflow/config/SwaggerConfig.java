package com.cloudflow.cloudflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CloudFlow API",
                version = "1.0",
                description = "Distributed Job Scheduling & Monitoring Platform"
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter your JWT token from the /auth/login endpoint"
)
public class SwaggerConfig {
    // All configuration is done via annotations above.
    // SpringDoc reads them and adds the Authorize button to Swagger UI.
}