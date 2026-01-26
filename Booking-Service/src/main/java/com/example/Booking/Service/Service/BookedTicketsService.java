package com.example.Booking.Service.Service;

import com.example.Booking.Service.DTO.*;
import com.example.Booking.Service.Entity.*;
import com.example.Booking.Service.Kafka.BookingEvent;
import com.example.Booking.Service.Repository.BookedTicketsRepo;
import com.example.Booking.Service.Repository.TrainCoachNumberBookingRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BookedTicketsService {

    private final Logger log = LoggerFactory.getLogger(BookedTicketsService.class);

    private BookedTicketsRepo bookedTicketsRepo;

    private TrainCoachNumberBookingRepo trainCoachNumberBookingRepo;

    private BookingEvent bookingEvent;

    public BookedTicketsService(BookedTicketsRepo bookedTicketsRepo, TrainCoachNumberBookingRepo trainCoachNumberBookingRepo,
                                BookingEvent bookingEvent) {
        this.bookedTicketsRepo = bookedTicketsRepo;
        this.trainCoachNumberBookingRepo = trainCoachNumberBookingRepo;
        this.bookingEvent = bookingEvent;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void addTickets(BookingRequestTable requestTable, BookingStatus bookingStatus, String waitingToConfirmTicket, double totalTicketAmount, double eachTicketPrice, String transactionID) {
        log.info("Request in addTickets:{}", requestTable);
        log.info("Booking Status:{}", bookingStatus);
        log.info("TotalTicketAmount:{}", totalTicketAmount);
//        List<PassengerDetailsDTO> passengerDetailsDTO = request.getPassengersList();
        List<TemporaryPassengerDetails> temporaryPassengerDetailsList = requestTable.getTemporaryPassengerDetailsList();
        for (TemporaryPassengerDetails detailsDTO : temporaryPassengerDetailsList) {
            List<PassengerDetails> details = new ArrayList<>();
            List<PassengerDetailsResponse> responseList = new ArrayList<>();
            BookedTicketsAndStatus bookedTicketsAndStatus = new BookedTicketsAndStatus(requestTable, bookingStatus, eachTicketPrice);
            String pnr = UUID.randomUUID().toString().substring(0, 11).replace("-", "");
            bookedTicketsAndStatus.setPnr(pnr);
            bookedTicketsAndStatus.setTransactionID(transactionID);
            bookedTicketsAndStatus.setIsCancellingTicketsClosed("NO");
            // PassengerDetails passengerDetails = new PassengerDetails(detailsDTO.getPassengerName(),detailsDTO.getGender(),detailsDTO.getAge());
            if (bookingStatus.equals(BookingStatus.CONFIRMED)) {
                PassengerDetails passengerDetails = addTrainCoachAndSeatNumber(bookedTicketsAndStatus, requestTable);
                passengerDetails.setPassengerName(detailsDTO.getPassengerName());
                passengerDetails.setGender(detailsDTO.getGender());
                passengerDetails.setAge(detailsDTO.getAge());
                details.add(passengerDetails);
                PassengerDetailsResponse response = new PassengerDetailsResponse(pnr, detailsDTO.getPassengerName(),
                        detailsDTO.getGender(), detailsDTO.getAge(), passengerDetails.getCoachName(),
                        passengerDetails.getCoachNumber(), passengerDetails.getSeatNumber());
                responseList.add(response);
            } else {
                PassengerDetails passengerDetails = new PassengerDetails();
                passengerDetails.setCoachName(requestTable.getCoachName());
                passengerDetails.setPassengerName(detailsDTO.getPassengerName());
                passengerDetails.setGender(detailsDTO.getGender());
                passengerDetails.setAge(detailsDTO.getAge());
                details.add(passengerDetails);
                PassengerDetailsResponse response = new PassengerDetailsResponse(pnr, detailsDTO.getPassengerName(),
                        detailsDTO.getGender(), detailsDTO.getAge(), requestTable.getCoachName());
                responseList.add(response);
            }
            bookedTicketsAndStatus.setPassengersList(details);
            bookedTicketsRepo.save(bookedTicketsAndStatus);
            BookingResponse bookingResponse = new BookingResponse(pnr, requestTable.getUserId(),
                    requestTable.getTrainNumber(), requestTable.getTravelDate(), requestTable.getFromStationName()
                    , requestTable.getToStationName(), 1, requestTable.getBookingMethod(),
                    eachTicketPrice, waitingToConfirmTicket, transactionID, bookingStatus, responseList);
            log.info("Saved BookedTicketsAndStatus:{}", bookedTicketsAndStatus);
            bookingEvent.sendBookingResponseToUser(bookingResponse);
            return;
        }
    }

    private PassengerDetails addTrainCoachAndSeatNumber(BookedTicketsAndStatus bookedTicketsAndStatus, BookingRequestTable requestTable) {
        log.info("Request in addTrainCoachAndSeatNumber Method");
        List<TrainCoachNumberBooking> trainCoachNumberBookingList =
                trainCoachNumberBookingRepo.findAllByTrainNumberAndCoachName(requestTable.getTrainNumber(), requestTable.getCoachName());
        PassengerDetails passengerDetails = new PassengerDetails();
        for (TrainCoachNumberBooking trainCoachNumberBooking : trainCoachNumberBookingList) {
            if (requestTable.getNumberOfTickets() <= trainCoachNumberBooking.getTotalNoOfSeats()) {
                passengerDetails.setCoachNumber(trainCoachNumberBooking.getCoachNumber());
                passengerDetails.setCoachName(trainCoachNumberBooking.getCoachName());
                int seat = trainCoachNumberBooking.getSeats();
                passengerDetails.setSeatNumber(seat);
                trainCoachNumberBooking.setSeats(++seat);
            }
        }
        return passengerDetails;
    }

}

//    @Transactional(propagation = Propagation.REQUIRED)
//    public void addTickets(BookingRequest request, BookingStatus bookingStatus, String waitingToConfirmTicket, double totalTicketAmount, double eachTicketPrice, String transactionID) {
//        log.info("Request in addTickets:{}", request);
//        log.info("Booking Status:{}", bookingStatus);
//        log.info("TotalTicketAmount:{}", totalTicketAmount);
//        List<PassengerDetailsDTO> passengerDetailsDTO = request.getPassengersList();
//        for (PassengerDetailsDTO detailsDTO : passengerDetailsDTO) {
//            List<PassengerDetails> details = new ArrayList<>();
//            List<PassengerDetailsResponse> responseList = new ArrayList<>();
//            BookedTicketsAndStatus bookedTicketsAndStatus = new BookedTicketsAndStatus(request, bookingStatus, eachTicketPrice);
//            String pnr = UUID.randomUUID().toString().substring(0, 11).replace("-", "");
//            bookedTicketsAndStatus.setPnr(pnr);
//            bookedTicketsAndStatus.setTransactionID(transactionID);
//            bookedTicketsAndStatus.setIsCancellingTicketsClosed("NO");
//            // PassengerDetails passengerDetails = new PassengerDetails(detailsDTO.getPassengerName(),detailsDTO.getGender(),detailsDTO.getAge());
//            if (bookingStatus.equals(BookingStatus.CONFIRMED)) {
//                PassengerDetails passengerDetails = addTrainCoachAndSeatNumber(bookedTicketsAndStatus, request);
//                passengerDetails.setPassengerName(detailsDTO.getPassengerName());
//                passengerDetails.setGender(detailsDTO.getGender());
//                passengerDetails.setAge(detailsDTO.getAge());
//                details.add(passengerDetails);
//                PassengerDetailsResponse response = new PassengerDetailsResponse(pnr, detailsDTO.getPassengerName(),
//                        detailsDTO.getGender(), detailsDTO.getAge(), passengerDetails.getCoachName(),
//                        passengerDetails.getCoachNumber(), passengerDetails.getSeatNumber());
//                responseList.add(response);
//            } else {
//                PassengerDetails passengerDetails = new PassengerDetails();
//                passengerDetails.setCoachName(request.getCoachName());
//                passengerDetails.setPassengerName(detailsDTO.getPassengerName());
//                passengerDetails.setGender(detailsDTO.getGender());
//                passengerDetails.setAge(detailsDTO.getAge());
//                details.add(passengerDetails);
//                PassengerDetailsResponse response = new PassengerDetailsResponse(pnr, detailsDTO.getPassengerName(),
//                        detailsDTO.getGender(), detailsDTO.getAge(), request.getCoachName());
//                responseList.add(response);
//            }
//            bookedTicketsAndStatus.setPassengersList(details);
//            bookedTicketsRepo.save(bookedTicketsAndStatus);
//            BookingResponse bookingResponse = new BookingResponse(pnr, request.getUserId(),
//                    request.getTrainNumber(), request.getTravelDate(), request.getFromStationName()
//                    , request.getToStationName(), 1, request.getBookingMethod(),
//                    eachTicketPrice, waitingToConfirmTicket, transactionID, bookingStatus, responseList);
//            log.info("Saved BookedTicketsAndStatus:{}", bookedTicketsAndStatus);
//            bookingEvent.sendBookingResponseToUser(bookingResponse);
//        }
//        return;
//    }
//
//    private PassengerDetails addTrainCoachAndSeatNumber(BookedTicketsAndStatus bookedTicketsAndStatus, BookingRequest request) {
//        List<TrainCoachNumberBooking> trainCoachNumberBookingList =
//                trainCoachNumberBookingRepo.findAllByTrainNumberAndCoachName(request.getTrainNumber(), request.getCoachName());
//        PassengerDetails passengerDetails = new PassengerDetails();
//        for (TrainCoachNumberBooking trainCoachNumberBooking : trainCoachNumberBookingList) {
//            if (request.getNumberOfTickets() <= trainCoachNumberBooking.getTotalNoOfSeats()) {
//                passengerDetails.setCoachNumber(trainCoachNumberBooking.getCoachNumber());
//                passengerDetails.setCoachName(trainCoachNumberBooking.getCoachName());
//                int seat = trainCoachNumberBooking.getSeats();
//                passengerDetails.setSeatNumber(seat);
//                trainCoachNumberBooking.setSeats(++seat);
//            }
//        }
//        return passengerDetails;
//    }
//
//}
