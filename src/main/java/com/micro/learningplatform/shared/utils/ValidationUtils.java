package com.micro.learningplatform.shared.utils;

import com.micro.learningplatform.models.CourseStatus;
import lombok.experimental.UtilityClass;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@UtilityClass
public class ValidationUtils {
    public static UUID parseUUID(String id) {
        return Optional.ofNullable(id)
                .map(rawId -> {
                    try {
                        return UUID.fromString(rawId);
                    } catch (IllegalArgumentException e) {
                        throw new InvalidParameterException("Invalid UUID format: " + rawId);
                    }
                })
                .orElseThrow(() -> new InvalidParameterException("ID cannot be null"));
    }

    public static CourseStatus parseCourseStatus(String status) {
        return Optional.ofNullable(status)
                .map(String::toUpperCase)
                .map(statusStr -> {
                    try {
                        return CourseStatus.valueOf(statusStr);
                    } catch (IllegalArgumentException e) {
                        throw new InvalidParameterException(
                                "Invalid status. Must be one of: " +
                                        Arrays.toString(CourseStatus.values())
                        );
                    }
                })
                .orElseThrow(() -> new InvalidParameterException("Status cannot be null"));
    }
}
