package com.micro.learningplatform.security.controller;

import com.micro.learningplatform.repositories.UserTokenRepository;
import com.micro.learningplatform.security.service.AuthenticationServiceImpl;
import com.micro.learningplatform.security.dto.AuthenticationRequest;
import com.micro.learningplatform.security.dto.AuthenticationResponse;
import com.micro.learningplatform.security.dto.RegisterRequest;
import com.micro.learningplatform.security.dto.TokenRefreshRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
