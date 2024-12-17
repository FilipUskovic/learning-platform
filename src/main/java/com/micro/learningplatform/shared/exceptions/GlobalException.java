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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
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

            // Prikupljanje poruka o greškama
            List<String> errors = validationException.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
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
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, WebRequest request) {
        log.error("General exception handler caught: {}", ex.getMessage());
        log.error("Exception type: {}", ex.getClass().getName());

        logError(ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
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
        if (ex instanceof CourseAlreadyExistsException) {
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



/*
    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCourseNotFound(
            CourseNotFoundException ex, WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Course Not Found",
                Collections.singletonList(ex.getMessage()),
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(CourseAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCourseAlreadyExists(
            CourseAlreadyExistsException ex, WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                Collections.singletonList(ex.getMessage()),
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(CourseValidationException.class)
    public ResponseEntity<ErrorResponse> handleCourseValidationException(CourseValidationException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Course Validation Error",
                ex.getViolations(),
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(CourseStateException.class)
    public ResponseEntity<ErrorResponse> handleCourseStateException(CourseStateException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Invalid Course State Transition",
                Collections.singletonList(String.valueOf(Collections.singletonList(ex.getMessage()))),
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }



    @ExceptionHandler(NoResultException.class)
    public ResponseEntity<ErrorResponse> handleNoResultException(
            NoResultException ex, WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "No Results Found",
                Collections.singletonList(ex.getMessage()),
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                Collections.singletonList(ex.getMessage()),
                request.getDescription(false),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }


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

    private HttpStatus determineHttpStatus(Exception ex) {
        if (ex instanceof CourseNotFoundException || ex instanceof NoResultException || ex instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof CourseAlreadyExistsException) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private void logError(Exception ex) {
        meterRegistry.counter("application.error",
                "type", ex.getClass().getSimpleName(),
                "message", ex.getMessage()).increment();
    }
*/

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


    /*
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        ValidationErrorResponse error = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Obrada CourseNotFoundException
    @ExceptionHandler(CourseNotFoundException.class)
    public ProblemDetail handleCourseNotFound(
            CourseNotFoundException ex, WebRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Course Not Found");
        problemDetail.setInstance(URI.create(request.getDescription(false)));

       return problemDetail;
    }

    // Obrada CourseValidationException
    @ExceptionHandler(CourseValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            CourseValidationException ex) {

        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                ex.getViolations()
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        recordError(ex);


        Problem problem = Problem.builder()
                .withTitle("Internal Server Error")
                .withStatus(Status.INTERNAL_SERVER_ERROR)
                .withDetail("An unexpected error occurred.")
                .withInstance(URI.create(request.getDescription(false).replace("uri=", "")))
                .build();


        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);


    }

    // Obrada ValidationException
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Problem> handleValidation(
            ValidationException ex,
            WebRequest request) {

        ValidationErrorResponse error = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                Collections.singletonList(ex.getMessage())
        );

        return ResponseEntity
                .badRequest()
                .body(Problem.builder()
                        .withStatus(Status.BAD_REQUEST)
                        .withTitle("Validation Error")
                        .withDetail(ex.getMessage())
                        .withInstance(URI.create(request.getDescription(false)))
                        .with("errors", error.violations())
                        .build());
    }

    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, status);
    }

    private void recordError(Exception ex) {
        meterRegistry.counter("application.error",
                        "type", ex.getClass().getSimpleName(),
                        "message", ex.getMessage())
                .increment();
    }

}
     */

