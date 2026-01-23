package com.example.User_Service.ExceptionHandlerPackage;

public class PaymentFailedException extends Exception{

    public PaymentFailedException(String message) {
        super(message);
    }


}
