package com.example.Booking.Service.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {


    private TransactionStatus paymentStatus;
    private String transactionID;

}
