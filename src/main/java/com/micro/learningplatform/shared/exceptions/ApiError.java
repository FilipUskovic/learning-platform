package com.micro.learningplatform.shared.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ApiError {
    public ApiError(HttpStatus httpStatus, String databasePartitionOperationFailed, String message) {

    }
}
