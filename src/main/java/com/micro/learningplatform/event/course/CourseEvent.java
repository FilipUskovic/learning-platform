package com.micro.learningplatform.event.course;

import com.micro.learningplatform.event.DomainEvent;

import java.util.UUID;

public interface CourseEvent extends DomainEvent {
    UUID getCourseId();

}
