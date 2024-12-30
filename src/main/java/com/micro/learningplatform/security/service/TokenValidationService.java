package com.micro.learningplatform.security.service;

import com.micro.learningplatform.repositories.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenValidationService {

    // centralizirano klasa za pravljanje tokenima razmisljam da ce u buducnosti rast jos nasa kompleknost i metoda pa bolje imati centralizirani pristup

    private final UserTokenRepository tokenRepository;

    /**
     * @return true ako je token pronađen i nije revokiran i nije istekao
     */
    public boolean isTokenActive(String validToken){
        return tokenRepository.findValidToken(validToken, LocalDateTime.now())
                .map(token -> !token.isRevoked())
                .orElse(false);
    }
}
