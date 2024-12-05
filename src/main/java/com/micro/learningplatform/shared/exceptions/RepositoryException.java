package com.micro.learningplatform.shared.exceptions;

public class RepositoryException extends Throwable {
    public RepositoryException(String s, Exception e) {
        super(s, e);
    }
}
