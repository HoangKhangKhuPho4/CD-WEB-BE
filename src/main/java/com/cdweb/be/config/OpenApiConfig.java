package com.cdweb.be.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Value("${app.server.url:http://localhost:8080}")
  private String serverUrl;

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("CD Web E-Commerce API Documentation")
                .version("1.0")
                .description(
                    "Hệ thống API dành cho dự án Website Thương mại điện tử CD-Web. Tài liệu này cung cấp chi tiết về các endpoint, tham số và cấu trúc dữ liệu cho Frontend Team.")
                .contact(new Contact().name("Backend Team").email("support@cdweb.com"))
                .license(new License().name("Apache 2.0").url("http://springdoc.org")))
        .servers(List.of(new Server().url(serverUrl).description("Development Server")))
        .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "Nhập mã JWT Token vào đây để gọi các API yêu cầu xác thực.")));
  }
}
