package com.micro.learningplatform.shared.validation;

import com.micro.learningplatform.models.dto.CreateCourseRequest;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class CreateCourseRequestValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return CreateCourseRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

    }


}
