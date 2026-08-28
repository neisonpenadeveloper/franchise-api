package com.accenture.franchise.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentacion OpenAPI expuesta en {@code /swagger-ui.html}.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI franchiseOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Franchise API")
                .version("1.0.0")
                .description("API reactiva para gestionar franquicias, sus sucursales y los productos "
                        + "ofertados en cada sucursal.")
                .contact(new Contact().name("Prueba tecnica backend"))
                .license(new License().name("MIT")));
    }
}
