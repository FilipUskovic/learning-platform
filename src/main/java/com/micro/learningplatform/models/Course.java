package com.micro.learningplatform.models;

import com.micro.learningplatform.models.dto.CreateCourseRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@AllArgsConstructor
@ToString
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

    protected Course() {
        super(); // pozivamo konstrukt s bazne klase
    }
    // Private konstruktor za factory metodu
    private Course(String title, String description) {
        this.title = title;
        this.description = description;
        this.courseStatus = CourseStatus.DRAFT;
    }

    // Factory metoda - static jer stvara novi objekt
    public static Course create(CreateCourseRequest createCourseRequest) {
        return new Course(createCourseRequest.title(), createCourseRequest.description());
    }

    // Business metode
    public void publish(){
        if(this.courseStatus != CourseStatus.DRAFT){
            throw new IllegalStateException("TCourse can only be published from DRAFT state");
        }
        courseStatus = CourseStatus.PUBLISHED;
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
