package com.micro.learningplatform.security.controller;

import com.micro.learningplatform.repositories.UserTokenRepository;
import com.micro.learningplatform.security.service.AuthenticationServiceImpl;
import com.micro.learningplatform.security.dto.AuthenticationRequest;
import com.micro.learningplatform.security.dto.AuthenticationResponse;
import com.micro.learningplatform.security.dto.RegisterRequest;
import com.micro.learningplatform.security.dto.TokenRefreshRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationServiceImpl authService;
    private final UserTokenRepository tokenRepository;


    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @PostMapping("/register-without-token")
    public ResponseEntity<String> registerWithoutToken(@RequestBody @Valid RegisterRequest request) {
        String message = authService.registerWithoutdToken(request);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/register-instructor")
    public ResponseEntity<AuthenticationResponse> registerInstructor(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.registerInstructor(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) {
        log.info("Pokušaj autentifikacije za korisnika: {}", request.email());
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestBody @Valid TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            var storedToken = tokenRepository.findByToken(jwt)
                    .orElse(null);
            if (storedToken != null) {
                storedToken.setRevoked(true);
                tokenRepository.save(storedToken);
                SecurityContextHolder.clearContext();
            }
        }
        return ResponseEntity.ok().build();
    }


    // todo ovo bi sigurno malknuli iz javnih pristupa samo za test
    @PostMapping("/register-admin")
    public ResponseEntity<AuthenticationResponse> registerAdmin(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.registerAdmin(registerRequest));
    }


    // o2auth kontrolleri

    @GetMapping("/oauth2/callback/success")
    public ResponseEntity<AuthenticationResponse> handleOAuth2Success(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletRequest request) {

        log.debug("Primljen OAuth2 success callback za korisnika");

        OAuth2AuthorizationRequest authorizationRequest = getStoredAuthorizationRequest(request);
        if (authorizationRequest == null) {
            log.error("Nije pronađen authorization request u sesiji");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthenticationResponse());
        }

        // Kreiraj OAuth2UserRequest koristeći tvoju postojeću metodu
        OAuth2UserRequest userRequest = createOAuth2UserRequest(authorizationRequest, request, oauth2User);

        // Pozovi authService s ispravnim parametrima
        AuthenticationResponse response = authService.authenticateOAuth2User(userRequest, oauth2User);

        log.info("Uspješna OAuth2 autentifikacija za korisnika: {}",
                Optional.ofNullable(oauth2User.getAttribute("email")).orElse("N/A"));

        return ResponseEntity.ok(response);
    }

    private OAuth2UserRequest createOAuth2UserRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, OAuth2User oauth2User) {
        ClientRegistration clientRegistration = (ClientRegistration) request
                .getSession()
                .getAttribute("client_registration");

        if (clientRegistration == null) {
            throw new OAuth2AuthenticationException("Client registration not found in session.");
        }
        // Kreiraj i vrati OAuth2UserRequest
        return new OAuth2UserRequest(clientRegistration, (OAuth2AccessToken) oauth2User.getAttributes());
    }


    @GetMapping("/user")
    public ResponseEntity<?> getUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("message", "Niste prijavljeni"));
        }

        return ResponseEntity.ok(Map.of(
                "name", Objects.requireNonNull(principal.getAttribute("name")),
                "email", Objects.requireNonNull(principal.getAttribute("email")),
                "picture", Objects.requireNonNull(principal.getAttribute("picture"))
        ));
    }

    private OAuth2AuthorizationRequest getStoredAuthorizationRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (OAuth2AuthorizationRequest) session.getAttribute("oauth2_auth_request");
    }






}
