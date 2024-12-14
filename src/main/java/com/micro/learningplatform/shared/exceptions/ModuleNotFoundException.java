package com.micro.learningplatform.shared.exceptions;

import java.util.UUID;

public class ModuleNotFoundException extends RuntimeException {
    public ModuleNotFoundException(UUID moduleId) {
        super("Module with id " + moduleId + " not found");
    }
}
