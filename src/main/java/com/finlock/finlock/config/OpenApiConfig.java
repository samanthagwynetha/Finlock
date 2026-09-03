package com.finlock.finlock.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI finlockOpenAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste your JWT token here (without 'Bearer ' prefix)");

        return new OpenAPI()
                .info(new Info()
                        .title("FinLock API")
                        .description("""
                                Distributed Wallet System API
                                
                                **How to use:**
                                1. Register via POST /api/auth/register
                                2. Login via POST /api/auth/login to get a JWT token
                                3. Click Authorize and paste your token
                                4. Try any protected endpoint
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Samantha Gwyneth Arsua")
                                .url("https://github.com/samanthagwynetha/Finlock")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerScheme));
    }
}