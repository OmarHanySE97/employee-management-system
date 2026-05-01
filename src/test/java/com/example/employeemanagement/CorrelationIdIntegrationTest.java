package com.example.employeemanagement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.employeemanagement.exception.BusinessException;
import com.example.employeemanagement.filter.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        CorrelationIdIntegrationTest.TestSecurityConfig.class,
        CorrelationIdIntegrationTest.TestController.class
})
class CorrelationIdIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReuseIncomingCorrelationId() throws Exception {
        mockMvc.perform(get("/test/correlation")
                        .header(CorrelationIdFilter.HEADER_NAME, "incoming-correlation-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "incoming-correlation-id"));
    }

    @Test
    void shouldGenerateCorrelationIdAndIncludeItInErrorResponse() throws Exception {
        mockMvc.perform(get("/test/correlation/error"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.correlationId").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())))
                .andExpect(jsonPath("$.message").value("Invalid request data"));
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        @Order(0)
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/test/**")
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize
                            .anyRequest().permitAll()
                    )
                    .anonymous(anonymous -> anonymous
                            .principal("test-admin")
                            .authorities("ROLE_ADMIN")
                    )
                    .build();
        }
    }

    @RestController
    @RequestMapping("/test/correlation")
    static class TestController {

        @GetMapping
        String ok() {
            return "ok";
        }

        @GetMapping("/error")
        String error() {
            throw new BusinessException("Invalid request data");
        }
    }
}
