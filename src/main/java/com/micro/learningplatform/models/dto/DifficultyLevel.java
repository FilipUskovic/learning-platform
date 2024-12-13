package com.micro.learningplatform.models.dto;

import lombok.Getter;

@Getter
public enum DifficultyLevel {
    BEGINNER(1, "Beginner", "Good for new studens"),
    INTERMEDIATE(2,"Intermidiate", "You have to know basics"),
    ADVANCED(3,"Advanced", "For preoffesionals"),
    EXPERT(4,"Expert", "For experts");

    private final int score;
    private final String displayName;
    private final String description;

    DifficultyLevel(int score, String displayName, String description) {
        this.score = score;
        this.description = description;
        this.displayName = displayName;
    }

    public boolean isMoreAdvancedThan(DifficultyLevel other) {
        return this.score > other.score;
    }
}
