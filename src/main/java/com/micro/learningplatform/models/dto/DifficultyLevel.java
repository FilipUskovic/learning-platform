package com.micro.learningplatform.models.dto;

import lombok.Getter;

@Getter
public enum DifficultyLevel {
    BEGINNER(1, "Beginner"),
    INTERMEDIATE(2,"Intermidiate"),
    ADVANCED(3,"Advanced"),
    EXPERT(4,"Expert");

    private final int level;
    private final String displayName;

    DifficultyLevel(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public boolean isMoreAdvanceThan(DifficultyLevel other) {
        return this.level > other.level;
    }
}
