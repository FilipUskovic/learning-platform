package com.micro.learningplatform.api;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseStatus;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.util.UUID;
// Zamjeni ResourceSupport za -> EntityModel<CourseResource> moderni od Hateosa
public class CourseResource extends EntityModel<ResourceSupport>{

    // Reprezentacija coursa s HATEOAS podrškom.

    private final UUID id;
    private final String title;
    private final String description;

    public CourseResource(Course course) {
        this.id = course.getId();
        this.title = course.getTitle();
        this.description = course.getDescription();
        this.add(Link.of("/api/v1/courses/" + id).withSelfRel());
        this.add(Link.of("/api/v1/courses/" + id + "/modules").withRel("modules"));

        /*
        if (status == CourseStatus.PUBLISHED) {
            add(Link.of("/api/v1/courses/" + id + "/enroll").withRel("enroll"));
        }

         */

    }

    /* Stari nacin
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

     */
}
