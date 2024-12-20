package com.micro.learningplatform.shared.exceptions;

import com.micro.learningplatform.shared.validation.ValidationErrorResponse;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.NoResultException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE) // ima manji prioritet nego security ecxeptions
public class GlobalException extends ResponseEntityExceptionHandler {

    private final MeterRegistry meterRegistry;

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        log.error("Validation failed: {}", ex.getMessage());

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed for request fields",
                errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        if (ex instanceof HandlerMethodValidationException validationException) {
            log.error("HandlerMethodValidationException: {}", ex.getMessage());

            List<String> errors = validationException.getAllErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .toList();

            ValidationErrorResponse response = new ValidationErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Validation failed for request fields",
                    errors
            );

            return ResponseEntity.badRequest().body(response);
        }

        // Sve ostale greške proslijedi default handleru
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, WebRequest request) throws Exception {
        log.error("General exception handler caught: {}", ex.getMessage());
        log.error("Exception type: {}", ex.getClass().getName());

       // Proslijedi specifične iznimke dalje
        if (ex instanceof UserAlreadyExistsException || ex instanceof InvalidTokenException) {
            throw ex; // Ovo omogućuje specifičnim handlerima da obrade te iznimke
        }

        logError(ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    @ExceptionHandler(CourseValidationException.class)
    public ResponseEntity<ErrorResponse> handleCourseValidationException(CourseValidationException ex, WebRequest request) {
        log.error("Validation error occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Course validation failed",
                ex.getViolations(), // Lista detaljnih grešaka
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }



    @ExceptionHandler({
            CourseNotFoundException.class,
            CourseAlreadyExistsException.class,
            NoResultException.class,
            ResourceNotFoundException.class
    })

    public ResponseEntity<ErrorResponse> handleCustomExceptions(RuntimeException ex, WebRequest request) {
        HttpStatus status = determineHttpStatus(ex);
        return buildErrorResponse(status, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        String message = "A data integrity error occurred. Check for duplicates or invalid data.";
        return buildErrorResponse(HttpStatus.CONFLICT, message, request);
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<String> violations = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .toList();

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                violations,
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    // 6. Metoda za izgradnju standardiziranog odgovora
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                Collections.singletonList(message),
                request.getDescription(false),
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    // 7. Dinamičko određivanje HTTP statusa za custom iznimke
    private HttpStatus determineHttpStatus(Exception ex) {
        if (ex instanceof CourseNotFoundException || ex instanceof NoResultException || ex instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof CourseAlreadyExistsException || ex instanceof UserAlreadyExistsException) {
            return HttpStatus.CONFLICT;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }



    // 8. Bilježenje grešaka za metrike
    private void logError(Exception ex) {
        meterRegistry.counter("application.error",
                "type", ex.getClass().getSimpleName(),
                "message", ex.getMessage()).increment();
    }


    // todo premjesitit u odvojeno klasu

    @Getter
    public static class ErrorResponse {
        private final int status;
        private final String error;
        private final List<String> messages;
        private final String path;
        private final LocalDateTime timestamp;

        public ErrorResponse(int status, String error, List<String> messages, String path, LocalDateTime timestamp) {
            this.status = status;
            this.error = error;
            this.messages = messages;
            this.path = path;
            this.timestamp = timestamp;
        }

    }
}


