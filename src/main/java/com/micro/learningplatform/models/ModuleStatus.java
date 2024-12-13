package com.micro.learningplatform.models;

import lombok.Getter;

@Getter
public enum ModuleStatus{
    DRAFT("Draft", "Modul in progress", true),
    PUBLISHED("Published", "Modul is published and alive", false),
    COMPLETED("Completed", "Modul is completed", false);

    private final String displayName;
    private final String description;
    private final boolean editable;

    ModuleStatus(String displayName, String description, boolean editable) {
        this.displayName = displayName;
        this.description = description;
        this.editable = editable;
    }

}
