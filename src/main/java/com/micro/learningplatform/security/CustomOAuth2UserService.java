package com.micro.learningplatform.security;

import com.micro.learningplatform.models.User;
import com.micro.learningplatform.repositories.UseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {


    private final UseRepository userRepository;

    private static final String EMAIL_ATTRIBUTE = "email";
    private static final String SUB_ATTRIBUTE = "sub";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String GIVEN_NAME_ATTRIBUTE = "given_name";
    private static final String FAMILY_NAME_ATTRIBUTE = "family_name";


    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            // Prvo dohvaćamo osnovne OAuth2 podatke od providera kroz parent klasu
            OAuth2User oauth2User = super.loadUser(userRequest);

            // Izvlačimo informacije o provideru
            String provider = userRequest.getClientRegistration().getRegistrationId();

            // Dohvaćamo esencijalne podatke uz validaciju
            String email = extractRequiredAttribute(oauth2User, EMAIL_ATTRIBUTE);
            String providerId = extractRequiredAttribute(oauth2User, SUB_ATTRIBUTE);

            log.debug("Processing OAuth2 login for user with email: {}, provider: {}", email, provider);

            // Tražimo postojećeg korisnika ili kreiramo novog
            return (OAuth2User) userRepository.findByProviderAndProviderId(provider, providerId)
                    .map(existingUser -> updateExistingUser(existingUser, oauth2User))
                    .orElseGet(() -> createNewOAuth2User(oauth2User, provider, providerId));

        } catch (Exception e) {
            log.error("OAuth2 authentication failed", e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("authentication_error"),
                    "Failed to process OAuth2 login", e);
        }
    }

    private User createNewOAuth2User(OAuth2User oauth2User, String provider, String providerId) {
        String email = extractRequiredAttribute(oauth2User, EMAIL_ATTRIBUTE);

        if (userRepository.existsByEmail(email)) {
            log.error("User with email {} already exists with different provider", email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_exists"),
                    "Email already exists with different provider"
            );
        }

        String firstName = oauth2User.getAttribute(GIVEN_NAME_ATTRIBUTE);
        String lastName = oauth2User.getAttribute(FAMILY_NAME_ATTRIBUTE);

        // Ako nemamo ime i prezime, pokušavamo izvući iz full name atributa
        if (firstName == null || lastName == null) {
            String fullName = oauth2User.getAttribute(NAME_ATTRIBUTE);
            if (fullName != null) {
                String[] names = fullName.split(" ");
                firstName = names[0];
                lastName = names.length > 1 ? names[names.length - 1] : "";
            }
        }

        User newUser = User.createOAuth2User(
                email,
                firstName != null ? firstName : "Unknown",
                lastName != null ? lastName : "Unknown",
                AuthProvider.valueOf(provider.toUpperCase()),
                providerId
        );

        newUser.setAttributes(oauth2User.getAttributes());
        log.info("Created new OAuth2 user with email: {}", email);
        return userRepository.save(newUser);
    }


    private Object updateExistingUser(User existingUser, OAuth2User oauth2User) {
        // Ažuriramo samo ako su se podaci promijenili
        if(!shouldUpdateUserData(existingUser, oauth2User)) {
            existingUser.setAttributes(oauth2User.getAttributes());
            log.debug("Updated OAuth2 user attributes for user: {}", existingUser.getEmail());
        }
        return userRepository.save(existingUser);
    }

    private boolean shouldUpdateUserData(User user, OAuth2User oauth2User) {
        Map<String, Object> currentAttributes = user.getAttributes();
        Map<String, Object> newAttributes = oauth2User.getAttributes();
        return !Objects.equals(currentAttributes, newAttributes);
    }

    private String extractRequiredAttribute(OAuth2User oauth2User, String attributeName) {
        return Optional.ofNullable(oauth2User.getAttribute(attributeName))
                .map(String::valueOf)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_user_info"),
                        String.format("Required attribute %s is missing", attributeName)));
    }




  

/*
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Izvlačimo podatke iz OAuth2 odgovora
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oauth2User.getAttribute("sub"); // ili "id" za neke providere
        String email = oauth2User.getAttribute("email");
        // za svaki slucaj imam fallback metode
        String firstName = (String) Optional.ofNullable(oauth2User.getAttribute("given_name")).orElse("Unknown");
        String lastName = (String) Optional.ofNullable(oauth2User.getAttribute("family_name")).orElse("Unknown");



        // Tražimo postojećeg korisnika ili kreiramo novog
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    // Provjera postoji li korisnik s istim emailom
                    if (userRepository.existsByEmail(email)) {
                        throw new OAuth2AuthenticationException("Email already exists with different provider");
                    }

                    // Kreiramo novog korisnika
                    return User.createOAuth2User(
                            email,
                            firstName,
                            lastName,
                            AuthProvider.valueOf(provider.toUpperCase()),
                            providerId
                    );
                });

        user.setAttributes(oauth2User.getAttributes());
        return userRepository.save(user);
    }

 */

}


