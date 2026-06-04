package com.jayanta.usermanagement.config;

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
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("👤 User Management Service 👤")
                        .description("""

     **Jayanta Ghosh - cs23m513@smail.iitm.ac.in**
    """)

                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Jayanta Ghosh")
                                .email("cs23m513@smail.iitm.ac.in")))

                // JWT AUTH FOR ALL PROTECTED ENDPOINTS
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))

                //  SECURITY SCHEME DEFINITION
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)));
    }
}
