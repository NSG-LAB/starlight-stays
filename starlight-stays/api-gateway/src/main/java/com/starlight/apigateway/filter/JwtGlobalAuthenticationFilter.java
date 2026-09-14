package com.starlight.apigateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtGlobalAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:${JWT_SECRET:StarlightStaysSuperSecretKeyForJwtValidation2026}}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // 1. Bypass authentication for public endpoints, WebSockets, Actuator, and Fallback
        if (path.contains("/login") || 
            path.contains("/register") || 
            path.contains("/ws-starlight") || 
            path.startsWith("/actuator") || 
            path.startsWith("/fallback") ||
            (HttpMethod.GET.equals(method) && (path.startsWith("/api/rooms") || path.startsWith("/api/reviews")))) {
            return chain.filter(exchange);
        }

        // 2. Extract Authorization Header
        if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        // 3. Validate Token Signature
        try {
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token);
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 4. Proceed to the target microservice
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1; // Ensure this filter executes before routing
    }
}
