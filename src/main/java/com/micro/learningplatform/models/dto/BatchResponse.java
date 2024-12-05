package com.micro.learningplatform.models.dto;

import java.time.LocalDateTime;

public record BatchResponse(String message, LocalDateTime timestamp) {
}
