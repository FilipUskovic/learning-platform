package com.micro.learningplatform.models;

import com.micro.learningplatform.security.TokenType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cache;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_tokens",
        indexes = {
                @Index(name = "idx_user_token", columnList = "token"),
                @Index(name = "idx_user_token_user_type", columnList = "user_id, tokenType")
        }
)
@Getter
@Setter
@NoArgsConstructor
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class UserToken {

    /**
     * Entitet za upravljanje tokenima
     * Podržava i JWT i OAuth2 tokene
     */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private User user;

    @Column(unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    private boolean revoked;

    private LocalDateTime expiryDate;

    // Factory metode
    public static UserToken createAccessToken(User user, String token, LocalDateTime expiryDate) {
        UserToken userToken = new UserToken();
        userToken.setUser(user);
        userToken.setToken(token);
        userToken.setTokenType(TokenType.ACCESS);
        userToken.setExpiryDate(expiryDate);
        return userToken;
    }

    public static UserToken createRefreshToken(User user, String token, LocalDateTime expiryDate) {
        UserToken userToken = new UserToken();
        userToken.setUser(user);
        userToken.setToken(token);
        userToken.setTokenType(TokenType.REFRESH);
        userToken.setExpiryDate(expiryDate);
        return userToken;
    }

   // @Cacheable(value = "tokenValidity", key = "#token")
    public boolean isValid() {
        return !revoked && LocalDateTime.now().isBefore(expiryDate);
    }
}
