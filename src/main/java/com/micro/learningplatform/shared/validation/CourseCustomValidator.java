package com.micro.learningplatform.shared.validation;

import com.micro.learningplatform.models.Course;
import com.micro.learningplatform.models.CourseModule;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.HashSet;
import java.util.Set;

public class CourseCustomValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return Course.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Course course = (Course) target;

        validateTitleLength(course, errors);
        validateModulesSequence(course, errors);
      //  validateBusinessRules(course, errors);
    }

    private void validateTitleLength(Course course, Errors errors) {
        if (course.getTitle() != null &&
                (course.getTitle().length() < 3 || course.getTitle().length() > 200)) {
            errors.rejectValue("title", "course.title.length",
                    "Title must be between 3 and 200 characters");
        }
    }

    private void validateModulesSequence(Course course, Errors errors) {
        if (course.getModules() != null) {
            Set<Integer> sequences = new HashSet<>();
            for (CourseModule module : course.getModules()) {
                if (!sequences.add(module.getSequenceNumber())) {
                    errors.rejectValue("modules", "course.modules.sequence.duplicate",
                            "Duplicate sequence numbers are not allowed");
                    break;
                }
            }
        }
    }
}

