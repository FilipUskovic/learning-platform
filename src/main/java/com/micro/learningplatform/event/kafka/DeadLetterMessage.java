package com.micro.learningplatform.event.kafka;

import java.time.LocalDateTime;

public record DeadLetterMessage(
        Object originalMessage,
        String errorMessage,
        LocalDateTime timestamp
) {}
