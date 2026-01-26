package com.example.Payment_Service.Controller;


//import com.example.InsufficientBalanceException;
//import com.example.PasswordIncorrectException;
//import com.example.PaymentFailedException;

import com.example.Payment_Service.DTO.PaymentRequest;
import com.example.Payment_Service.DTO.PaymentResponse;
import com.example.Payment_Service.ExceptionHandlingPackage.InsufficientBalanceException;
import com.example.Payment_Service.ExceptionHandlingPackage.PasswordIncorrectException;
import com.example.Payment_Service.ExceptionHandlingPackage.PaymentFailedException;
import com.example.Payment_Service.ServicePackage.PaymentServiceClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {


    public PaymentServiceClass paymentServiceClass;

    public PaymentController(PaymentServiceClass paymentServiceClass) {
        this.paymentServiceClass = paymentServiceClass;
    }

    @PostMapping("/createNewEWallet")
    public String createNewEWallet(@RequestParam String username, @RequestParam String userId, @RequestParam String password) {
        return paymentServiceClass.createNewEWallet(username, userId, password);
    }

    @PostMapping("/addMoneyToEWallet")
    public String addMoneyToEWallet(@RequestParam String userId, @RequestParam double amount) {
        return paymentServiceClass.addMoneyToEWallet(userId, amount);
    }

    //    @PostMapping("/paymentRequest")
//    public boolean paymentRequest(@RequestBody PaymentRequest paymentRequest){
//       return paymentServiceClass.paymentRequest(paymentRequest);
//    }
    @PostMapping("/paymentRequest")
    public ResponseEntity<PaymentResponse> paymentRequest(@RequestBody PaymentRequest request) throws InsufficientBalanceException, PasswordIncorrectException, PaymentFailedException {
        return paymentServiceClass.paymentRequest(request.getUserId(), request.getAmount(), request.getPassword());
    }

    @PostMapping("/paymentReturn")
    public void paymentReturn(@RequestParam String transactionID, @RequestParam double eachTicketPrice) {
        paymentServiceClass.paymentReturn(transactionID, eachTicketPrice);
    }
}
