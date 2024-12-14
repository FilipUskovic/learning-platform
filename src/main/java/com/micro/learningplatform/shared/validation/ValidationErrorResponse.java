package com.micro.learningplatform.shared.validation;


import java.time.LocalDateTime;
import java.util.List;


public record ValidationErrorResponse(int status, String message, List<String> violations) {


}
