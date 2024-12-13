package com.micro.learningplatform.event.module;

import com.micro.learningplatform.event.DomainEvent;

import java.util.UUID;

public interface ModuleEvent extends DomainEvent {
    UUID getModuleId();
}
