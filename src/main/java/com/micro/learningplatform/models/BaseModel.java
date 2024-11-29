package com.micro.learningplatform.models;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseModel {
    // Bazna klasa
    /* -> Mapped supperklass jer zelimo dijeliti audit polja medu entitetima
       -> ne zelimo stvarati zasebnu zablicu
       -> omogucava nam stvaranje jpa auditing-a (pracenje i promjena nad baza koje vrijeme)
    */

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

}
