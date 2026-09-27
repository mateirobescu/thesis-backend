package com.mateirobescu.thesis.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        System.out.println(header);

        if(header != null && header.startsWith("Bearer ")) {
            log.info("Authorization header received: {}", header);

            String token = header.substring(7);

            try {
                UUID userId = jwtService.validateAndExtractUserId(token);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userId.toString(), null, Collections.emptyList()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                log.warn("Token validation failed", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
