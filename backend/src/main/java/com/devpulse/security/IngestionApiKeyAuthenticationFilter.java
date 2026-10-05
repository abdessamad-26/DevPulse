package com.devpulse.security;

import com.devpulse.service.IngestionApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class IngestionApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-API-Key";
    private static final Pattern INGESTION_PATH =
            Pattern.compile("^/api/projects/\\d+/(metrics|logs)$");

    private final IngestionApiKeyService apiKeyService;

    public IngestionApiKeyAuthenticationFilter(IngestionApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String presentedKey = request.getHeader(HEADER_NAME);
        if (presentedKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!"POST".equals(request.getMethod())
                || !INGESTION_PATH.matcher(request.getRequestURI().substring(request.getContextPath().length())).matches()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "API keys are restricted to metric and log ingestion");
            return;
        }

        IngestionApiKeyPrincipal principal = apiKeyService.authenticate(presentedKey);
        if (principal == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid ingestion API key");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_INGESTION")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
