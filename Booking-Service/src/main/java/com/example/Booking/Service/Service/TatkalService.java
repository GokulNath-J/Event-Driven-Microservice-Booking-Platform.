package com.example.Booking.Service.Service;

import com.example.Booking.Service.DTO.*;
import com.example.Booking.Service.Entity.*;
import com.example.Booking.Service.ExceptionHandlerPackage.PaymentFailedException;
import com.example.Booking.Service.Repository.BookingRequestTableRepo;
import com.example.Booking.Service.Repository.CalculatedAmountRepo;
import com.example.Booking.Service.Repository.TatkalRepo;
import com.example.Booking.Service.Repository.TicketPriceRepo;

import jakarta.persistence.EntityManager;
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
public class TatkalService {

    private final static Logger log = LoggerFactory.getLogger(TatkalService.class);

    private static final LocalTime tatkal_opens_at_for_nonsleepers = LocalTime.of(11, 00, 00);

    private static final LocalTime tatkal_opens_at_for_sleepers = LocalTime.of(10, 00, 00);

    private TatkalRepo tatkalRepo;

    private TicketPriceRepo ticketPriceRepo;

    private BookingServiceToPaymentService bookingServiceToPaymentService;

    private BookedTicketsService bookedTicketsService;

    private CalculatedAmountRepo calculatedAmountRepo;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BookingRequestTableRepo requestTableRepo;

    public TatkalService(TatkalRepo tatkalRepo, TicketPriceRepo ticketPriceRepo,
                         BookingServiceToPaymentService bookingServiceToPaymentService,
                         BookedTicketsService bookedTicketsService, CalculatedAmountRepo calculatedAmountRepo) {
        this.tatkalRepo = tatkalRepo;
        this.ticketPriceRepo = ticketPriceRepo;
        this.bookingServiceToPaymentService = bookingServiceToPaymentService;
        this.bookedTicketsService = bookedTicketsService;
        this.calculatedAmountRepo = calculatedAmountRepo;
    }


    //    @Transactional
//    public void checkEntity(TatkalTickets ticket) {
//        boolean isManaged = entityManager.contains(ticket);
//        System.out.println("Is entity managed? " + isManaged);
//    }


    public ResponseEntity<TicketsResponse> book(BookingRequest request, ConfirmOrCancelRequest confirmOrCancelRequest,
                                                BookingRequestTable requestTable) throws PaymentFailedException {
        log.info("Request in Book Method");
        ResponseEntity<TicketsResponse> response = null;
        String coach = null;
        if (confirmOrCancelRequest == null) {
            coach = request.getCoachName();
        } else {
            coach = requestTable.getCoachName();
        }
        if (coach.equals("Sleeper") || coach.equals("2S")
                && (LocalTime.now().equals(tatkal_opens_at_for_nonsleepers)
                || (LocalTime.now().equals(tatkal_opens_at_for_nonsleepers)))) {
            if (confirmOrCancelRequest == null) {
                response = bookProcess1(request, requestTable);
                log.info("ResponseEntity in Book Method:{}", response);
            } else {
                bookprocess2(confirmOrCancelRequest, requestTable);
            }
            return response;
        } else if (LocalTime.now().equals(tatkal_opens_at_for_sleepers)
                || LocalTime.now().isAfter(tatkal_opens_at_for_sleepers)) {
            if (confirmOrCancelRequest == null) {
                response = bookProcess1(request, requestTable);
                log.info("ResponseEntity in Book Method:{}", response);
            } else {
                bookprocess2(confirmOrCancelRequest, requestTable);
            }
            return response;
        } else {
            throw new RuntimeException("Tatkal Not Yet Openned");
        }
    }

    @Transactional
    private ResponseEntity<TicketsResponse> bookProcess1(BookingRequest request, BookingRequestTable requestTable) throws PaymentFailedException {
        TatkalTickets tickets = findTrain(request, requestTable);
        String id = null;
        double totalTicketAmount = 0;
        Integer amountID = null;
        totalTicketAmount = calculateTotalAmount(request.getBookingMethod(), tickets.getEachSeatPrice(),
                request.getNumberOfTickets(), request.getCoachName());
        id = (request.getUserId() + UUID.randomUUID().toString().substring(0, 10).replace("-", ""));
        if (request.getNumberOfTickets() <= tickets.getNoOfSeatsAvailable()) {
            log.info("Tickets Are Available");
            TicketsResponse response = totalAmountRequestTableGenerator(id, totalTicketAmount, request, BookingStatus.YES,
                    tickets.getEachSeatPrice(), totalTicketAmount);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } else {
            log.info("Tickets Are Not Available");
            throw new RuntimeException("Tickets Are Not Available");
        }
    }

    private TatkalTickets findTrain(BookingRequest request, BookingRequestTable requestTable) {
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
        log.info("Request in Tatkal bookingprocess Service");
        log.info("BookingRequest Before Travel Date Check:{}", request);
        if (travelDate.equals(LocalDate.now().plusDays(1))) {
            log.info("getTravelDate() is Matched:");
            List<TatkalTickets> tatkalTickets = tatkalRepo.findAllByTrainNumber(trainNumber);
            log.info("TatkalTickets in TatkalService:{}", tatkalTickets);
            TatkalTickets tickets = new TatkalTickets();
            if (tatkalTickets != null) {
                for (TatkalTickets tatkalTicket : tatkalTickets) {
                    log.info("Inside For-each Loop");
                    if (tatkalTicket.getStationName().equalsIgnoreCase(fromStationName)
                            && tatkalTicket.getCoachName().equalsIgnoreCase(coachName)) {
                        for (TatkalTickets tatkalTicket1 : tatkalTickets) {
                            if (tatkalTicket1.getStationName().equals(toStationName)) {
                                log.info("Tatkel Ticket Found {}", true);
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

    @Transactional
    private void bookprocess2(ConfirmOrCancelRequest confirmOrCancelRequest, BookingRequestTable requestTable) throws PaymentFailedException {
        PaymentResponse response = bookingServiceToPaymentService.bookTicket(confirmOrCancelRequest).getBody();
        verifyPaymentAndBookTickets(response, requestTable);
//                    return new ResponseEntity<>(new TicketsResponse(), HttpStatus.ACCEPTED);
    }

    private void verifyPaymentAndBookTickets(PaymentResponse response,
                                             BookingRequestTable requestTable) {
        TatkalTickets tickets = findTrain(null, requestTable);
        if (tickets == null) {
            throw new RuntimeException("Train Not Found");
        }
        if (!(requestTable.getNumberOfTickets() <= tickets.getNoOfSeatsAvailable())) {
            throw new RuntimeException("Tickets Not Available");
        }
        List<TatkalTickets> tatkalTickets = tatkalRepo.findAllByTrainNumber(tickets.getTrainNumber());
        if (response.getPaymentStatus().equals(TransactionStatus.Success)) {
            int eachTticketPrice = calculateEachTicketPrice(requestTable.getBookingMethod(), requestTable.getCoachName());
            addTicketsToAnotherStations(tatkalTickets, requestTable.getToStationName(), requestTable.getCoachName(),
                    requestTable.getNumberOfTickets());
//            tickets.setNoOfSeatsBooked(tickets.getNoOfSeatsBooked() + requestTable.getNumberOfTickets());
            int noOfTickets = requestTable.getNumberOfTickets();
            tickets.setNoOfSeatsBooked(tickets.getNoOfSeatsBooked() + noOfTickets);
            tickets.setNoOfSeatsAvailable(tickets.getNoOfSeatsAvailable() - noOfTickets);
            bookedTicketsService.addTickets(requestTable, BookingStatus.CONFIRMED, "NO",
                    requestTable.getTotalTicketsPrice(), tickets.getEachSeatPrice() + eachTticketPrice,
                    response.getTransactionID());
        }
    }

    private double calculateTotalAmount(String bookingMethod, Double eachSeatPrice, int numberOfTickets,
                                        String coachName) {
        log.info("Request in calculateTotalAmount Method in TatkalService");
        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName).orElseThrow(() -> new RuntimeException("Tickt Price is Not Found"));
        double totalTicketPrice = 0.0;
        if ((ticketPrice != null) && bookingMethod.equalsIgnoreCase("Tatkal")) {
            totalTicketPrice = (ticketPrice.getPrice() + eachSeatPrice) * numberOfTickets;
        }
        return totalTicketPrice;
    }

    private TicketsResponse totalAmountRequestTableGenerator(String id, double totalTicketAmount, BookingRequest request,
                                                             BookingStatus status, Double eachSeatPrice, double ticketAmount) {
        List<PassengerDetailsDTO> passengerDetailsListDto = request.getPassengersList();
        List<TemporaryPassengerDetails> passengerDetails = new ArrayList<>();
        for (PassengerDetailsDTO dto : passengerDetailsListDto) {
            TemporaryPassengerDetails details = new TemporaryPassengerDetails
                    (request.getUserId(), dto.getPassengerName(), dto.getGender(), dto.getAge());
            passengerDetails.add(details);
            CalculatedAmount amount = CalculatedAmount.builder()
                    .id(id)
                    .bookingMethod(request.getBookingMethod())
                    .trainNumber(request.getTrainNumber())
                    .fromStationName(request.getFromStationName())
                    .toStationName(request.getToStationName())
                    .travelDate(request.getTravelDate())
                    .coachName(request.getCoachName())
                    .userId(request.getUserId())
                    .totalAmount(totalTicketAmount)
                    .passengerName(details.getPassengerName())
                    .build();
            calculatedAmountRepo.save(amount);
        }
        BookingRequestTable requestTable = BookingRequestTable.builder()
                .userId(request.getUserId())
                .bookingMethod(request.getBookingMethod())
                .coachName(request.getCoachName())
                .fromStationName(request.getFromStationName())
                .numberOfTickets(request.getNumberOfTickets())
                .toStationName(request.getToStationName())
                .trainNumber(request.getTrainNumber())
                .travelDate(request.getTravelDate())
                .temporaryPassengerDetailsList(passengerDetails)
                .calculatedAmountId(id)
                .eachSeatPrice(eachSeatPrice)
                .totalTicketsPrice(ticketAmount)
                .bookingStatus(BookingStatus.WAITING)
                .ticketAvailability(status)
                .build();
        requestTableRepo.save(requestTable);
        TicketsResponse response = TicketsResponse.builder()
                .userId(request.getUserId())
                .bookingMethod(request.getBookingMethod())
                .coachName(request.getCoachName())
                .fromStationName(request.getFromStationName())
                .numberOfTickets(request.getNumberOfTickets())
                .toStationName(request.getToStationName())
                .trainNumber(request.getTrainNumber())
                .travelDate(request.getTravelDate())
                .passengersList(request.getPassengersList())
                .calculatedAmountId(id)
                .ticketAvailability(status)
                .eachSeatPrice(eachSeatPrice)
                .totalTicketsPrice(ticketAmount)
                .build();
        return response;
    }

    private int calculateEachTicketPrice(String bookingMethod, String coachName) {
        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName).orElseThrow(() -> new RuntimeException("Coach Not Found"));
        return ticketPrice.getPrice();
    }

    private void addTicketsToAnotherStations(List<TatkalTickets> tatkalTickets, String toStationName, String
            coachName, Integer numberOfTickets) {
        log.info("Adding Tickets To Others Station:{},{},{}", toStationName, coachName, numberOfTickets);
        TatkalTickets tickets = null;
        for (TatkalTickets tatkalTicket : tatkalTickets) {
            if (tatkalTicket.getStationName().equalsIgnoreCase(toStationName)
                    && tatkalTicket.getCoachName().equalsIgnoreCase(coachName)) {
                tickets = tatkalTicket;
            }
        }
        tickets.setNoOfSeatsAvailable(tickets.getNoOfSeatsAvailable() + numberOfTickets);
        log.info("Tickets Successfully Added to Station:{}", toStationName);
    }
}
//    @Transactional
//    public ResponseEntity<String> bookProcess(BookingRequest request) throws PaymentFailedException {
//        log.info("Request in Tatkal bookingprocess Service");
//        log.info("BookingRequest Before Travel Date Check:{}", request);
//        if (request.getTravelDate().equals(LocalDate.now().plusDays(1))) {
//            log.info("getTravelDate() is Matched:");
//            List<TatkalTickets> tatkalTickets = tatkalRepo.findAllByTrainNumber(request.getTrainNumber());
//            log.info("TatkalTickets in TatkalService:{}", tatkalTickets);
//            TatkalTickets tickets = new TatkalTickets();
//            for (TatkalTickets tatkalTicket : tatkalTickets) {
//                log.info("Inside For-each Loop");
//                if (tatkalTicket.getStationName().equalsIgnoreCase(request.getFromStationName())
//                        && tatkalTicket.getCoachName().equalsIgnoreCase(request.getCoachName())) {
//                    for (TatkalTickets tatkalTicket1 : tatkalTickets) {
//                        if (tatkalTicket1.getStationName().equals(request.getToStationName())) {
//                            log.info("Tatkel Ticket Found {}", true);
//                            tickets = tatkalTicket;
//                        }
//                    }
//                }
//            }
//            log.info("TatkalTickets:{}", tickets);
//            // checkEntity(tickets);
//            double totalTicketAmount = 0;
//            if (request.getNumberOfTickets() <= tickets.getNoOfSeatsAvailable()) {
//                log.info("Tickets Are Available");
//                totalTicketAmount = calculateTotalAmount(request.getBookingMethod(), tickets.getEachSeatPrice(),
//                        request.getNumberOfTickets(), request.getCoachName());
//                int eachTticketPrice = calculateEachTicketPrice(request.getBookingMethod(), request.getCoachName());
//                if (totalTicketAmount > 0.0) {
//                    PaymentResponse response = bookingServiceToPaymentService.bookTatkalTicket(tickets, request, totalTicketAmount).getBody();
//                    String result = response.getPaymentStatus();
//                    log.info("Payment Result in TatkalService:{}", result);
//                    if (result.equalsIgnoreCase("Payment Success")) {
//                        addTicketsToAnotherStations(tatkalTickets, request.getToStationName(), request.getCoachName(), request.getNumberOfTickets());
//                        //    tickets.setNoOfSeatsBooked(tickets.getNoOfSeatsBooked() + request.getNumberOfTickets());
//                        int noOfTickets = request.getNumberOfTickets();
//                        tickets.setNoOfSeatsBooked(tickets.getNoOfSeatsBooked() + noOfTickets);
//                        tickets.setNoOfSeatsAvailable(tickets.getNoOfSeatsAvailable() - noOfTickets);
//                        bookedTicketsService.addTickets(request, BookingStatus.CONFIRMED, "NO", totalTicketAmount, tickets.getEachSeatPrice() + eachTticketPrice, response.getTransactionID());
//                        return new ResponseEntity<>("Ticket Booked Successfully", HttpStatus.OK);
//                    } else {
//                        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
//                    }
//                } else {
//                    return ResponseEntity.ok("Booking Cancelled");
//                }
//            } else {
//                System.out.println("Tickets are Insufficient We Can Confirm Onces Tickets Available");
//                System.out.print("Do you Want to proceed:Y/N:");
//                Scanner scanner = new Scanner(System.in);
//                String yesOrno = scanner.nextLine();
//                if (yesOrno.equalsIgnoreCase("y")) {
//                    totalTicketAmount = calculateTotalAmount(request.getBookingMethod(), tickets.getEachSeatPrice(),
//                            request.getNumberOfTickets(), request.getCoachName());
//                    int eachTticketPrice = calculateEachTicketPrice(request.getBookingMethod(), request.getCoachName());
//                    if (totalTicketAmount > 0.0) {
//                        PaymentResponse response = bookingServiceToPaymentService.bookTatkalTicket(tickets, request, totalTicketAmount).getBody();
//                        String result = response.getPaymentStatus();
//                        log.info("Payment Result in TatkalService:{}", result);
//                        if (result.equalsIgnoreCase("Payment Success")) {
//                            bookedTicketsService.addTickets(request, BookingStatus.WAITING, "null", totalTicketAmount, tickets.getEachSeatPrice() + eachTticketPrice, response.getTransactionID());
//                            return ResponseEntity.ok("Ticket Booked Successfully In the WAITING List");
//                        } else {
//                            return ResponseEntity.badRequest().body(result);
//                        }
//                    } else {
//                        return ResponseEntity.ok("Booking Cancelled");
//                    }
//                } else {
//                    return ResponseEntity.ok("Booking Cancelled");
//                }
//            }
//        }
//        return ResponseEntity.ok("There is Not Train on this Date");
//    }
//
//    private double calculateTotalAmount(String bookingMethod, Double eachSeatPrice, int numberOfTickets, String coachName) {
//        log.info("Request in calculateTotalAmount Method in TatkalService");
//        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName);
//        Scanner scanner = new Scanner(System.in);
//        if (bookingMethod.equalsIgnoreCase("Tatkal")) {
//            double totalTicketPrice = (ticketPrice.getPrice() + eachSeatPrice) * numberOfTickets;
//            System.out.println("Total Tatakl TicketsPrice = " + totalTicketPrice);
//            System.out.print("Do yo want to place booking Y/N.?:");
//            String yesOrno = scanner.nextLine();
//            if (yesOrno.equalsIgnoreCase("y")) {
//                return totalTicketPrice;
//            }
//        }
//        return 0.0;
//    }
//
//    private int calculateEachTicketPrice(String bookingMethod, String coachName) {
//        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName);
//        return ticketPrice.getPrice();
//    }

//@Transactional
//private ResponseEntity<TicketsResponse> bookProcess1(BookingRequest request, ConfirmOrCancelRequest confirmOrCancelRequest,
//                                                     BookingRequestTable requestTable) throws PaymentFailedException {
//    log.info("Request in Tatkal bookingprocess Service");
//    log.info("BookingRequest Before Travel Date Check:{}", request);
//    String id = null;
//    LocalDate travelDate = null;
//    if (request == null) {
//        travelDate = requestTable.getTravelDate();
//    } else {
//        travelDate = request.getTravelDate();
//    }
//    if (travelDate.equals(LocalDate.now().plusDays(1))) {
//        log.info("getTravelDate() is Matched:");
//        List<TatkalTickets> tatkalTickets = tatkalRepo.findAllByTrainNumber(request.getTrainNumber());
//        log.info("TatkalTickets in TatkalService:{}", tatkalTickets);
//        TatkalTickets tickets = new TatkalTickets();
//        if (tatkalTickets != null) {
//            for (TatkalTickets tatkalTicket : tatkalTickets) {
//                log.info("Inside For-each Loop");
//                if (tatkalTicket.getStationName().equalsIgnoreCase(request.getFromStationName())
//                        && tatkalTicket.getCoachName().equalsIgnoreCase(request.getCoachName())) {
//                    for (TatkalTickets tatkalTicket1 : tatkalTickets) {
//                        if (tatkalTicket1.getStationName().equals(request.getToStationName())) {
//                            log.info("Tatkel Ticket Found {}", true);
//                            tickets = tatkalTicket;
//                        }
//                    }
//                }
//            }
//            log.info("TatkalTickets:{}", tickets);
//            // checkEntity(tickets);
//            double totalTicketAmount = 0;
//            Integer amountID = null;
//            totalTicketAmount = calculateTotalAmount(request.getBookingMethod(), tickets.getEachSeatPrice(),
//                    request.getNumberOfTickets(), request.getCoachName());
//            id = (request.getUserId() + UUID.randomUUID().toString().substring(0, 10).replace("-", ""));
//            if (request.getNumberOfTickets() <= tickets.getNoOfSeatsAvailable()) {
//                log.info("Tickets Are Available");
//                if (confirmOrCancelRequest == null) {
//                    TicketsResponse response = totalAmountRequestTableGenerator(id, totalTicketAmount, request, BookingStatus.YES,
//                            tickets.getEachSeatPrice(), totalTicketAmount);
//                    return new ResponseEntity<>(response, HttpStatus.CREATED);
//                } else {
//                    PaymentResponse response = bookingServiceToPaymentService.bookTicket(confirmOrCancelRequest).getBody();
//                    verifyPaymentAndBookTickets(tatkalTickets, tickets, request, response, requestTable);
//                    return new ResponseEntity<>(new TicketsResponse(), HttpStatus.ACCEPTED);
//                }
//            } else {
//                log.info("Tickets Are Not Available");
//                throw new RuntimeException("Tickets Are Not Available");
//            }
//        } else {
//            throw new RuntimeException("Train Not Found");
//        }
//    } else {
//        throw new RuntimeException("Train Not Found On This Date");
//    }
//}
//
