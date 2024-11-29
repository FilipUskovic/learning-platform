package com.micro.learningplatform.shared.exceptions;

import java.util.List;

public class CourseValidationException extends RuntimeException {
    private final List<String> violations;

    public CourseValidationException(List<String> violations) {
        super("Course validation failed");
        this.violations = violations;
    }

    public List<String> getViolations() {
        return violations;
    }
}
