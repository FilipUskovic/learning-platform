package com.micro.learningplatform.models;



public enum CourseStatus{
    DRAFT, PUBLISHED, ARCHIVED, APPROVED, REJECTED;

    public boolean canTransitionTo(CourseStatus newStatus) {
        return switch(this) {
            case DRAFT -> newStatus == PUBLISHED;
            case PUBLISHED -> newStatus == ARCHIVED;
            case ARCHIVED -> false;
            case APPROVED -> true;
            case REJECTED -> false;
        };
    }

    public boolean allowsModification() {
        return this == DRAFT;
    }

}
