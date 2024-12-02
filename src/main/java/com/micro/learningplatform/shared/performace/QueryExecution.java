package com.micro.learningplatform.shared.performace;

import java.time.LocalDateTime;

public record QueryExecution(
        long duration,
        LocalDateTime executionTime
) {
}
