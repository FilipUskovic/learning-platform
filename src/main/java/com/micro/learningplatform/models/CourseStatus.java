package com.micro.learningplatform.models;

public enum CourseStatus{
    DRAFT, PUBLISHED, ARCHIVED, APPROVED, REJECTED;

    public boolean canTransitionTo(CourseStatus newStatus) {
        return switch(this) {
            case DRAFT -> newStatus == PUBLISHED;
            case PUBLISHED -> newStatus == ARCHIVED;
            case ARCHIVED, REJECTED -> false;
            case APPROVED -> true;
        };
    }

    public boolean allowsModification() {
        return this == DRAFT;
    }

}
