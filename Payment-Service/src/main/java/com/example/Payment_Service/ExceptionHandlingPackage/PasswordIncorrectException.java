package com.example.Payment_Service.ExceptionHandlingPackage;

public class PasswordIncorrectException extends Exception{

    public PasswordIncorrectException (String message) {
        super(message);
    }
}
