package com.micro.learningplatform.api;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseStatus;
import org.springframework.hateoas.Link;

import java.util.UUID;

public class CourseResource extends ResourceSupport{

    // Reprezentacija kursa s HATEOAS podrškom.

    private final UUID id;
    private final String title;
    private final String description;
    private final CourseStatus status;

    public CourseResource(Course course) {
        this.id = course.getId();
        this.title = course.getTitle();
        this.description = course.getDescription();
        this.status = course.getCourseStatus();

        // Dodajemo relevantne linkove
        add(Link.of("/api/v1/courses/" + id).withSelfRel());
        add(Link.of("/api/v1/courses/" + id + "/modules").withRel("modules"));

        if (status == CourseStatus.PUBLISHED) {
            add(Link.of("/api/v1/courses/" + id + "/enroll").withRel("enroll"));
        }
    }
}
