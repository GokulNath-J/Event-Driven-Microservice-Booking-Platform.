package com.example.Payment_Service.ServicePackage;


import com.example.Payment_Service.DTO.PaymentResponse;
import com.example.Payment_Service.Entity.BookingTransaction;
import com.example.Payment_Service.Entity.EWalletDetails;
import com.example.Payment_Service.Entity.TransactionStatus;
import com.example.Payment_Service.ExceptionHandlingPackage.InsufficientBalanceException;
import com.example.Payment_Service.ExceptionHandlingPackage.PasswordIncorrectException;
import com.example.Payment_Service.ExceptionHandlingPackage.PaymentFailedException;
import com.example.Payment_Service.Repository.BookingTransactionRepo;
import com.example.Payment_Service.Repository.EWalletDetailsRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Scanner;
import java.util.UUID;

@Service
public class PaymentServiceClass {

    private final Logger logger = LoggerFactory.getLogger(PaymentServiceClass.class);

    private EWalletDetailsRepo eWalletDetailsRepo;

    private BookingTransactionRepo bookingTransactionRepo;

    public PaymentServiceClass(EWalletDetailsRepo eWalletDetailsRepo, BookingTransactionRepo bookingTransactionRepo) {
        this.eWalletDetailsRepo = eWalletDetailsRepo;
        this.bookingTransactionRepo = bookingTransactionRepo;
    }

    public String createNewEWallet(String username, String userId, String password) {
        EWalletDetails eWalletDetails = new EWalletDetails(username, userId, password);
        eWalletDetailsRepo.save(eWalletDetails);
        return "EWallet Created";
    }

    public String addMoneyToEWallet(String userId, double amount) {
        EWalletDetails walletDetails = eWalletDetailsRepo.findByUserId(userId);
        if (walletDetails != null) {
            walletDetails.setAmount(amount);
            eWalletDetailsRepo.save(walletDetails);
            return "Amount Added Successfully";
        } else {
            return "Wallet Not Found";
        }
    }

    public ResponseEntity<PaymentResponse> paymentRequest(String userName, double totalTicketAmount) throws PaymentFailedException, InsufficientBalanceException, PasswordIncorrectException {
        logger.info("username:{},totalTicketAmount:{}", userName, totalTicketAmount);
        EWalletDetails eWalletDetails = eWalletDetailsRepo.findByUserName(userName);
        logger.info("eWalletDetails:{}", eWalletDetails);
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Password:");
        String password = scanner.nextLine();
        if (eWalletDetails.getPassword().equals(password)) {
            logger.info("Password correct");
            double eWalletAmount = eWalletDetails.getAmount();
            if (totalTicketAmount <= eWalletAmount) {
                eWalletDetails.setAmount(eWalletAmount - totalTicketAmount);
                eWalletDetailsRepo.save(eWalletDetails);
                BookingTransaction bookingTransaction = new BookingTransaction();
                String transactionID = UUID.randomUUID().toString().substring(0, 14).replace("-", "");
                bookingTransaction.setTransactionID(transactionID);
                bookingTransaction.setUserName(userName);
                bookingTransaction.setAmount(totalTicketAmount);
                bookingTransaction.setStatus(TransactionStatus.Success);
                bookingTransactionRepo.save(bookingTransaction);
                PaymentResponse paymentResponse = new PaymentResponse("Payment Success", transactionID);
                return ResponseEntity.ok(paymentResponse);
            } else {
                logger.info("Insuffient Amount:{}", totalTicketAmount);
                System.out.println("Insuffient Amount");
                BookingTransaction bookingTransaction = new BookingTransaction();
                String transactionID = UUID.randomUUID().toString().substring(0, 14).replace("-", "");
                bookingTransaction.setTransactionID(transactionID);
                bookingTransaction.setUserName(userName);
                bookingTransaction.setAmount(totalTicketAmount);
                bookingTransaction.setStatus(TransactionStatus.Failed);
                bookingTransactionRepo.save(bookingTransaction);
                throw new InsufficientBalanceException("InsufficientBalanceException");
            }
        } else {
            logger.info("Password Incorrect:{}", password);
            throw new PasswordIncorrectException("PasswordIncorrectException");
        }
    }

    @Transactional
    public void paymentReturn(String transactionID, double eachTicketPrice) {
        BookingTransaction bookingTransaction = bookingTransactionRepo.findByTransactionID(transactionID);
        double getTotalAmount = bookingTransaction.getAmount();
        bookingTransaction.setAmountReturned(eachTicketPrice);
        bookingTransaction.setAmountAfterReturned(getTotalAmount - eachTicketPrice);
    }


//        private EWalletDetailsRepo eWalletDetailsRepo;
//
//    public String createNewEWallet(EWallet eWallet) {
//        EWalletDetails eWalletDetails = new EWalletDetails(eWallet.getEWalletNumber()
//                ,eWallet.getUserName(),eWallet.getPassword(),eWallet.getAmount());
//        eWalletDetailsRepo.save(eWalletDetails);
//        return "EWallet is Sucessfully Added";
//    }

}

