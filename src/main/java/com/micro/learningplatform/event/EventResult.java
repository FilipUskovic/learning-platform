package com.micro.learningplatform.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResult(
        UUID eventId,
        String eventJson,
        LocalDateTime timestamp
) {
}
