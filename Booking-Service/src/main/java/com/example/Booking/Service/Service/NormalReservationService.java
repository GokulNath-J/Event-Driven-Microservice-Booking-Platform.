package com.example.Booking.Service.Service;

import com.example.Booking.Service.DTO.*;
import com.example.Booking.Service.Entity.*;
import com.example.Booking.Service.ExceptionHandlerPackage.PaymentFailedException;
import com.example.Booking.Service.Repository.BookingRequestTableRepo;
import com.example.Booking.Service.Repository.CalculatedAmountRepo;
import com.example.Booking.Service.Repository.NormalReservationRepo;
import com.example.Booking.Service.Repository.TicketPriceRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
public class NormalReservationService {

    private final static Logger log = LoggerFactory.getLogger(NormalReservationService.class);

    private NormalReservationRepo normalReservationRepo;

    private TicketPriceRepo ticketPriceRepo;

    private BookingServiceToPaymentService bookingServiceToPaymentService;

    private BookedTicketsService bookedTicketsService;

    @Autowired
    private BookingRequestTableRepo requestTableRepo;

    @Autowired
    private CalculatedAmountRepo calculatedAmountRepo;

    public NormalReservationService(NormalReservationRepo normalReservationRepo, TicketPriceRepo ticketPriceRepo,
                                    BookingServiceToPaymentService bookingServiceToPaymentService,
                                    BookedTicketsService bookedTicketsService) {
        this.normalReservationRepo = normalReservationRepo;
        this.ticketPriceRepo = ticketPriceRepo;
        this.bookingServiceToPaymentService = bookingServiceToPaymentService;
        this.bookedTicketsService = bookedTicketsService;
    }

    @Transactional
    public ResponseEntity<TicketsResponse> bookProcess1(BookingRequest request, BookingRequestTable requestTable) throws PaymentFailedException {
        log.info("Request in bookProcess1 Method");
        NormalReservationTickets tickets = findTrain(request, requestTable);
        if (tickets.getIsBookingClosed().equals(BookingStatus.YES)) {
            throw new RuntimeException("Booking Closed for this train");
        }
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
            TicketsResponse response = totalAmountRequestTableGenerator(id, totalTicketAmount, request, BookingStatus.NO,
                    tickets.getEachSeatPrice(), totalTicketAmount);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
    }

    private NormalReservationTickets findTrain(BookingRequest request, BookingRequestTable requestTable) {
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
        log.info("Request in normalTickets bookingprocess Service");
        log.info("BookingRequest Before Travel Date Check:{}", request);
        if (travelDate.equals(LocalDate.now().plusDays(1))) {
            log.info("getTravelDate() is Matched:");
            List<NormalReservationTickets> normalTickets = normalReservationRepo.findAllByTrainNumber(trainNumber);
            log.info("Normal Tickets in normal Reservation Service:{}", normalTickets);
            NormalReservationTickets tickets = new NormalReservationTickets();
            if (normalTickets != null) {
                for (NormalReservationTickets normalTicket1 : normalTickets) {
                    log.info("Inside For-each Loop");
                    if (normalTicket1.getStationName().equalsIgnoreCase(fromStationName)
                            && normalTicket1.getCoachName().equalsIgnoreCase(coachName)) {
                        for (NormalReservationTickets tatkalTicket1 : normalTickets) {
                            if (tatkalTicket1.getStationName().equals(toStationName)) {
                                log.info("Normal Ticket Found {}", true);
                                tickets = normalTicket1;
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

    public void bookprocess2(ConfirmOrCancelRequest confirmOrCancelRequest, BookingRequestTable requestTable) throws PaymentFailedException {
        log.info("Request in bookprocess2 Method");
        PaymentResponse response = bookingServiceToPaymentService.bookTicket(confirmOrCancelRequest).getBody();
        verifyPaymentAndBookTickets(response, requestTable);
//                    return new ResponseEntity<>(new TicketsResponse(), HttpStatus.ACCEPTED);
    }

    private void verifyPaymentAndBookTickets(PaymentResponse response,
                                             BookingRequestTable requestTable) {
        log.info("Request in verifyPaymentAndBookTickets Method");
        NormalReservationTickets tickets = findTrain(null, requestTable);
        if (tickets == null) {
            throw new RuntimeException("Train Not Found");
        }
//        if (!(requestTable.getNumberOfTickets() <= tickets.getNoOfSeatsAvailable())) {
//            throw new RuntimeException("Tickets Not Available");
//        }
        List<NormalReservationTickets> normalTickets = normalReservationRepo.findAllByTrainNumber(tickets.getTrainNumber());
        if (response.getPaymentStatus().equals(TransactionStatus.Success)) {
            int eachTicketPrice = calculateEachTicketPrice(requestTable.getBookingMethod(), requestTable.getCoachName());
            if (requestTable.getTicketAvailability().equals(BookingStatus.YES)) {
                addTicketsToAnotherStations(normalTickets, requestTable.getToStationName(), requestTable.getCoachName(),
                        requestTable.getNumberOfTickets());
//            tickets.setNoOfSeatsBooked(tickets.getNoOfSeatsBooked() + requestTable.getNumberOfTickets());
                int noOfTickets = requestTable.getNumberOfTickets();
                tickets.setNoOfSeatsBooked(tickets.getNoOfSeatsBooked() + noOfTickets);
                tickets.setNoOfSeatsAvailable(tickets.getNoOfSeatsAvailable() - noOfTickets);
                bookedTicketsService.addTickets(requestTable, BookingStatus.CONFIRMED, "NO",
                        requestTable.getTotalTicketsPrice(), tickets.getEachSeatPrice() + eachTicketPrice,
                        response.getTransactionID());
            } else {
                bookedTicketsService.addTickets(requestTable, BookingStatus.WAITING, "NO",
                        requestTable.getTotalTicketsPrice(), tickets.getEachSeatPrice() + eachTicketPrice,
                        response.getTransactionID());
            }
        }
    }

    private double calculateTotalAmount(String bookingMethod, Double eachSeatPrice, int numberOfTickets,
                                        String coachName) {
        log.info("Request in calculateTotalAmount Method in NormalReservationService");
        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName).orElseThrow(() -> new RuntimeException("Tickt Price is Not Found"));
        double totalTicketPrice = 0.0;
        if ((ticketPrice != null) && bookingMethod.equalsIgnoreCase("Normal Reservation")) {
            totalTicketPrice = (ticketPrice.getPrice() + eachSeatPrice) * numberOfTickets;
        }
        return totalTicketPrice;
    }

    private TicketsResponse totalAmountRequestTableGenerator(String id, double totalTicketAmount, BookingRequest request,
                                                             BookingStatus status, Double eachSeatPrice, double ticketAmount) {
        log.info("Request in totalAmountRequestTableGenerator Method");
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
        log.info("Request in calculateEachTicketPrice Method");
        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName)
                .orElseThrow(() -> new RuntimeException("Coach Not Found"));
        return ticketPrice.getPrice();
    }

    private void addTicketsToAnotherStations(List<NormalReservationTickets> tatkalTickets, String toStationName, String
            coachName, Integer numberOfTickets) {
        log.info("Adding Tickets To Others Station:{},{},{}", toStationName, coachName, numberOfTickets);
        NormalReservationTickets tickets = null;
        for (NormalReservationTickets normalTicket : tatkalTickets) {
            if (normalTicket.getStationName().equalsIgnoreCase(toStationName)
                    && normalTicket.getCoachName().equalsIgnoreCase(coachName)) {
                tickets = normalTicket;
            }
        }
        tickets.setNoOfSeatsAvailable(tickets.getNoOfSeatsAvailable() + numberOfTickets);
        log.info("Tickets Successfully Added to Station:{}", toStationName);
    }
}

//    public ResponseEntity<String> bookNormalReservationTickets(BookingRequest request) throws PaymentFailedException {
//        log.info("BookingRequest:{}", request);
//        List<NormalReservationTickets> ticketsList = normalReservationRepo.findAllByTrainNumber(request.getTrainNumber());
//        log.info("TicketList:{}", ticketsList);
//        if (ticketsList != null) {
//            log.info("(ticketsList != null): True");
//            NormalReservationTickets normalTickets = new NormalReservationTickets();
//            for (NormalReservationTickets normalReservationTickets : ticketsList) {
//                if (normalReservationTickets.getTravelDate().equals(request.getTravelDate())) {
//                    log.info("normalReservationTickets:{}", normalTickets);
//                    if (normalReservationTickets.getStationName().equals(request.getFromStationName())
//                            && normalReservationTickets.getCoachName().equals(request.getCoachName())) {
//                        log.info("If (From Station and TravelDay found):");
//                        for (NormalReservationTickets reservationTickets : ticketsList) {
//                            if (reservationTickets.getStationName().equals(request.getToStationName())) {
//                                log.info("If (To Station found):");
//                                normalTickets = normalReservationTickets;
//                                log.info("Train Found:{}", normalTickets);
//                            }
//                        }
//                    }
//                }
//            }
//            if (normalTickets != null) {
//                double totalTicketAmount = 0;
//                if (request.getNumberOfTickets() <= normalTickets.getNoOfSeatsAvailable()) {
//                    log.info("Tickets Are Available");
//                    totalTicketAmount = calculateTotalAmount(request.getBookingMethod(), normalTickets.getEachSeatPrice(),
//                            request.getNumberOfTickets(), request.getCoachName());
//                    if (totalTicketAmount > 0.0) {
//                        PaymentResponse response = bookingServiceToPaymentService.bookNormalTicket(normalTickets, request, totalTicketAmount).getBody();
//                        String result = response.getPaymentStatus();
//                        log.info("Payment Result in NormalReservationService:{}", result);
//                        if (result.equalsIgnoreCase("Payment Success")) {
//                            addTicketsToAnotherStations(ticketsList, normalTickets.getTravelDate(), request.getToStationName(), request.getCoachName(), request.getNumberOfTickets());
//                            int noOfTickets = request.getNumberOfTickets();
//                            normalTickets.setNoOfSeatsAvailable(normalTickets.getNoOfSeatsAvailable() - noOfTickets);
//                            normalTickets.setNoOfSeatsBooked(normalTickets.getNoOfSeatsBooked() + noOfTickets);
//                            bookedTicketsService.addTickets(request, BookingStatus.CONFIRMED, "NO", totalTicketAmount, totalTicketAmount, response.getTransactionID());
//                            return new ResponseEntity<>("Ticket Booked Successfully", HttpStatus.OK);
//                        }
//                    } else {
//                        log.info("Booking Cancelled");
//                        return new ResponseEntity<>("Booking Cancelled", HttpStatus.BAD_REQUEST);
//                    }
//                } else {
//                    System.out.println("Tickets are Insufficient We Can Confirm Onces Tickets Available");
//                    System.out.print("Do you Want to proceed:Y/N:");
//                    Scanner scanner = new Scanner(System.in);
//                    String yesOrno = scanner.nextLine();
//                    if (yesOrno.equalsIgnoreCase("y")) {
//                        totalTicketAmount = calculateTotalAmount(request.getBookingMethod(), normalTickets.getEachSeatPrice(),
//                                request.getNumberOfTickets(), request.getCoachName());
//                        if (totalTicketAmount > 0.0) {
//                            PaymentResponse response = bookingServiceToPaymentService.bookNormalTicket(normalTickets, request, totalTicketAmount).getBody();
//                            String result = response.getPaymentStatus();
//                            log.info("Payment Result in NormalReservationService:{}", result);
//                            if (result.equalsIgnoreCase("Payment Success")) {
//                                bookedTicketsService.addTickets(request, BookingStatus.WAITING, "null", totalTicketAmount, totalTicketAmount, "");
//                                return ResponseEntity.ok("Ticket Booked Successfully In the WAITING List");
//                            } else {
//                                return ResponseEntity.badRequest().body(result);
//                            }
//                        } else {
//                            return ResponseEntity.ok("Booking Cancelled");
//                        }
//                    } else {
//                        return ResponseEntity.ok("Booking Cancelled");
//                    }
//                }
//            } else {
//                log.info("Train fromStation or Destination Station or Travel Date Not found!");
//                return new ResponseEntity<>("Destination Station Not found!", HttpStatus.BAD_REQUEST);
//
//            }
//        } else {
//            log.info("Train Date or Starting Station Not Found");
//            return new ResponseEntity<>("Train Date or Starting Station Not Found", HttpStatus.BAD_REQUEST);
//        }
//        return new ResponseEntity<>("", HttpStatus.OK);
//    }
//
//
//    private double calculateTotalAmount(String bookingMethod, Double eachSeatPrice, int numberOfTickets, String coachName) {
//        log.info("Request in calculateTotalAmount Method in NormalReservationService");
//        TicketPrice ticketPrice = ticketPriceRepo.findByBookingTypeAndCoachName(bookingMethod, coachName).orElseThrow(() -> new RuntimeException("Tickt Price is Not Found"));;
//        Scanner scanner = new Scanner(System.in);
//        if (bookingMethod.equalsIgnoreCase("Normal Reservation")) {
//            double totalTicketPrice = (ticketPrice.getPrice() + eachSeatPrice) * numberOfTickets;
//            System.out.println("Total TicketsPrice = " + totalTicketPrice);
//            System.out.print("Do yo want to place booking Y/N.?:");
//            String yesOrno = scanner.nextLine();
//            if (yesOrno.equalsIgnoreCase("y")) {
//                return totalTicketPrice;
//            }
//        }
//        return 0.0;
//    }
//
//    private void addTicketsToAnotherStations(List<NormalReservationTickets> normalReservationTicketsList, LocalDate travelDate, String toStationName, String coachName, Integer numberOfTickets) {
//        log.info("Adding Tickets To Others Station:{},{},{}", toStationName, coachName, numberOfTickets);
//        NormalReservationTickets normalReservationTickets = null;
//        for (NormalReservationTickets reservationTickets : normalReservationTicketsList) {
//            if (reservationTickets.getTravelDate().equals(travelDate)
//                    && reservationTickets.getStationName().equalsIgnoreCase(toStationName)
//                    && reservationTickets.getCoachName().equalsIgnoreCase(coachName)) {
//                normalReservationTickets = reservationTickets;
//            }
//        }
//        normalReservationTickets.setNoOfSeatsAvailable(normalReservationTickets.getNoOfSeatsAvailable() + numberOfTickets);
//        log.info("Tickets Successfully Added to Station:{}", toStationName);
//    }

