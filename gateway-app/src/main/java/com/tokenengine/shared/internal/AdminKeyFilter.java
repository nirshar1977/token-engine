package com.tokenengine.shared.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protects the engine's own admin API ({@code /engine/api/**}) with a static admin key.
 * Proxied LLM traffic is not touched by this filter.
 */
@Component
class AdminKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "x-admin-key";

    private final byte[] adminKey;

    AdminKeyFilter(EngineSecurityProperties properties) {
        this.adminKey = properties.adminKey() == null ? new byte[0]
                : properties.adminKey().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/engine/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        boolean ok = adminKey.length > 0 && supplied != null
                && MessageDigest.isEqual(adminKey, supplied.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"missing or invalid x-admin-key\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
