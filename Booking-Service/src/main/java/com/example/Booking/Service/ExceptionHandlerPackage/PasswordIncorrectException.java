package com.example.Booking.Service.ExceptionHandlerPackage;

public class PasswordIncorrectException extends Exception{

    public PasswordIncorrectException (String message) {
        super(message);
    }
}
