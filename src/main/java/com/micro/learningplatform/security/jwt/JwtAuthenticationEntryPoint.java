package com.micro.learningplatform.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Unauthorized");
        // Generička poruka umjesto pune iznimke
        if (authException.getCause() instanceof SignatureException) {
            errorResponse.put("message", "Invalid token signature");
        } else if (authException.getCause() instanceof ExpiredJwtException) {
            errorResponse.put("message", "Token has expired");
        } else {
            errorResponse.put("message", "Invalid token");
        }


        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
