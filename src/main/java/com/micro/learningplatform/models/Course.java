package com.micro.learningplatform.models;

import com.micro.learningplatform.models.dto.CreateCourseRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@AllArgsConstructor
//Secong level cache za smanjejne opterecenja baze
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Course extends BaseModel{


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



    protected Course() {
      //  super(); // pozivamo konstrukt s bazne klase
    }

    //TODO: ostaviti samo osnovnu domensku logiku, ostalo premjesiti prema ddd prinicipma

    // Private konstruktor za factory metodu
    private Course(String title, String description) {
        this.Id = UUID.randomUUID();
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
        validateModuleAddition(courseModule);
        modules.add(courseModule);
        courseModule.setCourse(this);

    }

    public void publish(){
        if(this.courseStatus != CourseStatus.DRAFT){
            throw new IllegalStateException("TCourse can only be published from DRAFT state");
        }
        courseStatus = CourseStatus.PUBLISHED;
    }

    private void validateModuleAddition(CourseModule module) {
        if (courseStatus != CourseStatus.DRAFT) {
            throw new IllegalStateException(
                    "Can only add modules to courses in DRAFT status");
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
