package com.example.User_Service.ExceptionHandlerPackage;

public class InsufficientBalanceException extends Exception{

    public InsufficientBalanceException(String message) {
        super(message);
    }


}
