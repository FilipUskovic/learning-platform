package com.micro.learningplatform.shared.exceptions;

import lombok.Getter;
import org.w3c.dom.events.EventException;

@Getter
public sealed abstract class DomainException extends RuntimeException permits CourseException, CustomEventException {

    private final String code;

    protected DomainException(String message, String code) {
        super(message);
        this.code = code;
    }
}

