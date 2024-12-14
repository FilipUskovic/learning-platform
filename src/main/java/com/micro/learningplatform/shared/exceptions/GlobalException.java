package com.micro.learningplatform.shared.exceptions;

import com.micro.learningplatform.shared.validation.ValidationErrorResponse;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalException extends ResponseEntityExceptionHandler {

    private final MeterRegistry meterRegistry;


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

    private void recordError(Exception ex) {
        meterRegistry.counter("application.error",
                        "type", ex.getClass().getSimpleName(),
                        "message", ex.getMessage())
                .increment();
    }

}
