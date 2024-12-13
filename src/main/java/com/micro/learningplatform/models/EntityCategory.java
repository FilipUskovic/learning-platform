package com.micro.learningplatform.models;

import lombok.Getter;

@Getter
public enum EntityCategory {
    // Definiram unaprijed određene kategorije entitea

    COURSE("Course"),
    MODULE("Module"),
    ASSESSMENT("Assessment"),
    RESOURCE("Resource");

    private final String displayName;

    EntityCategory(String displayName) {
        this.displayName = displayName;
    }

}