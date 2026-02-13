package com.btctech.mailapp.config;

import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final UserService userService;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // Skip filter for public endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/register") || 
            path.startsWith("/api/auth/login") ||
            path.equals("/error")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String jwt = authHeader.substring(7);
            String jwtSubject = jwtUtil.extractEmail(jwt);
            
            log.debug("Processing JWT for subject: {}", jwtSubject);
            
            if (jwtSubject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                User user = null;
                String principal = null;
                
                if (jwtSubject.startsWith("temp_")) {
                    // Temporary token
                    String username = jwtSubject.substring(5);
                    user = userService.getUserByUsername(username);
                    principal = username;
                    
                    log.debug("Temp token - username: {}", username);
                    
                } else if (jwtSubject.contains("@")) {
                    // Regular token (email)
                    user = userService.getUserByEmail(jwtSubject);
                    principal = jwtSubject;
                    
                    log.debug("Regular token - email: {}", jwtSubject);
                }
                
                if (user != null && jwtUtil.validateToken(jwt, jwtSubject)) {
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            principal,  // Email or username
                            null,
                            new ArrayList<>()
                        );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("✓ Authentication successful for: {}", principal);
                } else {
                    log.warn("✗ Token validation failed for: {}", jwtSubject);
                }
            }
            
        } catch (JwtException e) {
            log.error("JWT validation error: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage(), e);
        }
        
        filterChain.doFilter(request, response);
    }
}