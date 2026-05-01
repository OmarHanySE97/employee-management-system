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

/**
 * Configures OpenAPI metadata, JWT security documentation, and actuator info details.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Builds the base OpenAPI definition exposed through Springdoc.
     *
     * @return the configured OpenAPI model
     */
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

    /**
     * Applies JWT bearer security requirements to documented endpoints except authentication APIs.
     *
     * @return an operation customizer that enriches secured operations
     */
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

    /**
     * Publishes application metadata to the actuator info endpoint.
     *
     * @param appName configured application name
     * @param appVersion configured application version
     * @param appDescription configured application description
     * @return an info contributor for actuator
     */
    @Bean
    InfoContributor applicationInfoContributor(
            @Value("${app.name}") String appName,
            @Value("${app.version}") String appVersion,
            @Value("${app.description}") String appDescription
    ) {
        return builder -> builder.withDetail("app", new AppInfo(appName, appVersion, appDescription));
    }

    /**
     * Immutable representation of application metadata exposed via actuator.
     */
    private record AppInfo(String name, String version, String description) {
    }
}
