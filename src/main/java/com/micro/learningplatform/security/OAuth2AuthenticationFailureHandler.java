package com.micro.learningplatform.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final ObjectMapper objectMapper;


    private static final Map<String, String> ERROR_DESCRIPTIONS = Map.of(
            "access_denied", "Pristup odbijen od strane korisnika",
            "invalid_request", "Neispravan zahtjev za autentifikaciju",
            "invalid_client", "Neispravna klijentska konfiguracija",
            "server_error", "Greška na serveru tijekom autentifikacije"
    );

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
        log.error("OAuth2 autentifikacija nije uspjela", exception);

        // Pripremamo detalje o grešci
        AuthenticationError error = prepareAuthenticationError(exception);

        // Pripremamo URL za redirect s informacijama o grešci
        String targetUrl = buildFailureRedirectUrl(error);

        // Čistimo security context
        SecurityContextHolder.clearContext();

        // Postavljamo sigurnosne header-e
        configureSecurityHeaders(response);

        // Izvršavamo preusmjeravanje
        getRedirectStrategy().sendRedirect(request, response, targetUrl);

        log.debug("Korisnik preusmjeren na error page: {}", targetUrl);
    }

    private AuthenticationError prepareAuthenticationError(AuthenticationException exception) {
        String errorCode = determineErrorCode(exception);
        String errorDescription = ERROR_DESCRIPTIONS.getOrDefault(errorCode,
                "Nepoznata greška tijekom autentifikacije");

        return new AuthenticationError(errorCode, errorDescription, exception.getMessage());
    }

    private String determineErrorCode(AuthenticationException exception) {
        // Određujemo specifični error kod based on exception type
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2Error oauth2Error = ((OAuth2AuthenticationException) exception).getError();
            return oauth2Error.getErrorCode();
        }
        return "server_error";
    }


    private String buildFailureRedirectUrl(AuthenticationError error) {
        try {
            String errorDetails = objectMapper.writeValueAsString(error);
            return UriComponentsBuilder.fromUriString("/login")
                    .queryParam("error", "oauth2_error")
                    .queryParam("details", URLEncoder.encode(errorDetails,
                            StandardCharsets.UTF_8))
                    .build().toUriString();
        } catch (JsonProcessingException e) {
            log.error("Greška pri serijalizaciji error detalja", e);
            return "/login?error=oauth2_error";
        }
    }


    private void configureSecurityHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }


    @Getter
    @AllArgsConstructor
    private static class AuthenticationError {
        private final String code;
        private final String description;
        private final String details;
    }

    /*
    private static final String DEFAULT_FAILURE_URL = "/login?error=oauth2_error";

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        // Možemo dodati specifične parametre za različite tipove grešaka
        String targetUrl = DEFAULT_FAILURE_URL +
                "&error_message=" + exception.getLocalizedMessage();

        logger.error("OAuth2 authentication failed: " + exception.getMessage());

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

     */


}
