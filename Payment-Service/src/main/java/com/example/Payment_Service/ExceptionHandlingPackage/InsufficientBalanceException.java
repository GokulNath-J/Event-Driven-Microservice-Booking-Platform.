package com.example.Payment_Service.ExceptionHandlingPackage;

public class InsufficientBalanceException extends Exception{

    public InsufficientBalanceException(String message) {
        super(message);
    }


}
