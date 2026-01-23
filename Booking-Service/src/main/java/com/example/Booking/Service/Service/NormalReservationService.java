package com.example.Booking.Service.Service;

import com.example.Booking.Service.DTO.BookingRequest;
import com.example.Booking.Service.DTO.BookingStatus;
import com.example.Booking.Service.DTO.PaymentResponse;
import com.example.Booking.Service.DTO.TicketPrice;
import com.example.Booking.Service.Entity.NormalReservationTickets;
import com.example.Booking.Service.ExceptionHandlerPackage.PaymentFailedException;
import com.example.Booking.Service.Repository.NormalReservationRepo;
import com.example.Booking.Service.Repository.TicketPriceRepo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;


@Slf4j
@Service
public class NormalReservationService {

    private final static Logger log = LoggerFactory.getLogger(NormalReservationService.class);

    private NormalReservationRepo normalReservationRepo;

    private TicketPriceRepo ticketPriceRepo;

    private BookingServiceToPaymentService bookingServiceToPaymentService;

    private BookedTicketsService bookedTicketsService;

    public NormalReservationService(NormalReservationRepo normalReservationRepo, TicketPriceRepo ticketPriceRepo,
                                    BookingServiceToPaymentService bookingServiceToPaymentService,
                                    BookedTicketsService bookedTicketsService) {
        this.normalReservationRepo = normalReservationRepo;
        this.ticketPriceRepo = ticketPriceRepo;
        this.bookingServiceToPaymentService = bookingServiceToPaymentService;
        this.bookedTicketsService = bookedTicketsService;
    }

    public ResponseEntity<String> bookNormalReservationTickets(BookingRequest request) throws PaymentFailedException {
        log.info("BookingRequest:{}", request);
        List<NormalReservationTickets> ticketsList = normalReservationRepo.findAllByTrainNumber(request.getTrainNumber());
        log.info("TicketList:{}", ticketsList);
        if (ticketsList != null) {
            log.info("(ticketsList != null): True");
            NormalReservationTickets normalTickets = new NormalReservationTickets();
            for (NormalReservationTickets normalReservationTickets : ticketsList) {
                if (normalReservationTickets.getTravelDate().equals(request.getTravelDate())) {
                    log.info("normalReservationTickets:{}", normalTickets);
                    if (normalReservationTickets.getStationName().equals(request.getFromStationName())
                            && normalReservationTickets.getCoachName().equals(request.getCoachName())) {
                        log.info("If (From Station and TravelDay found):");
                        for (NormalReservationTickets reservationTickets : ticketsList) {
                            if (reservationTickets.getStationName().equals(request.getToStationName())) {
                                log.info("If (To Station found):");
                                normalTickets = normalReservationTickets;
                                log.info("Train Found:{}", normalTickets);
                            }
                        }
                    }
                }
            }
            if (normalTickets != null) {
                double totalTicketAmount = 0;
                if (request.getNumberOfTickets() <= normalTickets.getNoOfSeatsAvailable()) {
                    log.info("Tickets Are Available");
                    totalTicketAmount = calculateTotalAmount(request.getBookingMethod(), normalTickets.getEachSeatPrice(),
                            request.getNumberOfTickets(), request.getCoachName());
                    if (totalTicketAmount > 0.0) {
                        PaymentResponse response = bookingServiceToPaymentService.bookNormalTicket(normalTickets, request, totalTicketAmount).getBody();
                        String result = response.getPaymentStatus();
                        log.info("Payment Result in NormalReservationService:{}", result);
                        if (result.equalsIgnoreCase("Payment Success")) {
                            addTicketsToAnotherStations(ticketsList, normalTickets.getTravelDate(), request.getToStationName(), request.getCoachName(), request.getNumberOfTickets());
                            int noOfTickets = request.getNumberOfTickets();
                            normalTickets.setNoOfSeatsAvailable(normalTickets.getNoOfSeatsAvailable() - noOfTickets);
                            normalTickets.setNoOfSeatsBooked(normalTickets.getNoOfSeatsBooked() + noOfTickets);
                            bookedTicketsService.addTickets(request, BookingStatus.CONFIRMED, "NO", totalTicketAmount, totalTicketAmount, response.getTransactionID());
                            return new ResponseEntity<>("Ticket Booked Successfully", HttpStatus.OK);
                        }
                    } else {
                        log.info("Booking Cancelled");
                        return new ResponseEntity<>("Booking Cancelled", HttpStatus.BAD_REQUEST);
                    }
                } else {
                    System.out.println("Tickets are Insufficient We Can Confirm Onces Tickets Available");
                    System.out.print("Do you Want to proceed:Y/N:");
                    Scanner scanner = new Scanner(System.in);
                    String yesOrno = scanner.nextLine();
                    if (yesOrno.equalsIgnoreCase("y")) {
                        totalTicketAmount = calculateTotalAmount(request.getBookingMethod(), normalTickets.getEachSeatPrice(),
                                request.getNumberOfTickets(), request.getCoachName());
                        if (totalTicketAmount > 0.0) {
                            PaymentResponse response = bookingServiceToPaymentService.bookNormalTicket(normalTickets, request, totalTicketAmount).getBody();
                            String result = response.getPaymentStatus();
                            log.info("Payment Result in NormalReservationService:{}", result);
                            if (result.equalsIgnoreCase("Payment Success")) {
                                bookedTicketsService.addTickets(request, BookingStatus.WAITING, "null", totalTicketAmount, totalTicketAmount, "");
                                return ResponseEntity.ok("Ticket Booked Successfully In the WAITING List");
                            } else {
                                return ResponseEntity.badRequest().body(result);
                            }
                        } else {
                            return ResponseEntity.ok("Booking Cancelled");
                        }
                    } else {
                        return ResponseEntity.ok("Booking Cancelled");
                    }
                }
            } else {
                log.info("Train fromStation or Destination Station or Travel Date Not found!");
                return new ResponseEntity<>("Destination Station Not found!", HttpStatus.BAD_REQUEST);

            }
        } else {
            log.info("Train Date or Starting Station Not Found");
            return new ResponseEntity<>("Train Date or Starting Station Not Found", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("", HttpStatus.OK);
    }


    private double calculateTotalAmount(String bookingMethod, Double eachSeatPrice, int numberOfTickets, String coachName) {
        log.info("Request in calculateTotalAmount Method in NormalReservationService");
        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName);
        Scanner scanner = new Scanner(System.in);
        if (bookingMethod.equalsIgnoreCase("Normal Reservation")) {
            double totalTicketPrice = (ticketPrice.getPrice() + eachSeatPrice) * numberOfTickets;
            System.out.println("Total TicketsPrice = " + totalTicketPrice);
            System.out.print("Do yo want to place booking Y/N.?:");
            String yesOrno = scanner.nextLine();
            if (yesOrno.equalsIgnoreCase("y")) {
                return totalTicketPrice;
            }
        }
        return 0.0;
    }

    private void addTicketsToAnotherStations(List<NormalReservationTickets> normalReservationTicketsList, LocalDate travelDate, String toStationName, String coachName, Integer numberOfTickets) {
        log.info("Adding Tickets To Others Station:{},{},{}", toStationName, coachName, numberOfTickets);
        NormalReservationTickets normalReservationTickets = null;
        for (NormalReservationTickets reservationTickets : normalReservationTicketsList) {
            if (reservationTickets.getTravelDate().equals(travelDate)
                    && reservationTickets.getStationName().equalsIgnoreCase(toStationName)
                    && reservationTickets.getCoachName().equalsIgnoreCase(coachName)) {
                normalReservationTickets = reservationTickets;
            }
        }
        normalReservationTickets.setNoOfSeatsAvailable(normalReservationTickets.getNoOfSeatsAvailable() + numberOfTickets);
        log.info("Tickets Successfully Added to Station:{}", toStationName);
    }
}
