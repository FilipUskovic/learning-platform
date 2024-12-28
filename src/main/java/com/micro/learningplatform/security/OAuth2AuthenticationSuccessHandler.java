package com.micro.learningplatform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.micro.learningplatform.models.User;
import com.micro.learningplatform.models.UserToken;
import com.micro.learningplatform.repositories.UseRepository;
import com.micro.learningplatform.repositories.UserTokenRepository;
import com.micro.learningplatform.security.dto.AuthenticationResponse;
import com.micro.learningplatform.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserTokenRepository tokenRepository;
    private final UseRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.debug("OAuth2 authentication success handler pokrenut");

        try {
            // 1) The 'principal' should actually be your domain User
            if (authentication.getPrincipal() instanceof User domainUser) {
                // 2) Directly grab the real email from your domain User
                String email = domainUser.getEmail();
                log.debug("Korisnik se upravo prijavio s emailom: {}", email);

                // 3) Optionally re-fetch from DB, if needed
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

                log.debug("Pronađen korisnik u bazi: {}", user.getEmail());

                // 4) Continue your existing logic: create JWT tokens, etc.
                AuthenticationResponse authResponse = createAuthenticationResponse(user);
                String redirectUrl = prepareRedirectUrl(authResponse);

                configureSecurityHeaders(response);
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);

            } else {
                // If the principal isn't your domain User, throw an error
                log.error("Neočekivani tip principala: {}",
                        authentication.getPrincipal().getClass().getName());
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_principal"),
                        "Unexpected principal type"
                );
            }
        } catch (Exception e) {
            log.error("Greška tijekom OAuth2 success handlera", e);
            handleAuthenticationError(response, e);
        }
    }


    private AuthenticationResponse createAuthenticationResponse(User user) {
        // Generiramo access i refresh tokene s dodatnim podacima o korisniku
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("provider", user.getProvider().name());
        customClaims.put("roles", user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toList()));

        String accessToken = jwtService.generateToken(customClaims, user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Spremamo nove tokene u bazu
        saveUserTokens(user, accessToken, refreshToken);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }


    private void saveUserTokens(User user, String accessToken, String refreshToken) {
        // Prvo opozivamo sve postojeće tokene
        tokenRepository.revokeAllUserTokens(user);

        LocalDateTime now = LocalDateTime.now();

        var newAccessToken = UserToken.createAccessToken(
                user,
                accessToken,
                jwtService.getAccessTokenExpiration(now)
        );

        var newRefreshToken = UserToken.createRefreshToken(
                user,
                refreshToken,
                jwtService.getRefreshTokenExpiration(now)
        );

        tokenRepository.saveAll(Arrays.asList(newAccessToken, newRefreshToken));
        log.debug("Spremljeni novi tokeni za korisnika: {}", user.getEmail());
    }

    private String prepareRedirectUrl(AuthenticationResponse authResponse) {
        return UriComponentsBuilder.fromUriString("/oauth2/redirect")
                .queryParam("access_token", authResponse.getAccessToken())
                .queryParam("refresh_token", authResponse.getRefreshToken())
                .queryParam("token_type", authResponse.getTokenType())
                .build().toUriString();
    }

    private void configureSecurityHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    private void handleAuthenticationError(HttpServletResponse response, Exception e)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Autentifikacija nije uspjela");
        errorResponse.put("message", e.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

}