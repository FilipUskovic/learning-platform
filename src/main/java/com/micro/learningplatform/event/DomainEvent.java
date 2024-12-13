package com.micro.learningplatform.event;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {

  //  UUID getEventId();
    LocalDateTime getTimestamp();
}
