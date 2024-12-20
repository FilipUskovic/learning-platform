package com.micro.learningplatform.security.jwt;

import com.micro.learningplatform.repositories.UserTokenRepository;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserTokenRepository tokenRepository;



    /*
     * Filter koji presreće svaki HTTP zahtjev i provjerava JWT token
     * Radi zajedno s OAuth2 autentifikacijom tako da ne ometa OAuth2 endpoints
     */

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.debug("Primljen zahtjev na putanji: {}", request.getServletPath());

        // Provjera javnih ruta
        if (isPublicPath(request.getServletPath())) {
            log.debug("Javna ruta preskočena: {}", request.getServletPath());

            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Nema valjanog Authorization headera, preskačem autentifikaciju.");

            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String userEmail = jwtService.extractUsername(jwt);
            log.debug("Korisnički email iz tokena: {}", userEmail);


            // Ako korisnik nije autentificiran, provjeravamo token i korisnika
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (isTokenValid(jwt, userDetails)) {
                    log.debug("Token je valjan. Postavljam korisnika u SecurityContext.");

                    // Ako je token validan, postavi korisnika u SecurityContext
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException e) {
            log.error("Nevaljan JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }




    private boolean isPublicPath(String path) {
        log.debug("Provjera javne rute: {}", path);

        return path.equals("/api/v1/auth/register") ||
                path.equals("/api/v1/auth/login") ||
                path.contains("/oauth2") ||
                path.contains("/error") ||
                path.contains("/swagger-ui") ||
                path.contains("/v3/api-docs");
    }

    private boolean isTokenValid(String jwt, UserDetails userDetails) {
        return tokenRepository.findValidToken(jwt, LocalDateTime.now())
                .map(token -> !token.isRevoked() && jwtService.isTokenValid(jwt, userDetails))
                .orElse(false);
    }

/*
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (isPublicPath(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(userEmail);

                var isTokenValid = tokenRepository.findValidToken(jwt, LocalDateTime.now())
                        .map(token -> {
                            //provjera expiry date-a kroz JwtService
                            return !token.isRevoked() && jwtService.isTokenValid(jwt, userDetails);
                        })
                        .orElse(false);

                if (isTokenValid) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException e) {
            // Bolje rukovanje s JWT exceptions
            log.error("Nevaljan JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }

 */

    /*
    private boolean isPublicPath(String path) {
        return path.contains("/api/v1/auth") ||
                path.contains("/oauth2") ||
                path.contains("/error") ||
                path.contains("/swagger-ui") ||
                path.contains("/v3/api-docs");
    }

     */

}
