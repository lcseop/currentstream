package com.currentstreambackend.currentstreambackend.models.common;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("EMAIL_NOT_VERIFIED");
    }
}