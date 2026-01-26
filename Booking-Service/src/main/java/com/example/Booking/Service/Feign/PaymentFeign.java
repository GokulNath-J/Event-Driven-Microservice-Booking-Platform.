package com.example.Booking.Service.Feign;

import com.example.Booking.Service.DTO.PaymentRequest;
import com.example.Booking.Service.DTO.PaymentResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "Payment-Service")
public interface PaymentFeign {

    @PostMapping("/payment/paymentRequest")
    public ResponseEntity<PaymentResponse> paymentRequest(@RequestBody PaymentRequest request);

    @PostMapping("/payment/paymentReturn")
    public void paymentReturn(@RequestParam String transactionID, @RequestParam double eachTicketPrice);
}
