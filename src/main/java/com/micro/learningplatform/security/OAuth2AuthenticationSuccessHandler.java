package com.micro.learningplatform.security;

import com.micro.learningplatform.models.User;
import com.micro.learningplatform.models.UserToken;
import com.micro.learningplatform.repositories.UserTokenRepository;
import com.micro.learningplatform.security.jwt.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserTokenRepository tokenRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        User user = (User) authentication.getPrincipal();

        // Koristimo custom claims iz JwtService-a
        String token = jwtService.generateToken(
                jwtService.generateCustomClaims(user),
                user
        );

        // Također generiramo refresh token
        String refreshToken = jwtService.generateRefreshToken(user);

        // Spremamo oba tokena
        saveUserTokens(user, token, refreshToken);

        // Šaljemo oba tokena frontendu
        String redirectUrl = UriComponentsBuilder.fromUriString("/oauth2/redirect")
                .queryParam("access_token", token)
                .queryParam("refresh_token", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private void saveUserTokens(User user, String accessToken, String refreshToken) {
        // Prvo opozovemo sve postojeće tokene
        tokenRepository.revokeAllUserTokens(user);

        var newAccessToken = UserToken.createAccessToken(
                user,
                accessToken,
                LocalDateTime.now().plusHours(24) // Fiksno vrijeme ili možemo dodati u konfiguraciju
        );

        // Za refresh token - pretpostavljamo 7 dana trajanje
        var newRefreshToken = UserToken.createRefreshToken(
                user,
                refreshToken,
                LocalDateTime.now().plusDays(7)
        );

        user.addToken(newAccessToken);
        user.addToken(newRefreshToken);
        tokenRepository.saveAll(Arrays.asList(newAccessToken, newRefreshToken));
    }
}