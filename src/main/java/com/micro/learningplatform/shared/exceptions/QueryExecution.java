package com.micro.learningplatform.shared.exceptions;

import java.time.LocalDateTime;

public record QueryExecution(
        long duration,
        LocalDateTime executionTime
) {
}
