package com.banking.mcp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        return new ApiKeyAuthFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ApiKeyAuthFilter apiKeyAuthFilter) throws Exception {

        return http
                .csrf(csrf -> csrf.disable()) // important for MCP POST
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/mcp/**").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}