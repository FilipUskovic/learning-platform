package com.micro.learningplatform.security;

import com.micro.learningplatform.models.User;
import com.micro.learningplatform.repositories.UseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    //todo: drzati se ili hrv ili eng jezika

    /* Glvana zadace ove klase je :
        1. Identificirati "provider" (htihub i gogole=
        2. dohvatiti korisnikive attribute (name, sub, prviderId, orezime)
        3. spremiti u bazu
        4. atiti O2authUser-a

     */

    // korsitimo rest template samo u metodu fetchGithubEmail za ragovor s guthub-om
    private final RestTemplate restTemplate = new RestTemplate();
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
            log.debug("=== Početak OAuth2 autentikacije ===");
            log.debug("Provider: {}", userRequest.getClientRegistration().getRegistrationId());
            log.debug("Authorization grant type: {}", userRequest.getClientRegistration().getAuthorizationGrantType());
            log.debug("Redirect URI: {}", userRequest.getClientRegistration().getRedirectUri());

            // Loads the default OAuth2User using the built-in flow
            OAuth2User oauth2User = super.loadUser(userRequest);
            log.debug("OAuth2User uspješno učitan");
            log.debug("Dostupni atributi: {}", oauth2User.getAttributes().keySet());

            // Determine the provider (GITHUB, GOOGLE, etc.)
            String providerStr = userRequest.getClientRegistration().getRegistrationId();
            AuthProvider provider = AuthProvider.fromString(providerStr);

            log.debug("OAuth2 provider: {}", provider);
            log.debug("OAuth2 attributes: {}", oauth2User.getAttributes());

            // We must figure out how to get the email + providerId
            String email;
            String providerId;

            // todo ako budem dodavo vise providera jos kao npr linkedIn i facebook razmisliti o Map<AuthProvider, OAuth2ProviderHandler> umjesto if-else
            if (provider == AuthProvider.GITHUB) {
                email = oauth2User.getAttribute(EMAIL_ATTRIBUTE);
                if (email == null) {
                    // Fallback to the GitHub API call if "email" is not provided
                    email = fetchGithubEmail(userRequest);
                    log.debug("Fetched email from GitHub API: {}", email);
                }

                Integer githubId = oauth2User.getAttribute("id");
                if (githubId == null) {
                    log.error("GitHub ID nije pronađen u OAuth2 atributima");
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("missing_github_id"),
                            "GitHub ID nije dostupan"
                    );
                }
                providerId = githubId.toString();

                /* If we *still* have no email, create a fallback
                if (email == null) {
                    log.warn("Korisnik nema email - kreiram fallback email");
                    email = oauth2User.getAttribute("login") + "@github.local";
                }

                 */

            } else {
                // For Google or other providers that supply "email" & "sub"
                email = extractRequiredAttribute(oauth2User, EMAIL_ATTRIBUTE);
                providerId = extractRequiredAttribute(oauth2User, SUB_ATTRIBUTE);
            }

            log.debug("Processing OAuth2 login for user with email: {}, provider: {}", email, provider);

            // Attempt to find existing user by provider/providerId, or create a new one
            String finalEmail = email;
            return userRepository.findByProviderAndProviderId(provider, providerId)
                    .map(existingUser -> updateExistingUser(existingUser, oauth2User))
                    // Pass the resolved email directly to createNewOAuth2User
                    .orElseGet(() -> createNewOAuth2User(oauth2User, provider, providerId, finalEmail));

        } catch (Exception e) {
            log.error("OAuth2 authentication failed", e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("authentication_error"),
                    "Failed to process OAuth2 login: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * For GitHub, we may need to fetch the email from /user/emails
     * if the /user endpoint returns null for 'email'.
     */
    private String fetchGithubEmail(OAuth2UserRequest userRequest) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());

            HttpEntity<String> entity = new HttpEntity<>("", headers);
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> emails = response.getBody();
            if (emails != null && !emails.isEmpty()) {
                // Look for the 'primary' email
                Optional<String> primaryEmail = emails.stream()
                        .filter(emailObj -> Boolean.TRUE.equals(emailObj.get("primary")))
                        .map(emailObj -> (String) emailObj.get("email"))
                        .findFirst();

                if (primaryEmail.isPresent()) {
                    log.debug("Primary email found: {}", primaryEmail.get());
                    return primaryEmail.get();
                }

                // If no 'primary' found, fallback to the first available
                Optional<String> anyEmail = emails.stream()
                        .map(emailObj -> (String) emailObj.get("email"))
                        .findFirst();
                if (anyEmail.isPresent()) {
                    log.warn("No primary email found, using first available: {}", anyEmail.get());
                    return anyEmail.get();
                }
            }

            log.warn("No email found for GitHub user, using fallback email.");
            return "unknown@github.local";

        } catch (HttpClientErrorException ex) {
            log.error("Error fetching GitHub email: {}", ex.getMessage());
            return "unknown@github.local";
        }
    }

    /**
     * Creates a brand-new user from the OAuth2User attributes.
     * Instead of calling 'extractRequiredAttribute(oauth2User, "email")',
     * we *pass* the resolved email as a parameter.
     */
    private User createNewOAuth2User(
            OAuth2User oauth2User,
            AuthProvider provider,
            String providerId,
            String resolvedEmail  // <--- newly added parameter
    ) {
        // We already have the email from loadUser
      //  String email = resolvedEmail;

        // Attempt to retrieve firstName, lastName
        String firstName = oauth2User.getAttribute(GIVEN_NAME_ATTRIBUTE);
        String lastName = oauth2User.getAttribute(FAMILY_NAME_ATTRIBUTE);

        if (firstName == null || lastName == null) {
            // If 'given_name' or 'family_name' are missing, fall back to 'name'
            String fullName = oauth2User.getAttribute(NAME_ATTRIBUTE);
            if (fullName != null) {
                String[] names = fullName.split(" ");
                firstName = names[0];
                lastName = (names.length > 1) ? names[names.length - 1] : "";
            }
        }

        // Build a new user
        User newUser = User.createOAuth2User(
                resolvedEmail,
                firstName != null ? firstName : "Unknown",
                lastName != null ? lastName : "Unknown",
                provider,
                providerId
        );

        // Optionally store additional attributes
        newUser.setAttributes(oauth2User.getAttributes());

        log.info("Created new OAuth2 user with email: {}", resolvedEmail);
        return userRepository.save(newUser);
    }

    /**
     * Extracts a required attribute by name, throwing an exception if missing.
     */
    private String extractRequiredAttribute(OAuth2User oauth2User, String attributeName) {
        return Optional.ofNullable(oauth2User.getAttribute(attributeName))
                .map(String::valueOf)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("missing_attribute"),
                        "Required attribute " + attributeName + " is missing"
                ));
    }

    /**
     * Updates an existing user if attributes differ, otherwise returns the same user.
     */
    private User updateExistingUser(User existingUser, OAuth2User oauth2User) {
        if (!Objects.equals(existingUser.getAttributes(), oauth2User.getAttributes())) {
            existingUser.setAttributes(oauth2User.getAttributes());
            userRepository.save(existingUser);
            log.debug("Ažurirani OAuth2 atributi za korisnika: {}", existingUser.getEmail());
        }
        return existingUser;
    }

}


