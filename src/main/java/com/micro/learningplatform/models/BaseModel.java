package com.micro.learningplatform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.micro.learningplatform.event.DomainEvent;
import com.micro.learningplatform.models.dto.DifficultyLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@AllArgsConstructor
// cilj je sprijeciti d avanjske klase izravno pristupaju poljima i forsirati da nasljene klase upravljaju preko kontorlnih metoda
@Getter
@Setter
@MappedSuperclass // ne stvara vlastitu tablicu u bazi i ne moze se koristi ko entitet -> aka nadklasa ja jpa entietet
@EntityListeners(AuditingEntityListener.class) // korsiti se za slusanje događaja na entiteu, automatksi popujava poblja cratedDate itd..
@ToString
public abstract class BaseModel {
    private static final Logger log = LogManager.getLogger(BaseModel.class);

    /* -> Mapped supperklass jer zelimo dijeliti audit polja medu entitetima
       -> ne zelimo stvarati zasebnu zablicu
       -> omogucava nam stvaranje jpa auditing-a (pracenje i promjena nad baza koje vrijeme)
         1. Integracija s event sustavom
            -> događaji se skupljaju tijekom transakcije i dodaju nakon uspijenog komita
            -> smanjujemo gubitam događaja i održavamo konzistetnost podatka jako bitno za micoservis arch
         2. Optimizirano predmoiriranje
            -> standardiziran nacin generiranje kljuceva za keš
         3. Snažna tipizacija
            -> Sznađna tipizacija koristim enum umjesto stringova te bolja validacija podatka
         4. Unapriđene query analiza
            -> standarniziran metdadata za nalizu upita
            -> podrška ua optimizaciju i lako pracenje performanci
    */



    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", updatable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "author_id")
    //TODO Kada dodamo security onda posatviti ovo na not null i dohvacati iz security contexta
    //@NotNull(message = "Author must be added")
    private UUID authorId;

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private EntityCategory category;

    @Column(name = "difficulty_level")
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;

    @Column(name = "estimated_duration")
   // @NotNull(message = "estimatedDuration is required")
    private Duration estimatedDuration;

    @Column(name = "max_students")
    @Min(value = 1)
    @Max(value = 1000)
    private Integer maxStudents = 100;

    /* Iznaka Transient govori da se polje ne treba spremiti u bazu
      * -> sluzi za privremeno spremanje događaja koja ce se obraditi nakon uspiješne trnsakcije
      * Domain event prestavlja događaje u domeni npr -> kreiranje korisnika
     */
    @Transient
    private final Set<DomainEvent> pendingEvents = new HashSet<>();


    protected BaseModel (){

    }

    // Dodaje događaje u pendigs events, pripremajuci ga za objavu nakon tranzakcije
    protected void registerEvent(DomainEvent event) {
        pendingEvents.add(event);
        log.debug("Registering event {}", event);
    }

    /*
       * -> @PostPersist i @PostUpdate: - callback metode koje se pozivaju nakong uspiješnog spremaja ili azurirana entita
       *  Nakon obrade micemo evente iz privremenog popisa
     */
    @PrePersist
    @PostUpdate
    protected void publishEvents() {
        // event koji ce biti obavljen nakon uspiješne transakcije
        pendingEvents.clear();
    }

    protected boolean isEditableState(){
        return true; // ovo je bazna implementacija koje podklase mogu overidati
    }

        // Metode za podršku particije - pomaze kod horizotalnog skaliranja baze (npr podijela vremena podatka po vremensik periodima
        public LocalDateTime getPartitionKey() {
            return this.createdAt;
        }


        // za podršku kesiranja -> generira jedinstveni kljuc za svaki entitet npr -> "Course:1234-5678"
        public String getCacheKey() {
            return this.getClass().getSimpleName() + ":" + authorId;
        }

       // Metode za podršku query analizi -> pomaze u analizi pita i pracenju podatka o entieti bez izrvong pristupa bazi

        @JsonIgnore
        public Map<String, Object> getQueryMetadata(){
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("entityType", this.getClass().getSimpleName());
            metadata.put("createdAt", this.createdAt);
            metadata.put("category", this.category);
            return metadata;
        }

        @PreUpdate
        protected void validateStateChange() {
             log.debug("Validating state change for entity: {}", this);
             log.debug("Editable state check: {}", isEditableState());
            if (this instanceof Course) {
                ((Course) this).validateStateForUpdate();
            } else if (!isEditableState()) {
                 log.error("Entity is in an invalid state for updates: {}", this);
                 throw new IllegalStateException("Entitet is not in state that allows changes");
             }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseModel baseModel = (BaseModel) o;
        return Objects.equals(authorId, baseModel.authorId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(authorId);
    }


    /*
        klasa nije samo tehnički bolja nego i proširivija. Fokus na validaciju,
         event sourcing, auditing i optimizaciju performansi čini je pogodnom za enterprise aplikacije.

     */

    /*
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

    protected BaseModel() {
    }

     */


}
