package com.micro.learningplatform.security.service;

import com.micro.learningplatform.models.User;
import com.micro.learningplatform.models.UserToken;
import com.micro.learningplatform.repositories.UseRepository;
import com.micro.learningplatform.repositories.UserTokenRepository;
import com.micro.learningplatform.security.AuthProvider;
import com.micro.learningplatform.security.UserRole;
import com.micro.learningplatform.security.dto.*;
import com.micro.learningplatform.security.jwt.JwtService;
import com.micro.learningplatform.shared.exceptions.InvalidTokenException;
import com.micro.learningplatform.shared.exceptions.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger log = LogManager.getLogger(AuthenticationServiceImpl.class);
    private final UseRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserTokenRepository tokenRepository;

    //TOdo dodati bolje respone odgovore za metdoe, malo bolje validaciju pogotov za admiin metode

    /**
     * Registrira novog korisnika u sustav.
     * Kreira korisnika, generira tokene i sprema ih u bazu.
     */

    @Override
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
        // Automatski omogućiti i verificirati korisnika (po potrebi)
        user.setEnabled(true);
        user.setEmailVerified(true);

        var savedUser = userRepository.save(user);

        return generateAuthenticationResponse(savedUser);
    }

    // todo rijesit problem s tokenom validacjom
    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("Pokušaj autentifikacije za korisnika: {}", request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        log.info("Autentifikacija uspješna za korisnika: {}", request.email());

        return generateAuthenticationResponse(user);
    }



    /**
     * Osvježava access token koristeći refresh token.
     * Provjerava valjanost refresh tokena i generira novi access token.
     */
    //todo testirari ovo
    @Override
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

       return generateAuthenticationResponse(user);
    }

    @Override
    @Transactional
    public void addRoleToUser(String email, UserRole role) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if(!user.getRoles().contains(role)) {
            user.getRoles().add(role);
            userRepository.save(user);
        }

    }

    @Override
    @Transactional
    public void removeRoleFromUser(String email, UserRole role) {

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if(user.getRoles().contains(role)) {
            user.getRoles().remove(role);
            userRepository.save(user);
        }

    }

    @Override
    @Transactional
    public AuthenticationResponseWithRoles registerUserWithRoles(RegisterWithRolesRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        var user = User.createUserWithRoles(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                AuthProvider.LOCAL,
                request.roles().toArray(new UserRole[0])
        );

        user.setEnabled(true);
        user.setEmailVerified(true);
        var savedUser = userRepository.save(user);
        return generateAuthenticationResponseWithRoles(savedUser);
    }

    @Transactional
    @Override
    public AuthenticationResponse registerInstructor(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Instructor with email " + request.email() + " already exists");
        }
        var user = User.createUserWithRoles(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                AuthProvider.LOCAL,
                UserRole.INSTRUCTOR
        );
        user.setEnabled(true);
        user.setEmailVerified(true);
        var savedUser = userRepository.save(user);
        return generateAuthenticationResponse(savedUser);
    }

    // ovo je samo za test ne bi imali javni pristup svim za kreiranje admina
    @Transactional
    @Override
    public AuthenticationResponse registerAdmin(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Admin with email " + request.email() + " already exists");
        }

        var user = User.createUserWithRoles(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                AuthProvider.LOCAL,
                UserRole.ADMIN
        );
        user.setEnabled(true);
        user.setEmailVerified(true);
        var savedUser = userRepository.save(user);
        return generateAuthenticationResponse(savedUser);
    }


    private AuthenticationResponseWithRoles generateAuthenticationResponseWithRoles(User user) {
        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        saveUserTokens(user, accessToken, refreshToken);

        return AuthenticationResponseWithRoles.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .roles(user.getRoles().toString())
                .build();
    }



    private AuthenticationResponse generateAuthenticationResponse(User user) {
        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

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

        user.addToken(newAccessToken);
        user.addToken(newRefreshToken);
        tokenRepository.saveAll(Arrays.asList(newAccessToken, newRefreshToken));
    }


}
