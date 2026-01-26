package com.example.Payment_Service.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingTransaction {

    @Id
    @SequenceGenerator(name = "bk",sequenceName = "transactionSeq",initialValue = 1,allocationSize = 1)
    @GeneratedValue(generator = "bk", strategy = GenerationType.SEQUENCE)
    private int id;
    private String transactionID;
    private String userId;
    private double amount;
    private double amountReturned;
    private double amountAfterReturned;

    @Enumerated(value = EnumType.STRING)
    private TransactionStatus status;

}
