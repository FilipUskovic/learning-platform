package com.micro.learningplatform.shared.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@Slf4j
public class DatabaseExceptionHandler {

    @ExceptionHandler(PartitionException.class)
    public ResponseEntity<Object> handlePartitionException(
            PartitionException ex,
            WebRequest request) {

        log.error("Partition operation failed", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Database partition operation failed",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(QueryOptimizationException.class)
    public ResponseEntity<Object> handleQueryOptimizationException(
            QueryOptimizationException ex,
            WebRequest request) {

        log.error("Query optimization failed", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Query optimization failed",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            WebRequest request) {

        log.error("Data integrity violation", ex);

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        HttpStatus.CONFLICT,
                        "Data integrity violation occurred",
                        extractConstraintViolationMessage(ex)
                ));
    }

    private String extractConstraintViolationMessage(
            DataIntegrityViolationException ex) {
        if (ex.getCause() instanceof ConstraintViolationException cause) {
            return formatConstraintMessage(cause.getConstraintName());
        }
        return "Unknown constraint violation";
    }

    private String formatConstraintMessage(String constraintName) {
        return "Constraint violation: " + constraintName;
    }
}
