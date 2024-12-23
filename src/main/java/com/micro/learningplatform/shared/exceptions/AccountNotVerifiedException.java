package com.micro.learningplatform.shared.exceptions;

public class AccountNotVerifiedException extends RuntimeException {
    public AccountNotVerifiedException(String potrebnaJeVerifikacijaEmailAdrese) {
        super(potrebnaJeVerifikacijaEmailAdrese);
    }
}
