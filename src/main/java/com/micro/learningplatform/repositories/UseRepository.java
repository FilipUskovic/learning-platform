package com.micro.learningplatform.repositories;

import com.micro.learningplatform.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UseRepository extends JpaRepository<User, UUID> {

    // ovo nam je potrebno za userdetailsservice jer cemo preko nje dohvacati i provjeravat korsinik email je unique jel
    Optional<User> findByEmail(String email);

   // OAuth2 autentifikaciju
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByEmail(String email);

    // native query je bolji za rad s velikiim skupovima pogovo jer cemo korsiti indexke
    @Query(nativeQuery = true, value = "SELECT * FROM users WHERE enabled = true AND email_verified = true")
    List<User> findAllActiveAndVerified();

    @Query(value = "SELECT * FROM users WHERE attributes @> :jsonQuery",
            nativeQuery = true)
    List<User> findByAttributes(@Param("jsonQuery") String jsonQuery);

    


}
