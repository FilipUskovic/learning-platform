package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.User;
import com.micro.learningplatform.models.UserToken;
import com.micro.learningplatform.security.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, UUID> {

    @Query("SELECT t FROM UserToken t WHERE t.token = :token AND t.revoked = false AND t.expiryDate > :now")
    Optional<UserToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    List<UserToken> findAllByUser(User user);

    Optional<UserToken> findByToken(String token);

    List<UserToken> findAllByUserAndTokenType(User user, TokenType tokenType);

    @Modifying
    @Query("UPDATE UserToken t SET t.revoked = true WHERE t.user = :user")
    void revokeAllUserTokens(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM UserToken t WHERE t.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

}
