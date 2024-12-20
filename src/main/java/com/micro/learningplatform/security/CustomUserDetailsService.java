package com.micro.learningplatform.security;

import com.micro.learningplatform.repositories.UseRepository;
import com.micro.learningplatform.shared.exceptions.AccountStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UseRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Korisnik nije pronađen s email-om: " + email
                ));

        // Provjera statusa korisnika
        if (!user.isEnabled() || !user.isEmailVerified()) {
            throw new AccountStatusException("Korisnički račun nije aktiviran ili email nije verificiran.");
        }

        return user;
    }

    /*
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Korisnik nije pronađen s email-om: " + email
                ));
    }

     */
}
