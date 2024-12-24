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
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

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
     * Trenutne samo register metode odmah vracaju u accesstoken nije potrebni verificira iako je moguce
     * trenutno je samo jwt sve bez o2auth-a
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


    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("Pokušaj autentifikacije za korisnika: {}", request.email());

        // Provjera postoji li korisnik prije autentifikacije to jeb bitno korak bio
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + request.email()));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );


        log.info("Autentifikacija uspješna za korisnika: {}", request.email());

        return generateAuthenticationResponse(user);
    }

    /*


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

    // TODO ova metoda nije gotova dodati emailService koji ce slati email verifikaciju za tokena putem jwt-a
    @Override
    @Transactional
    public String registerWithoutdToken(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        var user = User.createLocalUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName()
        );

        user.setEnabled(true);
        user.setEmailVerified(false);  // Email verifikacija po potrebi

        userRepository.save(user);

        log.info("User registered with email: {}", request.email());
        return "User registered successfully. Please login to continue.";
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


    // O2AUTH METODE



    @Override
    @Transactional
    public AuthenticationResponse authenticateOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {

        log.debug("Započinjem OAuth2 autentifikaciju za providera: {}",
                userRequest.getClientRegistration().getRegistrationId());

        // Izvlačimo osnovne informacije iz OAuth2 odgovora
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oauth2User.getAttribute("sub"); // Jedinstveni ID od providera
        String email = oauth2User.getAttribute("email");

        User user = findOrCreateOAuth2User(oauth2User, provider, providerId);

        updateOAuth2UserAttributes(user, oauth2User);

        log.info("OAuth2 autentifikacija uspješna za korisnika: {}", user.getEmail());

        return generateAuthenticationResponse(user);
    }

    private User findOrCreateOAuth2User(OAuth2User oauth2User, String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(AuthProvider.valueOf(provider), providerId)
                .orElseGet(() -> {
                    // ako ne postoji, trazimo po email-u
                    String email = oauth2User.getAttribute("email");
                    return userRepository.findByEmail(email)
                            .map(existingUser -> {
                                // Ako korisnik postoji s emailom ali drugim providerom,
                                // moramo odlučiti kako postupiti
                                if (!provider.equals(existingUser.getProvider().name())) {
                                    log.warn("Postojeći korisnik {} pokušava se povezati s novim providerom {}",
                                            email, provider);
                                    throw new OAuth2AuthenticationException(
                                            new OAuth2Error("account_exists"),
                                            "Account already exists with different provider"
                                    );
                                }
                                return existingUser;
                            })
                            .orElseGet(() -> createNewOAuth2User(oauth2User, provider, providerId));
                });
    }

    private User createNewOAuth2User(OAuth2User oauth2User, String provider, String providerId) {
        String email = oauth2User.getAttribute("email");
        String firstName = extractFirstName(oauth2User);
        String lastName = extractLastName(oauth2User);

        return User.createOAuth2User(
                email,
                firstName,
                lastName,
                AuthProvider.valueOf(provider.toUpperCase()),
                providerId
        );
    }

    private String extractFirstName(OAuth2User oauth2User) {
        String firstName = oauth2User.getAttribute("given_name");
        if (firstName == null) {
            String fullName = oauth2User.getAttribute("name");
            if (fullName != null) {
                return fullName.split(" ")[0];
            }
        }
        return firstName != null ? firstName : "Unknown";
    }

    private String extractLastName(OAuth2User oauth2User) {
        String lastName = oauth2User.getAttribute("family_name");
        if (lastName == null) {
            String fullName = oauth2User.getAttribute("name");
            if (fullName != null) {
                String[] names = fullName.split(" ");
                return names.length > 1 ? names[names.length - 1] : "";
            }
        }
        return lastName != null ? lastName : "Unknown";
    }

    private void updateOAuth2UserAttributes(User user, OAuth2User oauth2User) {
        // Ažuriramo atribute samo ako su se promijenili
        if (!Objects.equals(user.getAttributes(), oauth2User.getAttributes())) {
            user.setAttributes(oauth2User.getAttributes());
            userRepository.save(user);
            log.debug("Ažurirani OAuth2 atributi za korisnika: {}", user.getEmail());
        }
    }


}


// todo ova ce biti metoda koja ce ferificirati korsnika putem maila kada dodam mail service
/*
    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.debug("Započinjem autentifikaciju za korisnika: {}", request.email());

        AuthenticationResult validationResult =  validateUser(request.email());
        if(!validationResult.isSuccess()){
            log.warn("Validacija korisnika neuspješna: {}", validationResult.message());
            throw new AuthenticationFailedException(validationResult.message());
        }

        if (validationResult.requiresVerification()) {
            log.warn("Korisnik {} zahtijeva dodatnu verifikaciju", request.email());
            throw new AccountNotVerifiedException("Potrebna je verifikacija email adrese");
        }

        // Provjera kredencijala
        AuthenticationResult credentialsResult = validateCredentials(request);
        if (!credentialsResult.isSuccess()) {
            log.warn("Validacija kredencijala neuspješna: {}", credentialsResult.message());
            throw new AuthenticationFailedException(credentialsResult.message());
        }

        log.info("Autentifikacija uspješna za korisnika: {}", request.email());
        return generateAuthenticationResponse(validationResult.user());


    }

    private AuthenticationResult validateCredentials(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            return new AuthenticationResult(null, "Kredencijali ispravni", true, false);
        } catch (BadCredentialsException e) {
            return new AuthenticationResult(null, "Neispravni kredencijali", false, true);
        }
    }



    private AuthenticationResult validateUser(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if(!user.isEnabled()) {
                        return new AuthenticationResult(null, "Korisnički račun nije aktivan", false, false);
                    }
                    if (!user.isEmailVerified()) {
                        return new AuthenticationResult(user, "Email nije verificiran", false, true);
                    }
                    return new AuthenticationResult(user, "Validacija uspješna", true, false);
                })
                .orElseGet(() -> new AuthenticationResult(null, "Neispravni kredencijali", false, false));
    }

 */