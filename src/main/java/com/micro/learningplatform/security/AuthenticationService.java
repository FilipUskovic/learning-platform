package com.micro.learningplatform.security;

import com.micro.learningplatform.models.User;
import com.micro.learningplatform.models.UserToken;
import com.micro.learningplatform.repositories.UseRepository;
import com.micro.learningplatform.repositories.UserTokenRepository;
import com.micro.learningplatform.security.dto.AuthenticationRequest;
import com.micro.learningplatform.security.dto.AuthenticationResponse;
import com.micro.learningplatform.security.dto.RegisterRequest;
import com.micro.learningplatform.security.jwt.JwtService;
import com.micro.learningplatform.shared.exceptions.InvalidTokenException;
import com.micro.learningplatform.shared.exceptions.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UseRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserTokenRepository tokenRepository;

    /**
     * Registrira novog korisnika u sustav.
     * Kreira korisnika, generira tokene i sprema ih u bazu.
     */
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        var user = User.createLocalUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName()
        );

        var savedUser = userRepository.save(user);

        var accessToken = jwtService.generateToken(savedUser);
        var refreshToken = jwtService.generateRefreshToken(savedUser);


        saveUserTokens(savedUser, accessToken, refreshToken);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();

    }

    /**
     * Autentificira postojećeg korisnika.
     * Provjerava kredencijale, generira nove tokene i sprema ih.
     */
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Autentifikacija korisnika kroz Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        saveUserTokens(user, accessToken, refreshToken);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    /**
     * Osvježava access token koristeći refresh token.
     * Provjerava valjanost refresh tokena i generira novi access token.
     */
    @Transactional
    public AuthenticationResponse refreshToken(String refreshToken) {
        final String userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail == null) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var storedToken = tokenRepository.findValidToken(refreshToken, LocalDateTime.now())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found or expired"));

        if (!jwtService.isTokenValid(refreshToken, user) || storedToken.isRevoked()) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        var newAccessToken = jwtService.generateToken(user);

        saveNewAccessToken(user, newAccessToken);

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Vraćamo isti refresh token
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

        user.addToken(newAccessToken);
        user.addToken(newRefreshToken);
        tokenRepository.saveAll(Arrays.asList(newAccessToken, newRefreshToken));
    }


    private void saveNewAccessToken(User user, String accessToken) {

        LocalDateTime now = LocalDateTime.now();

        var newAccessToken = UserToken.createAccessToken(
                user,
                accessToken,
                jwtService.getAccessTokenExpiration(now)
        );

        user.addToken(newAccessToken);
        tokenRepository.save(newAccessToken);
    }

}
