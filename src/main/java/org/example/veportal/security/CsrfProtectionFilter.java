package org.example.veportal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Double-submit CSRF protection for browser requests authenticated by the HttpOnly cookie. */
@Component
public class CsrfProtectionFilter extends OncePerRequestFilter {
    private static final String COOKIE = "ve_csrf";
    private static final String HEADER = "X-CSRF-TOKEN";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isUnsafe(request.getMethod()) && !isPublicAuthRequest(request)) {
            String cookie = cookieValue(request, COOKIE);
            String header = request.getHeader(HEADER);
            if (cookie == null || header == null || !MessageDigest.isEqual(
                    cookie.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"CSRF validation failed\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isUnsafe(String method) {
        return !HttpMethod.GET.matches(method) && !HttpMethod.HEAD.matches(method)
                && !HttpMethod.OPTIONS.matches(method);
    }

    private boolean isPublicAuthRequest(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/api/auth/login".equals(path) || "/api/auth/forgot-password".equals(path)
                || "/api/auth/reset-password".equals(path);
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }
}
