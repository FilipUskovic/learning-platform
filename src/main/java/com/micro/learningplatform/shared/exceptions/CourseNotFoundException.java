package com.micro.learningplatform.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(UUID id) {
        super("Course not found with id: " + id);
    }
}
