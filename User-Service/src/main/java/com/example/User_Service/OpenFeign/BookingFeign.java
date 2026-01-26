package com.example.User_Service.OpenFeign;


import com.example.User_Service.DTO.*;
import com.example.User_Service.ExceptionHandlerPackage.PaymentFailedException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "Booking-Service")
public interface BookingFeign {

    @PostMapping("/booking/bookPremiumAndTatkal")
    public ResponseEntity<TicketsResponse> bookPremiumAndTatkal(@RequestBody BookingRequest request) throws PaymentFailedException;

    @PostMapping("/booking/bookNormalReservation")
    public ResponseEntity<TicketsResponse> bookNormalReservation(@RequestBody BookingRequest request) throws PaymentFailedException;

    @PutMapping("/booking/bookingCancelRequest")
    public ResponseEntity<String> bookingCancelRequest(@RequestBody BookingCancelRequestDTO bookingCancelRequestDTO);

    @PostMapping("/booking/getTrainForNormalBookingByTrainNumber")
    public List<TicketDTO> getTrainForNormalBookingByTrainNumber(@RequestBody TrainDetailsRequest request);

    @PostMapping("/booking/getTrainForTatkalBookingByTrainNumber")
    public List<TicketDTO> getTrainForTatkalBookingByTrainNumber(@RequestBody TrainDetailsRequest request);

    @PostMapping("/booking/getTrainForPremiumTatkalBookingByTrainNumber")
    public List<TicketDTO> getTrainForPremiumTatkalBookingByTrainNumber(@RequestBody TrainDetailsRequest request);

    @PostMapping("/booking/confirmOrCancelRequest")
    public ResponseEntity<String> confirmOrCancelRequest(@RequestBody ConfirmOrCancelRequest request);
}
