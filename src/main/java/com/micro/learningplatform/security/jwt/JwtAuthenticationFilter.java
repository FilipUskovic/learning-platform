package com.micro.learningplatform.security.jwt;

import com.micro.learningplatform.repositories.UserTokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserTokenRepository tokenRepository;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // Thread pool za paralelno procesiranje tokena
    private final ExecutorService tokenValidationExecutor =
            Executors.newVirtualThreadPerTaskExecutor();




    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        boolean shouldNotFilter =
               // path.equals("/api/v1/auth/login") ||
                path.equals("/api/v1/auth/register") ||
                path.equals("/api/v1/auth/refresh-token") ||
                path.startsWith("/oauth2/") ||
                path.startsWith("/login/oauth2/") ||
              //  path.startsWith("/api/v1/auth/oauth2/") ||
                path.contains("/swagger-ui") ||
                path.contains("/v3/api-docs");

        log.debug("Ruta '{}' je {} za JWT filter.", path, shouldNotFilter ? "javno dostupna" : "zaštićena");
        return shouldNotFilter;
    }


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.debug("Primljen zahtjev na putanji: {}", request.getServletPath());


        final String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Nema valjanog Authorization headera, preskačem autentifikaciju.");
            filterChain.doFilter(request, response);  // Prosljeđuje javne rute bez autentifikacije
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            // Koristimo CompletableFuture za asinkronu validaciju
            CompletableFuture<String> userEmailFuture = CompletableFuture
                    .supplyAsync(() -> jwtService.extractUsername(jwt), tokenValidationExecutor);

            CompletableFuture<Boolean> tokenValidityFuture = CompletableFuture
                    .supplyAsync(() -> tokenRepository.findValidToken(jwt, LocalDateTime.now())
                                    .map(token -> !token.isRevoked())
                                    .orElse(false),
                            tokenValidationExecutor);

            // Čekamo rezultate validacije
            String userEmail = userEmailFuture.get(500, TimeUnit.MILLISECONDS);
            boolean isValidToken = tokenValidityFuture.get(500, TimeUnit.MILLISECONDS);

            if (userEmail != null && isValidToken) {
                processValidToken(userEmail, request);
            }else {
                throw new BadCredentialsException("Invalid or revoked token");
            }

            filterChain.doFilter(request, response);
        } catch (SignatureException | ExpiredJwtException e) {
            jwtAuthenticationEntryPoint.commence(request, response, new BadCredentialsException(e.getMessage()));
        } catch (Exception e) {
            jwtAuthenticationEntryPoint.commence(request, response, new AuthenticationException(e.getMessage()) {});
        }
    }

    private void processValidToken(String userEmail, HttpServletRequest request) {
        var userDetails = userDetailsService.loadUserByUsername(userEmail);
        var authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }



    /*
     * Filter koji presreće svaki HTTP zahtjev i provjerava JWT token
     * Radi zajedno s OAuth2 autentifikacijom tako da ne ometa OAuth2 endpoints
     */

    /*
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.debug("Primljen zahtjev na putanji: {}", request.getServletPath());


        final String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Nema valjanog Authorization headera, preskačem autentifikaciju.");
            filterChain.doFilter(request, response);  // Prosljeđuje javne rute bez autentifikacije
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
                }else {
                    log.warn("Token je nevaljan ili opozvan.");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                    response.getWriter().write("Invalid or revoked token");
                    return;
                }
            }
        } catch (JwtException e) {
            log.error("Nevaljan JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            response.getWriter().write("Invalid JWT token");
            return;
        }

        filterChain.doFilter(request, response);
    }

     */


    private boolean isTokenValid(String jwt, UserDetails userDetails) {
        return tokenRepository.findValidToken(jwt, LocalDateTime.now())
                .map(token -> !token.isRevoked() && jwtService.isTokenValid(jwt, userDetails))
                .orElse(false);
    }

}