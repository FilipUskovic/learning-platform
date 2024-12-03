package com.micro.learningplatform.api;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ApiResponse<T> {

    // Bazni dto za sve api odgovore osigurava konzistetnost u komunikacij i olaksava verzioniranje u buducnosti

    private final T data;
    private final String version;
    private final LocalDateTime timestamp;
    private final String requestId;

    public ApiResponse(T data, String version, LocalDateTime timestamp, String requestId) {
        this.data = data;
        this.version = "v1";
        this.timestamp = LocalDateTime.now();
        this.requestId = UUID.randomUUID().toString();
    }


}
