package com.micro.learningplatform.models;

import lombok.Getter;

@Getter
public enum EntityCategory {
    // Definiram unaprijed određene kategorije entitea

    COURSE("Course", 100),
    MODULE("Module", 50),
    ASSESSMENT("Assessment", 25),
    RESOURCE("Resource", 10);

    private final String displayName;
    private final int score;

    EntityCategory(String displayName, int score) {
        this.displayName = displayName;
        this.score = score;
    }

}