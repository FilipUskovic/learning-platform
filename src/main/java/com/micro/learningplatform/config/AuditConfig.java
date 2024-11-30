package com.micro.learningplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class AuditConfig {

    /* Klasa koja sluzi/ omogucje automatsko pracenje informacija o stvaranju i izmjeni entiteta
       -> Koristi automatizaciju pomocu createdBy i lastModifiedBy sto smo kreirali u Base modelu



     */

    @Bean
    public AuditorAware<String> auditorProvider() {
        //TODO: kada dodam security dohvacat korisnika
        return () -> Optional.of("system");
    }
}
