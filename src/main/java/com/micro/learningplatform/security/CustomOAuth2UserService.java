package com.micro.learningplatform.security;

import com.micro.learningplatform.models.User;
import com.micro.learningplatform.repositories.UseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {


    private final UseRepository userRepository;


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

}
