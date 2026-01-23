package com.example.Booking.Service.ExceptionHandlerPackage;

public class InsufficientBalanceException extends Exception{

    public InsufficientBalanceException(String message) {
        super(message);
    }


}
