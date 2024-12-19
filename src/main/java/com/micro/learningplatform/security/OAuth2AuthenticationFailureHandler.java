package com.micro.learningplatform.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

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
}
