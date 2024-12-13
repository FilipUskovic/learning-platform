package com.micro.learningplatform.models.dto.courses;

import com.micro.learningplatform.models.CourseStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;

public record CourseSearchRequest(
        @Size(max = 200, message = "Search term cannot exceed 200 characters")
        String searchTerm,

        CourseStatus status,

        @PositiveOrZero(message = "Page number must be positive or zero")
        Integer page,

        @Min(value = 5, message = "Page size must be at least 5")
        @Max(value = 100, message = "Page size must not exceed 100")
        Integer size
) {
    // Konstruktor s default vrijednostima

    public CourseSearchRequest {
        page = Objects.requireNonNullElse(page, 0);
        size = Objects.requireNonNullElse(size, 20);
        searchTerm = searchTerm != null ? searchTerm.trim() : null;
    }

    public Pageable getPageable() {
        return PageRequest.of(page, size, Sort.by("createdAt").ascending());
    }
}
