package com.example.cleopatra.game;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:2027").description("Development Server"),
                        new Server().url("https://cleopatra-game.com").description("Production Server")
                ))
                .info(new Info()
                        .title("Cleopatra Game API")
                        .description("API для игры 'Кто хочет стать миллионером' в социальной сети Cleopatra")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Cleopatra Team")
                                .email("support@cleopatra-game.com")
                                .url("https://cleopatra-game.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                );
    }
}
