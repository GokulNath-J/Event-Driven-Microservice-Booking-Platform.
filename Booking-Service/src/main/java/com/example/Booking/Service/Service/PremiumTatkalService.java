package com.example.Booking.Service.Service;

import com.example.Booking.Service.DTO.*;
import com.example.Booking.Service.Entity.*;
import com.example.Booking.Service.ExceptionHandlerPackage.PaymentFailedException;
import com.example.Booking.Service.Repository.BookingRequestTableRepo;
import com.example.Booking.Service.Repository.CalculatedAmountRepo;
import com.example.Booking.Service.Repository.PremiumTatkalRepo;
import com.example.Booking.Service.Repository.TicketPriceRepo;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PremiumTatkalService {

    private final static Logger log = LoggerFactory.getLogger(PremiumTatkalService.class);

    private static final LocalTime Premiumtatkal_opens_at_for_nonsleepers = LocalTime.of(11, 00, 00);
    private static final LocalTime Premiumtatkal_opens_at_for_sleepers = LocalTime.of(10, 00, 00);


    @Autowired
    private PremiumTatkalRepo premiumTatkalRepo;

    @Autowired
    private TicketPriceRepo ticketPriceRepo;

    @Autowired
    private BookingServiceToPaymentService bookingServiceToPaymentService;

    @Autowired
    private BookedTicketsService bookedTicketsService;

    @Autowired
    private BookingRequestTableRepo requestTableRepo;

    @Autowired
    private CalculatedAmountRepo calculatedAmountRepo;

    public ResponseEntity<TicketsResponse> book(BookingRequest request, ConfirmOrCancelRequest confirmOrCancelRequest, BookingRequestTable requestTable) throws PaymentFailedException {
        log.info("Request in Book Method");
        ResponseEntity<TicketsResponse> response = null;
        String coach = null;
        if (confirmOrCancelRequest == null) {
            coach = request.getCoachName();
        } else {
            coach = requestTable.getCoachName();
        }
        if (coach.equals("Sleeper") || coach.equals("2S")
                && (LocalTime.now().equals(Premiumtatkal_opens_at_for_nonsleepers)
                || (LocalTime.now().equals(Premiumtatkal_opens_at_for_nonsleepers)))) {
            if (confirmOrCancelRequest == null) {
                response = bookProcess1(request, requestTable);
                log.info("ResponseEntity in Book Method:{}", response);
            } else {
                bookprocess2(confirmOrCancelRequest, requestTable);
            }
            return response;
        } else if (LocalTime.now().equals(Premiumtatkal_opens_at_for_sleepers)
                || LocalTime.now().isAfter(Premiumtatkal_opens_at_for_sleepers)) {
            if (confirmOrCancelRequest == null) {
                response = bookProcess1(request, requestTable);
                log.info("ResponseEntity in Book Method:{}", response);
            } else {
                bookprocess2(confirmOrCancelRequest, requestTable);
            }
            return response;
        } else {
            throw new RuntimeException("Premium Tatkal Not Yet Openned");
        }
    }

    @Transactional
    private ResponseEntity<TicketsResponse> bookProcess1(BookingRequest request, BookingRequestTable requestTable) throws PaymentFailedException {
        log.info("Request in bookProcess1 Method");
        PremiumTatkalTickets tickets = findTrain(request, requestTable);
        String id = null;
        double totalTicketAmount = 0;
        Integer amountID = null;
        totalTicketAmount = calculateTotalAmount(request.getBookingMethod(), tickets.getEachSeatPrice(), request.getNumberOfTickets(), request.getCoachName());
        id = (request.getUserId() + UUID.randomUUID().toString().substring(0, 10).replace("-", ""));
        if (request.getNumberOfTickets() <= tickets.getNoOfSeatsAvailable()) {
            log.info("Tickets Are Available");
            TicketsResponse response = totalAmountRequestTableGenerator(id, totalTicketAmount, request, BookingStatus.YES, tickets.getEachSeatPrice(), totalTicketAmount);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } else {
            log.info("Tickets Are Not Available");
            throw new RuntimeException("Tickets Are Not Available");
        }
    }

    private PremiumTatkalTickets findTrain(BookingRequest request, BookingRequestTable requestTable) {
        log.info("Request in findTrain Method");
        Integer trainNumber;
        LocalDate travelDate;
        String coachName;
        String fromStationName;
        String toStationName;
        if (request != null) {
            trainNumber = request.getTrainNumber();
            travelDate = request.getTravelDate();
            coachName = request.getCoachName();
            fromStationName = request.getFromStationName();
            toStationName = request.getToStationName();
        } else {
            trainNumber = requestTable.getTrainNumber();
            travelDate = requestTable.getTravelDate();
            coachName = requestTable.getCoachName();
            fromStationName = requestTable.getFromStationName();
            toStationName = requestTable.getToStationName();
        }
        log.info("Request in PremiumTatkalTickets bookingprocess Service");
        log.info("BookingRequest Before Travel Date Check:{}", request);
        if (travelDate.equals(LocalDate.now().plusDays(1))) {
            log.info("getTravelDate() is Matched:");
            List<PremiumTatkalTickets> tatkalTickets = premiumTatkalRepo.findAllByTrainNumber(trainNumber);
            log.info("Premium TatkalTickets in TatkalService:{}", tatkalTickets);
            PremiumTatkalTickets tickets = new PremiumTatkalTickets();
            if (tatkalTickets != null) {
                for (PremiumTatkalTickets tatkalTicket : tatkalTickets) {
                    log.info("Inside For-each Loop");
                    if (tatkalTicket.getStationName().equalsIgnoreCase(fromStationName) && tatkalTicket.getCoachName().equalsIgnoreCase(coachName)) {
                        for (PremiumTatkalTickets tatkalTicket1 : tatkalTickets) {
                            if (tatkalTicket1.getStationName().equals(toStationName)) {
                                log.info("PremiumTatkal Ticket Found {}", true);
                                tickets = tatkalTicket;
                                return tickets;
                            }
                        }
                    }
                }
            } else {
                throw new RuntimeException("Train Not Found");
            }
        } else {
            throw new RuntimeException("Travel Date Not Found!");
        }
        return null;
    }

    private void bookprocess2(ConfirmOrCancelRequest confirmOrCancelRequest, BookingRequestTable requestTable) throws PaymentFailedException {
        log.info("Request in bookProcess2 Method");
        PaymentResponse response = bookingServiceToPaymentService.bookTicket(confirmOrCancelRequest).getBody();
        verifyPaymentAndBookTickets(response, requestTable);
//                    return new ResponseEntity<>(new TicketsResponse(), HttpStatus.ACCEPTED);
    }

    private void verifyPaymentAndBookTickets(PaymentResponse response, BookingRequestTable requestTable) {
        log.info("Request in verifyPaymentAndBookTickets Method");
        PremiumTatkalTickets tickets = findTrain(null, requestTable);
        if (tickets == null) {
            throw new RuntimeException("Train Not Found");
        }
        if (!(requestTable.getNumberOfTickets() <= tickets.getNoOfSeatsAvailable())) {
            throw new RuntimeException("Tickets Not Available");
        }
        List<PremiumTatkalTickets> tatkalTickets = premiumTatkalRepo.findAllByTrainNumber(tickets.getTrainNumber());
        if (response.getPaymentStatus().equals(TransactionStatus.Success)) {
            int eachTticketPrice = calculateEachTicketPrice(requestTable.getBookingMethod(), requestTable.getCoachName());
            addTicketsToAnotherStations(tatkalTickets, requestTable.getToStationName(), requestTable.getCoachName(), requestTable.getNumberOfTickets());
            int noOfTickets = requestTable.getNumberOfTickets();
            tickets.setNoOfSeatsBooked(tickets.getNoOfSeatsBooked() + noOfTickets);
            tickets.setNoOfSeatsAvailable(tickets.getNoOfSeatsAvailable() - noOfTickets);
            bookedTicketsService.addTickets(requestTable, BookingStatus.CONFIRMED, "NO", requestTable.getTotalTicketsPrice(), tickets.getEachSeatPrice() + eachTticketPrice, response.getTransactionID());
            increasePremiumTatkalTicketPrice(requestTable.getBookingMethod(), requestTable.getCoachName());
        }
    }

    @Transactional
    private void increasePremiumTatkalTicketPrice(String bookingMethod, String coachName) {
        log.info("Request in verifyPaymentAndBookTickets Method");
        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName).orElseThrow(() -> new RuntimeException("Tickt Price is Not Found"));
        ticketPrice.setPrice(ticketPrice.getPrice() + 10);
    }


    private double calculateTotalAmount(String bookingMethod, Double eachSeatPrice, int numberOfTickets, String coachName) {
        log.info("Request in calculateTotalAmount Method in Premium TatkalService");
        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName).orElseThrow(() -> new RuntimeException("Tickt Price is Not Found"));
        double totalTicketPrice = 0.0;
        if ((ticketPrice != null) && bookingMethod.equalsIgnoreCase("Premium Tatkal")) {
            totalTicketPrice = (ticketPrice.getPrice() + eachSeatPrice) * numberOfTickets;
        }
        return totalTicketPrice;
    }

    private TicketsResponse totalAmountRequestTableGenerator(String id, double totalTicketAmount, BookingRequest request, BookingStatus status, Double eachSeatPrice, double ticketAmount) {
        log.info("Request in verifyPaymentAndBookTickets Method");
        List<PassengerDetailsDTO> passengerDetailsListDto = request.getPassengersList();
        List<TemporaryPassengerDetails> passengerDetails = new ArrayList<>();
        for (PassengerDetailsDTO dto : passengerDetailsListDto) {
            TemporaryPassengerDetails details = new TemporaryPassengerDetails(request.getUserId(), dto.getPassengerName(), dto.getGender(), dto.getAge());
            passengerDetails.add(details);
            CalculatedAmount amount = CalculatedAmount.builder().id(id).bookingMethod(request.getBookingMethod()).trainNumber(request.getTrainNumber()).fromStationName(request.getFromStationName()).toStationName(request.getToStationName()).travelDate(request.getTravelDate()).coachName(request.getCoachName()).userId(request.getUserId()).totalAmount(totalTicketAmount).passengerName(details.getPassengerName()).build();
            calculatedAmountRepo.save(amount);
        }
        BookingRequestTable requestTable = BookingRequestTable.builder().userId(request.getUserId()).bookingMethod(request.getBookingMethod()).coachName(request.getCoachName()).fromStationName(request.getFromStationName()).numberOfTickets(request.getNumberOfTickets()).toStationName(request.getToStationName()).trainNumber(request.getTrainNumber()).travelDate(request.getTravelDate()).temporaryPassengerDetailsList(passengerDetails).calculatedAmountId(id).eachSeatPrice(eachSeatPrice).totalTicketsPrice(ticketAmount).bookingStatus(BookingStatus.WAITING).ticketAvailability(status).build();
        requestTableRepo.save(requestTable);
        TicketsResponse response = TicketsResponse.builder().userId(request.getUserId()).bookingMethod(request.getBookingMethod()).coachName(request.getCoachName()).fromStationName(request.getFromStationName()).numberOfTickets(request.getNumberOfTickets()).toStationName(request.getToStationName()).trainNumber(request.getTrainNumber()).travelDate(request.getTravelDate()).passengersList(request.getPassengersList()).calculatedAmountId(id).ticketAvailability(status).eachSeatPrice(eachSeatPrice).totalTicketsPrice(ticketAmount).build();
        return response;
    }

    private int calculateEachTicketPrice(String bookingMethod, String coachName) {
        log.info("Request in verifyPaymentAndBookTickets Method");
        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName).orElseThrow(() -> new RuntimeException("Coach Not Found"));
        return ticketPrice.getPrice();
    }

    private void addTicketsToAnotherStations(List<PremiumTatkalTickets> tatkalTickets, String toStationName, String coachName, Integer numberOfTickets) {
        log.info("Request in verifyPaymentAndBookTickets Method");
        log.info("Adding Tickets To Others Station:{},{},{}", toStationName, coachName, numberOfTickets);
        PremiumTatkalTickets tickets = null;
        for (PremiumTatkalTickets tatkalTicket : tatkalTickets) {
            if (tatkalTicket.getStationName().equalsIgnoreCase(toStationName) && tatkalTicket.getCoachName().equalsIgnoreCase(coachName)) {
                tickets = tatkalTicket;
            }
        }
        tickets.setNoOfSeatsAvailable(tickets.getNoOfSeatsAvailable() + numberOfTickets);
        log.info("Tickets Successfully Added to Station:{}", toStationName);
    }
}

//    public ResponseEntity<String> book(BookingRequest request) throws PaymentFailedException {
//        if (request.getCoachName().equals("Sleeper") || request.getCoachName().equals("2S")
//                && (LocalTime.now().isAfter(Premiumtatkal_opens_at_for_nonsleepers))
//                || (LocalTime.now().equals(Premiumtatkal_opens_at_for_nonsleepers))) {
//            ResponseEntity<String> response = bookProcess(request);
//            return response;
//        } else if (LocalTime.now().equals(Premiumtatkal_opens_at_for_sleepers)
//                || LocalTime.now().isAfter(Premiumtatkal_opens_at_for_sleepers)) {
//            ResponseEntity<String> response = bookProcess(request);
//            return response;
//        } else {
//            return ResponseEntity.badRequest().body("Premium Tatkal Not Yet Openned");
//        }
//    }
//
//    @Transactional
//    public ResponseEntity<String> bookProcess(BookingRequest request) throws PaymentFailedException {
//        log.info("Request in Premium TatkalService");
//        log.info("BookingRequest Before Travel Date Check:{}", request);
//        if (request.getTravelDate().equals(LocalDate.now().plusDays(1))) {
//            log.info("getTravelDate() is Matched:");
//            List<PremiumTatkalTickets> premiumtatkalTicketslist = premiumTatkalRepo.findAllByTrainNumber(request.getTrainNumber());
//            log.info("TatkalTickets in TatkalService:{}", premiumtatkalTicketslist);
//            PremiumTatkalTickets premiumTatkalTickets = new PremiumTatkalTickets();
//            for (PremiumTatkalTickets premiumTatkalTickets1 : premiumtatkalTicketslist) {
//                log.info("Inside For-each Loop");
//                if (premiumTatkalTickets1.getStationName().equalsIgnoreCase(request.getFromStationName())
//                        && premiumTatkalTickets1.getCoachName().equalsIgnoreCase(request.getCoachName())) {
//                    log.info("Tatkel Ticket Found {}", true);
//                    premiumTatkalTickets = premiumTatkalTickets1;
//                }
//            }
//            log.info("TatkalTickets:{}", premiumTatkalTickets);
//            // checkEntity(tickets);
//            double totalTicketAmount = 0;
//            if (request.getNumberOfTickets() <= premiumTatkalTickets.getNoOfSeatsAvailable()) {
//                log.info("Tickets Are Available");
//                totalTicketAmount = calculateTotalAmount(request.getBookingMethod(), premiumTatkalTickets.getEachSeatPrice(),
//                        request.getNumberOfTickets(), request.getCoachName());
//                if (totalTicketAmount > 0.0) {
//                    PaymentResponse response = bookingServiceToPaymentService.bookPremiumTatkalTicket(premiumTatkalTickets, request, totalTicketAmount).getBody();
//                    String result = response.getPaymentStatus();
//                    log.info("Payment Result in Premium TatkalService:{}", result);
//                    if (result.equalsIgnoreCase("Payment Success")) {
//                        addTicketsToAnotherStations(premiumtatkalTicketslist, request.getToStationName(), request.getCoachName(), request.getNumberOfTickets());
//                        int noOfTickets = request.getNumberOfTickets();
//                        premiumTatkalTickets.setNoOfSeatsAvailable(premiumTatkalTickets.getNoOfSeatsAvailable() - noOfTickets);
//                        premiumTatkalTickets.setNoOfSeatsBooked(premiumTatkalTickets.getNoOfSeatsBooked() + noOfTickets);
//                        bookedTicketsService.addTickets(request, BookingStatus.CONFIRMED, "NO", totalTicketAmount, totalTicketAmount, response.getTransactionID());
//                        return ResponseEntity.ok("Ticket Booked Successfully");
//                    } else {
//                        return ResponseEntity.badRequest().body(result);
//                    }
//                } else {
//                    return ResponseEntity.ok("Booking Cancelled");
//                }
//            } else {
//                return ResponseEntity.badRequest().body("Insufficient Tickets");
//            }
//        }
//        return ResponseEntity.ok("There is Not Train on this Date");
//    }
//
//    private double calculateTotalAmount(String bookingMethod, Double eachSeatPrice, int numberOfTickets, String coachName) {
//        log.info("Request in calculateTotalAmount Method in Premium TatkalService");
//        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName).orElseThrow(() -> new RuntimeException("Tickt Price is Not Found"));
//        ;
//        Scanner scanner = new Scanner(System.in);
//        if (bookingMethod.equalsIgnoreCase("Premium Tatkal")) {
//            double totalTicketPrice = (ticketPrice.getPrice() + eachSeatPrice) * numberOfTickets;
//            System.out.println("Total Premium Tatakl TicketsPrice = " + totalTicketPrice);
//            System.out.print("Do yo want to place booking Y/N.?:");
//            String yesOrno = scanner.nextLine();
//            if (yesOrno.equalsIgnoreCase("y")) {
//                return totalTicketPrice;
//            }
//        }
//        return 0.0;
//    }
//
//    private void addTicketsToAnotherStations(List<PremiumTatkalTickets> premiumTatkalTickets, String toStationName, String coachName, Integer numberOfTickets) {
//        log.info("Adding Tickets To Others Station:{},{},{}", toStationName, coachName, numberOfTickets);
//        PremiumTatkalTickets tickets = null;
//        for (PremiumTatkalTickets premiumTatkalTickets1 : premiumTatkalTickets) {
//            if (premiumTatkalTickets1.getStationName().equalsIgnoreCase(toStationName)
//                    && premiumTatkalTickets1.getCoachName().equalsIgnoreCase(coachName)) {
//                tickets = premiumTatkalTickets1;
//            }
//        }
//        tickets.setNoOfSeatsAvailable(tickets.getNoOfSeatsAvailable() + numberOfTickets);
//        log.info("Tickets Successfully Added to Station:{}", toStationName);
//    }

//
//
//
