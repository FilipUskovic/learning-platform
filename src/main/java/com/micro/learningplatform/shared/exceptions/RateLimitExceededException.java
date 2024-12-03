package com.micro.learningplatform.shared.exceptions;

public class RateLimitExceededException extends Throwable {
    public RateLimitExceededException(String s) {
        super(s);
    }
}
