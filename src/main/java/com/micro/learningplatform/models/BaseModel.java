package com.micro.learningplatform.models;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@MappedSuperclass // ne stvara vlastitu tablicu u bazi i ne moze se koristi ko entitet
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
    @Column(name = "version", nullable = false)
    private Long version;

    // dodajem nove fieldove
    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "difficulty_level", length = 50)
    private String difficultyLevel;

    @Column(name = "estimated_duration")
    private Duration estimatedDuration;

    @Column(name = "max_students")
    private Integer maxStudents = 100;

    protected BaseModel() {}


}
