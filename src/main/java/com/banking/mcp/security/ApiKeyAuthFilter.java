package com.banking.mcp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${app.mcp.security.api-key}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/mcp")) {

            log.info("API Key authentication attempt for path: {}", path);
            String apiKey = request.getHeader("X-API-Key");

            log.info("Received X-API-Key: {}", apiKey);

            if (apiKey == null || !apiKey.equals(expectedApiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized");
                return;
            }

            // Create authenticated user (no roles needed)
            AbstractAuthenticationToken authentication = new AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {
                        @Override
                        public Object getCredentials() { return null; }

                        @Override
                        public Object getPrincipal() {
                            return "mcp-client";
                        }
                    };

            authentication.setAuthenticated(true);

            // Set into context
            org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            log.info("API Key authentication successful for path: {}, X-API-Key: {}", path, apiKey);
        }

        filterChain.doFilter(request, response);
    }
}