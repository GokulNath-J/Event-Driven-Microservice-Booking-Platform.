package com.example.Booking.Service.Service;

import com.example.Booking.Service.DTO.BookingRequest;
import com.example.Booking.Service.DTO.ConfirmOrCancelRequest;
import com.example.Booking.Service.DTO.PaymentRequest;
import com.example.Booking.Service.DTO.PaymentResponse;
import com.example.Booking.Service.Entity.NormalReservationTickets;
import com.example.Booking.Service.Entity.PremiumTatkalTickets;
import com.example.Booking.Service.Entity.TatkalTickets;
import com.example.Booking.Service.ExceptionHandlerPackage.PaymentFailedException;
import com.example.Booking.Service.Feign.PaymentFeign;

import feign.FeignException;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingServiceToPaymentService {

    private final static Logger log = LoggerFactory.getLogger(BookingServiceToPaymentService.class);

    private PaymentFeign paymentFeign;

    private EntityManager entityManager;

    public BookingServiceToPaymentService(EntityManager entityManager, PaymentFeign paymentFeign) {
        this.entityManager = entityManager;
        this.paymentFeign = paymentFeign;
    }

    //    @Transactional
    public void checkEntity(TatkalTickets ticket) {
        boolean isManaged = entityManager.contains(ticket);
        System.out.println("Is entity managed? " + isManaged);
    }

//    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
//    public ResponseEntity<PaymentResponse> bookTatkalTicket(TatkalTickets tickets, BookingRequest request, double totalTicketAmount) throws PaymentFailedException {
//        log.info("Request on BookingServiceToPaymentService");
////        int noOfTickets = request.getNumberOfTickets();
////        tickets.setNoOfSeatsBooked(tickets.getNoOfSeatsBooked() + noOfTickets);
////        tickets.setNoOfSeatsAvailable(tickets.getNoOfSeatsAvailable() - noOfTickets);
//        checkEntity(tickets);
//        log.info("userName:{}", request.getUserId());
//        PaymentResponse paymentResult = new PaymentResponse();
//        try {
//            paymentResult = paymentFeign.paymentRequest(request.getUserId(), totalTicketAmount).getBody();
//            log.info("Payment Result in BookingServiceToPaymentService Try Block:{}", paymentResult);
//            int noOfTickets = request.getNumberOfTickets();
////            tickets.setNoOfSeatsBooked(tickets.getNoOfSeatsBooked() + noOfTickets);
////            tickets.setNoOfSeatsAvailable(tickets.getNoOfSeatsAvailable() - noOfTickets);
//            return ResponseEntity.ok(paymentResult);
//        } catch (FeignException.BadRequest e) {
//            String errorMessage = e.contentUTF8();
//            errorMessage = errorMessage.replace("\"", "");
//            System.out.println("Error message: " + errorMessage);
//            throw new PaymentFailedException(errorMessage);
//        } finally {
//            log.info("Payment Result in BookingServiceToPaymentService finally:{}", paymentResult);
//        }
//    }
//
//    @Transactional
//    public ResponseEntity<PaymentResponse> bookPremiumTatkalTicket(PremiumTatkalTickets premiumTatkalTickets, BookingRequest request, double totalTicketAmount) throws PaymentFailedException {
//        log.info("Request on BookingServiceToPaymentService");
//        // checkEntity(tickets);
//        log.info("userName:{}", request.getUserId());
//        PaymentResponse paymentResult = new PaymentResponse();
//        try {
//            paymentResult = paymentFeign.paymentRequest(request.getUserId(), totalTicketAmount).getBody();
//            log.info("Payment Result in BookingServiceToPaymentService Try Block:{}", paymentResult);
//            int noOfTickets = request.getNumberOfTickets();
////            premiumTatkalTickets.setNoOfSeatsAvailable(premiumTatkalTickets.getNoOfSeatsAvailable() - noOfTickets);
////            premiumTatkalTickets.setNoOfSeatsBooked(premiumTatkalTickets.getNoOfSeatsBooked() + noOfTickets);
//            return ResponseEntity.ok(paymentResult);
//        } catch (FeignException.BadRequest e) {
//            String errorMessage = e.contentUTF8();
//            errorMessage = errorMessage.replace("\"", "");
//            System.out.println("Error message: " + errorMessage);
//            throw new PaymentFailedException(errorMessage);
//        } finally {
//            log.info("Payment Result in BookingServiceToPaymentService finally:{}", paymentResult);
//        }
//    }
//
//    @Transactional
//    public ResponseEntity<PaymentResponse> bookNormalTicket(NormalReservationTickets normalReservationTickets, BookingRequest request, double totalTicketAmount) throws PaymentFailedException {
//        log.info("Request on BookingServiceToPaymentService");
//        // checkEntity(tickets);
//        log.info("userName:{}", request.getUserId());
//        PaymentResponse paymentResult = new PaymentResponse();
//        try {
//            paymentResult = paymentFeign.paymentRequest(request.getUserId(), totalTicketAmount).getBody();
//            log.info("Payment Result in BookingServiceToPaymentService Try Block:{}", paymentResult);
//            int noOfTickets = request.getNumberOfTickets();

    /// /            normalReservationTickets.setNoOfSeatsAvailable(normalReservationTickets.getNoOfSeatsAvailable() - noOfTickets);
    /// /            normalReservationTickets.setNoOfSeatsBooked(normalReservationTickets.getNoOfSeatsBooked() + noOfTickets);
//            return ResponseEntity.ok(paymentResult);
//        } catch (FeignException.BadRequest e) {
//            String errorMessage = e.contentUTF8();
//            errorMessage = errorMessage.replace("\"", "");
//            System.out.println("Error message: " + errorMessage);
//            throw new PaymentFailedException(errorMessage);
//        } finally {
//            log.info("Payment Result in BookingServiceToPaymentService finally:{}", paymentResult);
//        }
//    }
    public void paymentReturn(String transactionID, double eachTicketPrice) {
        paymentFeign.paymentReturn(transactionID, eachTicketPrice);
    }

    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
    public ResponseEntity<PaymentResponse> bookTicket(ConfirmOrCancelRequest confirmOrCancelRequest) throws PaymentFailedException {
        log.info("Request on BookingServiceToPaymentService");
        log.info("userName:{}", confirmOrCancelRequest.getUserId());
        PaymentResponse paymentResult = null;
        try {
            PaymentRequest request = new PaymentRequest(confirmOrCancelRequest.getUserId(), confirmOrCancelRequest.getPassword(),
                    confirmOrCancelRequest.getAmount());
            paymentResult = paymentFeign.paymentRequest(request).getBody();
            log.info("Payment Result in BookingServiceToPaymentService Try Block:{}", paymentResult);
            return ResponseEntity.ok(paymentResult);
        } catch (FeignException.BadRequest e) {
            String errorMessage = e.contentUTF8();
            errorMessage = errorMessage.replace("\"", "");
            System.out.println("Error message: " + errorMessage);
            throw new PaymentFailedException(errorMessage);
        } finally {
            log.info("Payment Result in BookingServiceToPaymentService finally:{}", paymentResult);
        }
    }
}
