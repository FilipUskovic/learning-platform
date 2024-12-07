package com.micro.learningplatform.models;

import com.micro.learningplatform.event.DomainEvent;
import com.micro.learningplatform.models.dto.CreateCourseRequest;
import com.micro.learningplatform.shared.exceptions.CourseValidationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cache;

import java.util.*;

@Entity
@Table(name = "courses")
@Getter
@Setter
@AllArgsConstructor
//Secong level cache za smanjejne opterecenja baze
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Course extends BaseModel{
    private static final int MINIMUM_MODULES_FOR_PUBLICATION = 1;
    private static final Logger log = LogManager.getLogger(Course.class);


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    //TODO: razmisliti o dodavanju sequence generatora
    private UUID Id;

    @Size(min = 1, max = 200)
    @Column(nullable = false, unique = true)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus courseStatus;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("sequenceNumber")
    // Secong level cache za smanjejne opterecenja baze
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<CourseModule> modules = new ArrayList<>();

    @Embedded
    private CourseStatistics statistics;

    // zelim da statisitka uvijek postoji
    protected Course() {
        this.statistics = new CourseStatistics();
    }

    //TODO: ostaviti samo osnovnu domensku logiku, ostalo premjesiti prema ddd prinicipma

    // Private konstruktor za factory metodu
    private Course(String title, String description) {
        this(); // da bi se cons za incijijalizaciju tecaja pravilno pozvao
        this.title = title;
        this.description = description;
        this.courseStatus = CourseStatus.DRAFT;
    }

    // Factory metoda - static jer stvara novi objekt
    public static Course create(CreateCourseRequest createCourseRequest) {
        return new Course(createCourseRequest.title(), createCourseRequest.description());
    }

    // Domenski važne metode ostaju u entitetu

    public void addModule(CourseModule courseModule){
        log.info("Adding module to course: {}", this.Id);
        validateModuleAddition(courseModule);
        log.info("Validation passed for module: {}", courseModule);

        courseModule.setCourse(this);
        modules.add(courseModule);
        log.info("Module added to course. Total modules: {}", this.modules.size());

        statistics.recalculate(modules);


    }


    public void publish() {
        if (!courseStatus.canTransitionTo(CourseStatus.PUBLISHED)) {
            throw new IllegalStateException("Course can only be published from DRAFT state");
        }
        courseStatus = CourseStatus.PUBLISHED;
    }

    private void validateModuleAddition(CourseModule module) {
        if (!courseStatus.allowsModification()) {
            throw new IllegalStateException("Can only add modules to courses in DRAFT status");
        }
        Objects.requireNonNull(module, "Module cannot be null");
    }


    @Override
    public String toString() {
        return "Course{" +
                "Id=" + Id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", courseStatus=" + courseStatus +
                ", modules=" + modules +
                ", statistics=" + statistics +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(Id, course.Id) && Objects.equals(title, course.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Id, title);
    }
}
