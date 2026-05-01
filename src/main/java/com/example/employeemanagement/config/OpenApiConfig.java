package com.example.employeemanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    OpenAPI employeeManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Management System API")
                        .description("Backend service for managing employees and departments.")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }

    @Bean
    OperationCustomizer jwtSecurityOperationCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            String beanType = handlerMethod.getBeanType().getSimpleName();
            if (!"AuthController".equals(beanType)) {
                operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
            }
            return operation;
        };
    }

    @Bean
    InfoContributor applicationInfoContributor(
            @Value("${app.name}") String appName,
            @Value("${app.version}") String appVersion,
            @Value("${app.description}") String appDescription
    ) {
        return builder -> builder.withDetail("app", new AppInfo(appName, appVersion, appDescription));
    }

    private record AppInfo(String name, String version, String description) {
    }
}
