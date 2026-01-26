package com.example.Payment_Service.DTO;


import com.example.Payment_Service.Entity.TransactionStatus;
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
