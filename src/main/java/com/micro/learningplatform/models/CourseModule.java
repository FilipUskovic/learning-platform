package com.micro.learningplatform.models;

import com.micro.learningplatform.models.dto.CreateModuleRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "course_modules")
@Getter
@Setter
@AllArgsConstructor
public class CourseModule extends BaseModel {

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
        module.Id = UUID.randomUUID();
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
}
