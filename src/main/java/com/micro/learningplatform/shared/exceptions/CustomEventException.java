package com.micro.learningplatform.shared.exceptions;

public final class CustomEventException extends DomainException{
    private CustomEventException(String message) {
       super(message, "COURSE_ERROR");
    }
}
