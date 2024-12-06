package com.micro.learningplatform.shared.exceptions;

public final class CourseException extends DomainException {
    public CourseException(String message) {
        super(message, "COURSE_ERROR");
    }

}
