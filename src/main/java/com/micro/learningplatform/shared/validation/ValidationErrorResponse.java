package com.micro.learningplatform.shared.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private List<Violation> violations = new ArrayList<>();


    public ValidationErrorResponse(int value, String validationFailed, List<String> errors, LocalDateTime now) {
    }
}
