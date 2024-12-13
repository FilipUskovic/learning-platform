package com.micro.learningplatform.models;

import com.micro.learningplatform.event.module.ModuleContentUpdatedEvent;
import com.micro.learningplatform.event.module.ModuleCreatedEvent;
import com.micro.learningplatform.event.module.ModulePrerequisiteAddedEvent;
import com.micro.learningplatform.models.dto.module.CreateModuleRequest;
import com.micro.learningplatform.models.dto.module.ModuleData;
import com.micro.learningplatform.models.dto.module.UpdateModuleRequest;
import com.micro.learningplatform.shared.exceptions.ModuleStateException;
import com.micro.learningplatform.shared.exceptions.ModuleValidationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cache;
import java.time.Duration;
import java.util.*;

@Entity
@Table(name = "course_modules")
@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CourseModule extends BaseModel {

    /* Ova klas pruza
      1. pobljem upravljanjem stanjem kroz sustav kroz modulestatus enum i validaciju stanja
      2. odrška prequisitia za ucenje krot preqeusitie kolekicju i robusna validacija
      3- odvajanje između kreacije i zuriranja
      4. integracija s event sustavom kroz specificne dogadaje
      5. podrška za predmomoriranje
     */

    private static final Duration MINIMUM_DURATION = Duration.ofMinutes(5);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false)
    @Size(min = 1, max = 255)
    @NotBlank(message = "title cannot be emtpy")
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name ="sequence_number", nullable = false)
    @Min(value = 0, message = "sequence number must be postive")
    private Integer sequenceNumber;

    @Column(name = "duration")
    @NotNull(message = "duration of modula is needed")
    private Duration duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ModuleStatus status = ModuleStatus.DRAFT;

    /**
     * clementCollection: -> definira kolekciju snovnih tipoiva ili ugrađenih klasa
     * collectionTable -> sprema preduvjete  zasebnu tablicu "module_prerequisites"  gjde module_id povezuje moduel s preduviejtima
     */

    @ElementCollection
    @CollectionTable(
            name = "module_prerequisites",
            joinColumns = @JoinColumn(name = "module_id")
    )
    private Set<UUID> prerequisites = new HashSet<>();

    protected CourseModule() {
        // super();
        setCategory(EntityCategory.MODULE);
    }

    // Postavlja naslov, opis, redni broj i trajanje. i registira događaj
    public static CourseModule create(CreateModuleRequest request) {
        validateRequest(request);
        CourseModule module = new CourseModule();
        module.setTitle(request.title());
        module.setDescription(request.description());
        module.setSequenceNumber(request.sequenceNumber());
        module.setDuration(request.duration());

        module.registerEvent(new ModuleCreatedEvent(module.getId()));
        return module;
    }

    // Pohranjuje prethodni sadržaj kako bi se omogućilo praćenje promjena.
    public void updateContent(UpdateModuleRequest request){
        validateModuleIsEditable();
       // validateRequest(request);
        ModuleData previousContent = new ModuleData(this.title, this.description, this.duration);

        this.title = request.title();
        this.description = request.description();
        this.duration = request.duration();

        registerEvent(new ModuleContentUpdatedEvent(
                this.getId(),
                previousContent,
                new ModuleData(this.title, this.description, this.duration)
        ));
    }

    public void addPrerequisite(CourseModule prerequisite) {
        validateModuleIsEditable();
        validatePrerequisite(prerequisite);

        prerequisites.add(prerequisite.getId());
        registerEvent(new ModulePrerequisiteAddedEvent(this.getId(), prerequisite.getId()));
    }



    private void validateModuleIsEditable() {
        if (!isEditableState()) {
            throw new ModuleStateException("Modul can be edit only in DRAFT stauts");
        }
    }

    private static void validateRequest(CreateModuleRequest request) {
        Objects.requireNonNull(request, "Request cannot be null");
        Objects.requireNonNull(request.title(), "Title of modula is needed");

        if (request.duration() != null && request.duration().compareTo(MINIMUM_DURATION) < 0) {
            throw new ModuleValidationException(
                    "Duration of modules have to be at least " + MINIMUM_DURATION.toMinutes() + " min"
            );
        }
    }

    private void validatePrerequisite(CourseModule prerequisite) {
        if (prerequisite == null) {
            throw new ModuleValidationException("prerequisites cannot be null");
        }
        if (this.equals(prerequisite)) {
            throw new ModuleValidationException("Modul cannot be self prerequisites");
        }
        if (!this.course.equals(prerequisite.course)) {
            throw new ModuleValidationException("prerequisites must be part of same course");
        }
        if (prerequisite.getPrerequisites().contains(this.getId())) {
            throw new ModuleValidationException("Cuircle refrence in prerequisites is not accaptable");
        }
    }

    @Override
    public String getCacheKey() {
        return String.format("module:%s:course:%s", this.getId(), this.course.getId());
    }

    @Override
    public Map<String, Object> getQueryMetadata() {
        Map<String, Object> metadata = super.getQueryMetadata();
        metadata.put("moduleStatus", this.status);
        metadata.put("sequenceNumber", this.sequenceNumber);
        metadata.put("prerequisiteCount", this.prerequisites.size());
        return metadata;
    }

   /*
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "duration")
    private Duration duration;

    protected CourseModule() {}

    // Factory metoda za kreiranje modula

    public static CourseModule create(CreateModuleRequest request) {
        CourseModule module = new CourseModule();
       // module.Id = UUID.randomUUID();
        module.title = Objects.requireNonNull(request.title(), "Title must not be null");
        module.description = request.description();
        module.sequenceNumber = request.sequenceNumber();
        module.duration = request.duration();
        return module;
    }

    @Override
    public String toString() {
        return "CourseModule{" +
                "Id=" + Id +
                ", course=" + course +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                ", duration=" + duration +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CourseModule that = (CourseModule) o;
        return Objects.equals(Id, that.Id) && Objects.equals(course, that.course);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Id, course);
    }

    */
}
