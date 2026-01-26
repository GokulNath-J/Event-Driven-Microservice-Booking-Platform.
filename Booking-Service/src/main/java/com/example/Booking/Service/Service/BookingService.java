package com.example.Booking.Service.Service;


import com.example.Booking.Service.DTO.*;
import com.example.Booking.Service.Entity.*;
import com.example.Booking.Service.ExceptionHandlerPackage.PaymentFailedException;
import com.example.Booking.Service.Feign.TrainFeign;
import com.example.Booking.Service.Kafka.BookingEvent;
import com.example.Booking.Service.Repository.*;

import com.example.Booking.Service.ScheduledMethodsPackage.ScheduledEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class BookingService {

    private TatkalRepo tatkalRepo;

    private PremiumTatkalRepo premiumTatkalRepo;

    private NormalReservationRepo normalReservationRepo;

    private BookingEvent bookingEvent;

    private TicketPriceRepo ticketPriceRepo;

    private TrainFeign trainFeign;

    private TatkalService tatkalService;

    private PremiumTatkalService premiumTatkalService;

    private NormalReservationService normalReservationService;

    private BookingServiceToPaymentService bookingServiceToPaymentService;

    private BookedTicketsRepo bookedTicketsRepo;

    private TrainCoachNumberBookingRepo trainCoachNumberBookingRepo;

    private PassengerDetailsRepo passengerDetailsRepo;

    @Autowired
    private CalculatedAmountRepo calculatedAmountRepo;

    @Autowired
    private BookedTicketsService bookedTicketsService;

    @Autowired
    private BookingRequestTableRepo requestTableRepo;

    @Autowired
    private LocalNormalReservationService localNormalReservationService;

    public BookingService(TatkalRepo tatkalRepo, PremiumTatkalRepo premiumTatkalRepo, NormalReservationRepo normalReservationRepo,
                          BookingEvent bookingEvent, TicketPriceRepo ticketPriceRepo, TrainFeign trainFeign,
                          TatkalService tatkalService, PremiumTatkalService premiumTatkalService,
                          TrainCoachNumberBookingRepo trainCoachNumberBookingRepo,
                          NormalReservationService normalReservationService, BookedTicketsRepo bookedTicketsRepo,
                          BookingServiceToPaymentService bookingServiceToPaymentService, PassengerDetailsRepo passengerDetailsRepo) {
        this.tatkalRepo = tatkalRepo;
        this.premiumTatkalRepo = premiumTatkalRepo;
        this.normalReservationRepo = normalReservationRepo;
        this.bookingEvent = bookingEvent;
        this.ticketPriceRepo = ticketPriceRepo;
        this.trainFeign = trainFeign;
        this.tatkalService = tatkalService;
        this.premiumTatkalService = premiumTatkalService;
        this.trainCoachNumberBookingRepo = trainCoachNumberBookingRepo;
        this.normalReservationService = normalReservationService;
        this.bookedTicketsRepo = bookedTicketsRepo;
        this.bookingServiceToPaymentService = bookingServiceToPaymentService;
        this.passengerDetailsRepo = passengerDetailsRepo;
    }

    /// //////////////////

    private final static Logger log = LoggerFactory.getLogger(BookingService.class);

    private static boolean isTatkalAndPremiunTatkalClosed = true;

    private static LocalTime normalReservationClosingTime = LocalTime.of(20, 0, 0);

    private Map<Integer, TrainNumberTravelDateStartingTime> trainNumberTravelDateStartingTimes = new HashMap<>();

    private Map<Integer, LocalDate> trainNumberAndLastTravelDate = new HashMap<>();

    /// ////////////////////

    public static boolean isIsTatkalAndPremiunTatkalClosed() {
        return isTatkalAndPremiunTatkalClosed;
    }

    public static void setIsTatkalAndPremiunTatkalClosed(boolean isTatkalAndPremiunTatkalClosed) {
        BookingService.isTatkalAndPremiunTatkalClosed = isTatkalAndPremiunTatkalClosed;
    }

    public Map<Integer, TrainNumberTravelDateStartingTime> getTrainNumberTravelDateStartingTimes() {
        return trainNumberTravelDateStartingTimes;
    }

    public void setTrainNumberTravelDateStartingTimes(Map<Integer, TrainNumberTravelDateStartingTime> trainNumberTravelDateStartingTimes) {
        this.trainNumberTravelDateStartingTimes = trainNumberTravelDateStartingTimes;
    }

    //--------------------------------------------------------------------------------------------------------------------

    public List<PremiumAndTatkalDTO> getPremiumAndTataklDTOManually() {
        List<PremiumAndTatkalDTO> trainDTO1 = trainFeign.sendTatkalAndPremiumTataklTicketsToBookingServiceManually();
        addTatkalAndPremiumTatkatTickets(trainDTO1);
        return trainDTO1;
    }

    public void addTatkalAndPremiumTatkatTickets(List<PremiumAndTatkalDTO> trainDTO1) {
        List<TatkalTickets> tatkalList = new ArrayList<>();
        List<PremiumTatkalTickets> premiunTataklList = new ArrayList<>();
        for (PremiumAndTatkalDTO dto1 : trainDTO1) {
            if (dto1.getBooking_type().equalsIgnoreCase("Premium Tatkal")) {
                PremiumTatkalTickets premiumTatkalTickets = new PremiumTatkalTickets(dto1.getTrain_number(), dto1.getCoach_name(), dto1.getStation_name(), dto1.getBooking_type(), dto1.getTotal_no_of_seats(), dto1.getTotal_no_of_seats(), 0, dto1.getEach_seat_price());
                premiunTataklList.add(premiumTatkalTickets);
            } else {
                TatkalTickets tatkalTickets = new TatkalTickets(dto1.getTrain_number(), dto1.getCoach_name(), dto1.getStation_name(), dto1.getBooking_type(), dto1.getTotal_no_of_seats(), dto1.getTotal_no_of_seats(), 0, dto1.getEach_seat_price());
                tatkalList.add(tatkalTickets);
            }
        }
        tatkalRepo.saveAll(tatkalList);
        premiumTatkalRepo.saveAll(premiunTataklList);
    }

    public ResponseEntity<TicketsResponse> bookPremiumAndTatkal(BookingRequest request) throws PaymentFailedException {
        log.info("request in the BookingService");
        if ((isTatkalAndPremiunTatkalClosed != true) && request.getBookingMethod().equalsIgnoreCase("Tatkal")) {
            return tatkalService.book(request, null, null);
        } else if ((isTatkalAndPremiunTatkalClosed != true) && request.getBookingMethod().equalsIgnoreCase("Premium Tatkal")) {
            return premiumTatkalService.book(request, null, null);
        } else {
            throw new RuntimeException("Send the Correct Request");
        }
    }

    private Boolean checkNormalReservationClosingTime(Integer trainNumber, LocalDate travelDate) {
        for (TrainNumberTravelDateStartingTime value : trainNumberTravelDateStartingTimes.values()) {
            if (value.getTrainNumber().equals(trainNumber) && value.getTravelDate().equals(travelDate)) {
                if (value.getIsBookingClosed().equals(true)) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        throw new RuntimeException("Select the Correct Train Number or Travel Date");
    }

    public String getTrainCoachNumberDTOManually(Integer trainNumber) {
        List<TrainCoachNumberDTO> list = trainFeign.sendTrainCoachNumberDTO(trainNumber);
        for (TrainCoachNumberDTO trainCoachNumberDTO : list) {
            List<String> stringList = trainCoachNumberDTO.getCoachNumber();
            for (String string : stringList) {
                TrainCoachNumberBooking trainCoachNumberBooking = new TrainCoachNumberBooking(trainCoachNumberDTO.getTrainNumber(), trainCoachNumberDTO.getCoachName(), trainCoachNumberDTO.getTotalNoOfSeats());
                trainCoachNumberBooking.setCoachNumber(string);
                trainCoachNumberBookingRepo.save(trainCoachNumberBooking);
            }
        }
        return "Success";
    }

    public List<TatkalTickets> getAllTatkalTickets() {
        return tatkalRepo.findAll();
    }

    public List<PremiumTatkalTickets> getPremiumTatkalTickets() {
        return premiumTatkalRepo.findAll();
    }

    public List<TatkalTickets> getAllTatkalTicketsByTrainNumber(Integer trainNumber) {
        return tatkalRepo.findAllByTrainNumber(trainNumber);
    }

    public void addPrice(TicketPrice ticketPrice) {
        ticketPriceRepo.save(ticketPrice);
    }

    public ResponseEntity<String> getNextNormalReservationTickets() {
        log.info("Request in getNextNormalReservationTickets");
        NormalTicketDTOWrapper wrapper = trainFeign.getNextNormalReservationTickets(trainNumberAndLastTravelDate);
        Queue<NormalTicketDTO> dtos = wrapper.getNormalTicketDTOQueue();
        log.info("dtos:{}", dtos);
        List<NormalReservationTickets> ticketsList = new ArrayList<>();
        for (NormalTicketDTO dto : dtos) {
            trainNumberAndLastTravelDate.put(dto.getTrain_number(), dto.getTravelDate());
            NormalReservationTickets normalReservationTickets = new NormalReservationTickets
                    (dto.getTrain_number(), dto.getBooking_type(), dto.getCoach_name(), dto.getTravelDate(),
                            dto.getArrivalDateTime(), dto.getDepartureDateTime(), dto.getStation_name(),
                            dto.getTotal_no_of_seats(), dto.getTotal_no_of_seats(), 0,
                            dto.getEach_seat_price(), dto.getStartingTime());
            normalReservationTickets.setIsBookingClosed(BookingStatus.NO);
            ticketsList.add(normalReservationTickets);
        }
        normalReservationRepo.saveAll(ticketsList);
        localNormalReservationService.setListOfTrains(ticketsList);
        return ResponseEntity.ok("Next Normal Reservation Tickets Saved");
    }

    public void addNormalReservationTickets(NormalTicketDTOWrapper normalTicketDTOWrapper) {
        log.info("Normal Reservation Tickets In BookingService:{}", normalTicketDTOWrapper.getNormalTicketDTOQueue());
        Queue<NormalTicketDTO> normalTicketDTO = normalTicketDTOWrapper.getNormalTicketDTOQueue();
        Queue<NormalReservationTickets> reservationTickets = new LinkedList<>();
        Queue<NormalReservationTickets> reservationTicketsQueue = NormalReservationTickets.getNormalReservationTicketsQueue();
//        Set<Integer> trainNumnuberSet = new HashSet<>();
        for (NormalTicketDTO dto : normalTicketDTO) {
            Integer trainNumber = dto.getTrain_number();
            LocalDate travelDate = dto.getTravelDate();
            NormalReservationTickets normalReservationTickets = new NormalReservationTickets
                    (trainNumber, dto.getBooking_type(), dto.getCoach_name(), travelDate,
                            dto.getArrivalDateTime(), dto.getDepartureDateTime(), dto.getStation_name(),
                            dto.getTotal_no_of_seats(), dto.getTotal_no_of_seats(), 0,
                            dto.getEach_seat_price(), dto.getStartingTime());
            reservationTickets.add(normalReservationTickets);
            reservationTicketsQueue.add(normalReservationTickets);
            trainNumberAndLastTravelDate.put(trainNumber, travelDate);
        }
        normalReservationRepo.saveAll(reservationTickets);

        getTrainCoachNumberDTO(normalTicketDTOWrapper.getTrainCoachNumberDTOList());
    }

    private void addTrainNumberTravelDateStartingTime(NormalReservationTickets normalReservationTickets) {


    }

    private void getTrainCoachNumberDTO(List<TrainCoachNumberDTO> trainCoachNumberDTOS) {
        for (TrainCoachNumberDTO trainCoachNumberDTO : trainCoachNumberDTOS) {
            List<String> stringList = trainCoachNumberDTO.getCoachNumber();
            for (String string : stringList) {
                TrainCoachNumberBooking trainCoachNumberBooking = new TrainCoachNumberBooking(trainCoachNumberDTO.getTrainNumber(), trainCoachNumberDTO.getCoachName(), trainCoachNumberDTO.getTotalNoOfSeats());
                trainCoachNumberBooking.setCoachNumber(string);
                trainCoachNumberBookingRepo.save(trainCoachNumberBooking);
            }
        }
    }

    public List<NormalReservationTickets> getTrainByTrainNumber(Integer trainNumber) {
        return normalReservationRepo.findAllByTrainNumber(trainNumber);
    }

    @Transactional
    public void getWaitingListTickets() {
        log.info("Request in getWaitingListTickets()");
        List<BookedTicketsAndStatus> list = bookedTicketsRepo.findAllByBookingStatusAndTravelDate(BookingStatus.WAITING, LocalDate.now().plusDays(1));
        log.info("BookedTicketsAndStatus in getWaitingListTickets():{}", list);
        Map<Integer, List<BookedTicketsAndStatus>> map = new HashMap<>();
        Map<Integer, List<NormalReservationTickets>> NR = new HashMap<>();
        Map<Integer, List<PremiumTatkalTickets>> PR = new HashMap<>();
        Map<Integer, List<TatkalTickets>> TT = new HashMap<>();
        LocalDate travelDate = list.getFirst().getTravelDate();
        for (BookedTicketsAndStatus bookedTicketsAndStatus : list) {
            Integer trainNumber = bookedTicketsAndStatus.getTrainNumber();
            if (map.containsKey(trainNumber)) {
                map.get(trainNumber).add(bookedTicketsAndStatus);
            } else {
                List<BookedTicketsAndStatus> newlist = new ArrayList<>();
                newlist.add(bookedTicketsAndStatus);
                map.put(trainNumber, newlist);
            }
            if (map.containsKey(trainNumber)) {
                map.get(trainNumber).add(bookedTicketsAndStatus);
            } else {
                List<BookedTicketsAndStatus> newlist = new ArrayList<>();
                newlist.add(bookedTicketsAndStatus);
                map.put(trainNumber, newlist);
            }
            if (map.containsKey(trainNumber)) {
                map.get(trainNumber).add(bookedTicketsAndStatus);
            } else {
                List<BookedTicketsAndStatus> newlist = new ArrayList<>();
                newlist.add(bookedTicketsAndStatus);
                map.put(trainNumber, newlist);
            }
        }
        if (map.size() != 0) {
            log.info("(map.size():{}", map.size());
            getAvailableTicketsFromDB(map, travelDate);
        }
    }

    @Transactional
    private void getAvailableTicketsFromDB(Map<Integer, List<BookedTicketsAndStatus>> map, LocalDate travelDate) {
        log.info("Request in getAvailableTicketsFromDB");
//        List<NormalReservationTickets> normalTickets = new ArrayList<>();
        Map<Integer, List<PremiumTatkalTickets>> PT = new HashMap<>();
        Map<Integer, List<TatkalTickets>> TT = new HashMap<>();
        Map<Integer, List<NormalReservationTickets>> NR = new HashMap<>();
        for (Integer trainNumber : map.keySet()) {
            List<PremiumTatkalTickets> ticketsList = premiumTatkalRepo.findAllByTrainNumber(trainNumber);
            PT.put(trainNumber, ticketsList);
        }
        for (Integer trainNumber : map.keySet()) {
            List<TatkalTickets> ticketsList = tatkalRepo.findAllByTrainNumber(trainNumber);
            TT.put(trainNumber, ticketsList);
        }
        for (Integer trainNumber : map.keySet()) {
            List<NormalReservationTickets> ticketsList = normalReservationRepo.findAllByTrainNumberAndTravelDate(trainNumber, travelDate);
            NR.put(trainNumber, ticketsList);
        }
        log.info("PT:{}", PT);
        log.info("TT:{}", TT);
        log.info("NR:{}", NR);
        log.info("PT Size:{}", PT.size());
        log.info("TT Size:{}", TT.size());
        log.info("NR Size:{}", NR.size());
        checkTickets(map, PT, TT, NR);
    }

    @Transactional
    private void checkTickets(Map<Integer, List<BookedTicketsAndStatus>> map, Map<Integer,
            List<PremiumTatkalTickets>> pt, Map<Integer, List<TatkalTickets>> tt, Map<Integer,
            List<NormalReservationTickets>> nr) {
        log.info("Request in checkTickets");
        for (Map.Entry<Integer, List<BookedTicketsAndStatus>> integerListEntry : map.entrySet()) {
            List<BookedTicketsAndStatus> list = integerListEntry.getValue();
            for (BookedTicketsAndStatus bookedTicketsAndStatus : list) {
                if (bookedTicketsAndStatus.getBookingMethod().equals("Premium Tatkal")) {
                    if (pt.containsKey(integerListEntry.getKey())) {
                        List<PremiumTatkalTickets> ticketsList = pt.get(integerListEntry.getKey());
                        List<PremiumTatkalTickets> finalPR = new ArrayList<>();
                        for (PremiumTatkalTickets premiumTatkalTickets : ticketsList) {
                            if (bookedTicketsAndStatus.getFromStationName().equals(premiumTatkalTickets.getStationName())
                                    || bookedTicketsAndStatus.getToStationName().equals(premiumTatkalTickets.getStationName())) {
                                finalPR.add(premiumTatkalTickets);
                            }
                        }
                        finalPRTicketchecks(bookedTicketsAndStatus, finalPR);
                    }
                } else if (bookedTicketsAndStatus.getBookingMethod().equals("Tatkal")) {
                    if (tt.containsKey(integerListEntry.getKey())) {
                        List<TatkalTickets> ticketsList = tt.get(integerListEntry.getKey());
                        List<TatkalTickets> finalTT = new ArrayList<>();
                        for (TatkalTickets tatkalTickets : ticketsList) {
                            if (bookedTicketsAndStatus.getFromStationName().equals(tatkalTickets.getStationName())
                                    || bookedTicketsAndStatus.getToStationName().equals(tatkalTickets.getStationName())) {
                                finalTT.add(tatkalTickets);
                            }
                        }
                        finalTTTicketchecks(bookedTicketsAndStatus, finalTT);
                    }
                } else {
                    if (nr.containsKey(integerListEntry.getKey())) {
                        List<NormalReservationTickets> ticketsList = nr.get(integerListEntry.getKey());
                        List<NormalReservationTickets> finalNR = new ArrayList<>();
                        for (NormalReservationTickets normalTickets : ticketsList) {
                            if (bookedTicketsAndStatus.getFromStationName().equals(normalTickets.getStationName())
                                    || bookedTicketsAndStatus.getToStationName().equals(normalTickets.getStationName())) {
                                finalNR.add(normalTickets);
                            }
                        }
                        finalNRTicketchecks(bookedTicketsAndStatus, finalNR);
                    }
                }
            }
        }
    }

    @Transactional
    private void finalNRTicketchecks(BookedTicketsAndStatus bookedTicketsAndStatus, List<NormalReservationTickets> finalNR) {
        log.info("Request in finalNRTicketchecks");
        for (NormalReservationTickets normalReservationTickets : finalNR) {
            List<PassengerDetails> passengerDetails = bookedTicketsAndStatus.getPassengersList();
            for (PassengerDetails passengerDetail : passengerDetails) {
                if (passengerDetail.getCoachName().equals(normalReservationTickets.getCoachName())) {
                    boolean bookingOccured = false;
                    int noOfTickets = bookedTicketsAndStatus.getNumberOfTickets();
                    if (bookedTicketsAndStatus.getFromStationName().equals(normalReservationTickets.getStationName())) {
                        int noOfTicketsBooked = normalReservationTickets.getNoOfSeatsBooked();
                        int noOfTicketsAvailabe = normalReservationTickets.getNoOfSeatsAvailable();
                        log.info("Station Name:{}", normalReservationTickets.getStationName());
                        if (bookedTicketsAndStatus.getNumberOfTickets() <= normalReservationTickets.getNoOfSeatsAvailable()) {
                            normalReservationTickets.setNoOfSeatsBooked(noOfTicketsBooked + noOfTickets);
                            normalReservationTickets.setNoOfSeatsAvailable(noOfTicketsAvailabe - noOfTickets);
                            bookedTicketsAndStatus.setBookingStatus(BookingStatus.CONFIRMED);
                            bookedTicketsAndStatus.setWaitingToConfirmTicket("YES");
                            bookingOccured = true;
                        }
                    } else if (bookedTicketsAndStatus.getToStationName().equals(normalReservationTickets.getStationName())) {
                        if (bookingOccured) {
                            int noOfTicketsBooked = normalReservationTickets.getNoOfSeatsBooked();
                            int noOfTicketsAvailabe = normalReservationTickets.getNoOfSeatsAvailable();
                            log.info("Station Name:{}", normalReservationTickets.getStationName());
                            normalReservationTickets.setNoOfSeatsAvailable(noOfTicketsAvailabe + noOfTickets);
                        }
                    }
                }
            }

        }

    }

    @Transactional
    private void finalTTTicketchecks(BookedTicketsAndStatus bookedTicketsAndStatus, List<TatkalTickets> finalPR) {
        log.info("Request in finalNRTicketchecks");
        for (TatkalTickets tatkalTickets : finalPR) {
            List<PassengerDetails> passengerDetails = bookedTicketsAndStatus.getPassengersList();
            for (PassengerDetails passengerDetail : passengerDetails) {
                if (passengerDetail.getCoachName().equals(tatkalTickets.getCoachName())) {
                    boolean bookingOccured = false;
                    int noOfTickets = bookedTicketsAndStatus.getNumberOfTickets();
                    if (bookedTicketsAndStatus.getFromStationName().equals(tatkalTickets.getStationName())) {
                        int noOfTicketsBooked = tatkalTickets.getNoOfSeatsBooked();
                        int noOfTicketsAvailabe = tatkalTickets.getNoOfSeatsAvailable();
                        log.info("Station Name:{}", tatkalTickets.getStationName());
                        if (bookedTicketsAndStatus.getNumberOfTickets() <= tatkalTickets.getNoOfSeatsAvailable()) {
                            tatkalTickets.setNoOfSeatsBooked(noOfTicketsBooked + noOfTickets);
                            tatkalTickets.setNoOfSeatsAvailable(noOfTicketsAvailabe - noOfTickets);
                            bookedTicketsAndStatus.setBookingStatus(BookingStatus.CONFIRMED);
                            bookedTicketsAndStatus.setWaitingToConfirmTicket("YES");
                            bookingOccured = true;
                        }
                    } else if (bookedTicketsAndStatus.getToStationName().equals(tatkalTickets.getStationName())) {
                        if (bookingOccured) {
                            int noOfTicketsBooked = tatkalTickets.getNoOfSeatsBooked();
                            int noOfTicketsAvailabe = tatkalTickets.getNoOfSeatsAvailable();
                            log.info("Station Name:{}", tatkalTickets.getStationName());
                            tatkalTickets.setNoOfSeatsAvailable(noOfTicketsAvailabe + noOfTickets);
                        }
                    }
                }
            }
        }
    }

    @Transactional
    private void finalPRTicketchecks(BookedTicketsAndStatus bookedTicketsAndStatus, List<PremiumTatkalTickets> finalPR) {
        log.info("Request in finalNRTicketchecks");
        for (PremiumTatkalTickets premiumTatkalTickets : finalPR) {
            List<PassengerDetails> passengerDetails = bookedTicketsAndStatus.getPassengersList();
            for (PassengerDetails passengerDetail : passengerDetails) {
                if (passengerDetail.getCoachName().equals(premiumTatkalTickets.getCoachName())) {
                    boolean bookingOccured = false;
                    int noOfTickets = bookedTicketsAndStatus.getNumberOfTickets();
                    if (bookedTicketsAndStatus.getFromStationName().equals(premiumTatkalTickets.getStationName())) {
                        int noOfTicketsBooked = premiumTatkalTickets.getNoOfSeatsBooked();
                        int noOfTicketsAvailabe = premiumTatkalTickets.getNoOfSeatsAvailable();
                        log.info("Station Name:{}", premiumTatkalTickets.getStationName());
                        if (bookedTicketsAndStatus.getNumberOfTickets() <= premiumTatkalTickets.getNoOfSeatsAvailable()) {
                            premiumTatkalTickets.setNoOfSeatsBooked(noOfTicketsBooked + noOfTickets);
                            premiumTatkalTickets.setNoOfSeatsAvailable(noOfTicketsAvailabe - noOfTickets);
                            bookedTicketsAndStatus.setBookingStatus(BookingStatus.CONFIRMED);
                            bookedTicketsAndStatus.setWaitingToConfirmTicket("YES");
                            bookingOccured = true;
                        }
                    } else if (bookedTicketsAndStatus.getToStationName().equals(premiumTatkalTickets.getStationName())) {
                        if (bookingOccured) {
                            int noOfTicketsBooked = premiumTatkalTickets.getNoOfSeatsBooked();
                            int noOfTicketsAvailabe = premiumTatkalTickets.getNoOfSeatsAvailable();
                            log.info("Station Name:{}", premiumTatkalTickets.getStationName());
                            premiumTatkalTickets.setNoOfSeatsAvailable(noOfTicketsAvailabe + noOfTickets);
                        }
                    }
                }
            }
        }
    }

    @Transactional
    public ResponseEntity<String> bookingCancelRequest(BookingCancelRequestDTO bookingCancelRequestDTO) {
        log.info("Request In BookingCancelRequest:{}", bookingCancelRequestDTO);
        BookedTicketsAndStatus bookedTicketsAndStatus = bookedTicketsRepo.findByPnr(bookingCancelRequestDTO.getPnr());
        PassengerDetails details = passengerDetailsRepo.findByPnr(bookingCancelRequestDTO.getPnr());
        log.info("BookedTicketsAndStatus:{}", bookedTicketsAndStatus);
        if (bookedTicketsAndStatus.getBookingStatus().equals(BookingStatus.CONFIRMED)) {
            if (bookedTicketsAndStatus.getIsCancellingTicketsClosed().equals("NO")) {
                double eachTicketPrice = bookedTicketsAndStatus.getAmount();
                bookingServiceToPaymentService.paymentReturn(bookedTicketsAndStatus.getTransactionID(), eachTicketPrice);
                bookedTicketsAndStatus.setBookingStatus(BookingStatus.CANCELLED);
                callCancellationBookingEvent(bookedTicketsAndStatus, details);
                CheckAnyWaitingListTicket(bookedTicketsAndStatus);
            } else {
                log.info("Cancellation Request Closed");
            }
        } else {
            log.info("WAITING Tickets Cant be Cancelled!");
            return ResponseEntity.badRequest().body("WAITING Tickets Cant be Cancelled!");
        }


        return ResponseEntity.ok().body("OK");
    }

    private void callCancellationBookingEvent(BookedTicketsAndStatus bookedTicketsAndStatus, PassengerDetails details) {
        PassengerDetailsResponse detailsResponse = new PassengerDetailsResponse(
                details.getPnr(), details.getPassengerName(), details.getGender(), details.getAge(),
                details.getCoachName(), details.getCoachNumber(), details.getSeatNumber());
        BookingResponse response = new BookingResponse(
                bookedTicketsAndStatus.getPnr(), bookedTicketsAndStatus.getUserName(),
                bookedTicketsAndStatus.getTrainNumber(), bookedTicketsAndStatus.getTravelDate(),
                bookedTicketsAndStatus.getFromStationName(), bookedTicketsAndStatus.getToStationName(), 1,
                bookedTicketsAndStatus.getBookingMethod(), bookedTicketsAndStatus.getAmount(),
                bookedTicketsAndStatus.getWaitingToConfirmTicket(), bookedTicketsAndStatus.getTransactionID(),
                BookingStatus.CANCELLED, List.of(detailsResponse));
        bookingEvent.sendBookingResponseToUser(response);

    }

    @Transactional
    private void CheckAnyWaitingListTicket(BookedTicketsAndStatus bookedTicketsAndStatus) {
        PassengerDetails passengerDetails = passengerDetailsRepo.findByPnr(bookedTicketsAndStatus.getPnr());
        List<BookedTicketsAndStatus> bookTicketsList = bookedTicketsRepo.findAllByBookingStatusAndTravelDate(BookingStatus.WAITING, bookedTicketsAndStatus.getTravelDate());
        for (BookedTicketsAndStatus ticketsAndStatus : bookTicketsList) {
            if (ticketsAndStatus.getTrainNumber().equals(bookedTicketsAndStatus.getTrainNumber())) {
                List<PassengerDetails> waitingPassengerList = ticketsAndStatus.getPassengersList();
                for (PassengerDetails details : waitingPassengerList) {
                    if ((details.getCoachName().equals(passengerDetails.getCoachName()))) {
                        if (ticketsAndStatus.getFromStationName().equals(bookedTicketsAndStatus.getFromStationName())
                                && ticketsAndStatus.getToStationName().equals(bookedTicketsAndStatus.getToStationName())) {
                            ticketsAndStatus.setBookingStatus(BookingStatus.CONFIRMED);
                            ticketsAndStatus.setWaitingToConfirmTicket("YES");
//                            PassengerDetails newPassengerDetails = passengerDetailsRepo.findByPnr(ticketsAndStatus.getPnr());
                            details.setSeatNumber(passengerDetails.getSeatNumber());
                            details.setCoachName(passengerDetails.getCoachName());
                            details.setCoachNumber(passengerDetails.getCoachNumber());
                            passengerDetails.setCoachName("NUL");
                            passengerDetails.setCoachNumber("NULL");
                            passengerDetails.setSeatNumber(0);
                            PassengerDetailsResponse detailsResponse = new PassengerDetailsResponse(
                                    details.getPnr(), details.getPassengerName(), details.getGender(), details.getAge(),
                                    details.getCoachName(), details.getCoachNumber(), details.getSeatNumber());
                            BookingResponse response = new BookingResponse(
                                    details.getPnr(), bookedTicketsAndStatus.getUserName(),
                                    ticketsAndStatus.getTrainNumber(), ticketsAndStatus.getTravelDate(),
                                    ticketsAndStatus.getFromStationName(), ticketsAndStatus.getToStationName(), 1,
                                    ticketsAndStatus.getBookingMethod(), ticketsAndStatus.getAmount(),
                                    ticketsAndStatus.getWaitingToConfirmTicket(), ticketsAndStatus.getTransactionID(),
                                    ticketsAndStatus.getBookingStatus(), List.of(detailsResponse));
                            bookingEvent.sendBookingResponseToUser(response);
                        }
                    }
                }
            }
        }
    }

    public BookedTicketsAndStatus getBookedTicketsAndStatusByPNR(String pnr) {
        return bookedTicketsRepo.findByPnr(pnr);
    }


    public void getLastTrainNumberAndTravelDay() {

    }

    public void getNextDayNormalReservationTickets() {


    }


    public Map<Integer, LocalDate> getTrainNumberAndTravelDay() {
        return trainNumberAndLastTravelDate;
    }

    public void clearTatkalAndPremiumTatkalRecord() {
        tatkalRepo.deleteAll();
        premiumTatkalRepo.deleteAll();
    }

//    @Transactional
//    public void clearNormalTickets() {
//        normalReservationRepo.deleteAllByTravelDate(LocalDate.now());
//    }


    public void getDistinctNormalReservationTickets() {
        List<NormalReservationTickets> list = normalReservationRepo.findTopRowPerTrainNumber();
        log.info("list:{}", list);
        for (NormalReservationTickets tickets : list) {
            TrainNumberTravelDateStartingTime ts = new TrainNumberTravelDateStartingTime(tickets.getTrainNumber(),
                    tickets.getTravelDate(), tickets.getStartingTime());
            trainNumberTravelDateStartingTimes.put(tickets.getTrainNumber(), ts);
        }
        log.info("Map:{}", trainNumberTravelDateStartingTimes);
    }

    @Transactional
    public void closingExistingTickets(Integer trainNumber, LocalDate travelDate) {
        List<BookedTicketsAndStatus> bookedTickets = bookedTicketsRepo.findAllByTrainNumberAndTravelDate(trainNumber, travelDate);
        log.info("bookedTickets:{}", bookedTickets);
        for (BookedTicketsAndStatus bookedTicket : bookedTickets) {
            bookedTicket.setIsCancellingTicketsClosed("YES");
        }
    }

    @Transactional
    public void clearNormalTickets(Integer trainNumber, LocalDate travelDate) {
        normalReservationRepo.deleteAllByTrainNumberAndTravelDate(trainNumber, travelDate);
    }

    public List<NormalTicketDTO> getTrainForNormalBookingByTrainNumber(TrainDetailsRequest request) {
        log.info("In the BookingService");
        List<NormalReservationTickets> ticketsList = normalReservationRepo.findAllByTrainNumberAndTravelDate(request.getTrainNumber(), request.getTravelDate());
        List<NormalTicketDTO> dtoList = new ArrayList<>();
        for (NormalReservationTickets tickets : ticketsList) {
            if (tickets.getStationName().equals(request.getFromStation()) || tickets.getStationName().equals(request.getDestinationStation())) {
                NormalTicketDTO list = NormalTicketDTO.builder()
                        .train_number(tickets.getTrainNumber())
                        .booking_type(tickets.getBookingType())
                        .arrivalDateTime(tickets.getArrivalDateTime())
                        .departureDateTime(tickets.getDepartureDateTime())
                        .coach_name(tickets.getCoachName())
                        .each_seat_price(tickets.getEachSeatPrice())
                        .travelDate(tickets.getTravelDate())
                        .total_no_of_seats(tickets.getNoOfSeatsAvailable())
                        .startingTime(tickets.getStartingTime())
                        .station_name(tickets.getStationName())
                        .build();
                dtoList.add(list);
            }
        }
        return dtoList;
    }

    public List<NormalTicketDTO> getTrainForTatkalBookingByTrainNumber(TrainDetailsRequest request) {
        List<TatkalTickets> tatkalTickets = tatkalRepo.findAllByTrainNumber(request.getTrainNumber());

//        Optional<TatkalTickets> tatkalTickets = tatkalRepo.findByTrainNumber(request.getTrainNumber());

        List<NormalTicketDTO> list = new ArrayList<>();
        for (TatkalTickets tatkalTicket : tatkalTickets) {
            if (tatkalTicket.getStationName().equals(request.getFromStation()) || tatkalTicket.getStationName().equals(request.getDestinationStation())) {
                NormalTicketDTO dto = NormalTicketDTO.builder()
                        .train_number(tatkalTicket.getTrainNumber())
                        .booking_type(tatkalTicket.getBookingType())
                        .coach_name(tatkalTicket.getCoachName())
                        .each_seat_price(tatkalTicket.getEachSeatPrice())
                        .total_no_of_seats(tatkalTicket.getNoOfSeatsAvailable())
                        .station_name(tatkalTicket.getStationName())
                        .build();
                list.add(dto);
            }
        }
        return list;
    }

    public List<NormalTicketDTO> getTrainForPremiumTatkalBookingByTrainNumber(TrainDetailsRequest request) {

        List<PremiumTatkalTickets> tatkalTickets = premiumTatkalRepo.findAllByTrainNumber(request.getTrainNumber());

        List<NormalTicketDTO> list = new ArrayList<>();
        for (PremiumTatkalTickets tatkalTicket : tatkalTickets) {
            if (tatkalTicket.getStationName().equals(request.getFromStation()) || tatkalTicket.getStationName().equals(request.getDestinationStation())) {
                NormalTicketDTO dto = NormalTicketDTO.builder()
                        .train_number(tatkalTicket.getTrainNumber())
                        .booking_type(tatkalTicket.getBookingType())
                        .coach_name(tatkalTicket.getCoachName())
                        .each_seat_price(tatkalTicket.getEachSeatPrice())
                        .total_no_of_seats(tatkalTicket.getNoOfSeatsAvailable())
                        .station_name(tatkalTicket.getStationName())
                        .build();
                list.add(dto);
            }
        }
        return list;
    }

    @Transactional
    public ResponseEntity<String> confirmOrCancelRequest(ConfirmOrCancelRequest request) throws PaymentFailedException {
        CalculatedAmount amount = calculatedAmountRepo.findById(request.getCalculatedAmountId())
                .orElseThrow(() -> new RuntimeException("Id Not Found"));
        BookingRequestTable requestTable = requestTableRepo.findByUserIdAndCalculatedAmountId(request.getUserId(),
                        request.getCalculatedAmountId())
                .orElseThrow(() -> new RuntimeException("User Id or CalculatedAmount ID Not Present"));
        if (!request.getAmount().equals(requestTable.getTotalTicketsPrice())) {
            throw new RuntimeException("Wrong Amount");
        }
        if (requestTable.getBookingStatus().equals(BookingStatus.WAITING) &&
                request.getStatus().equals(BookingStatus.CONFIRMED)) {
            if (requestTable.getBookingMethod().equals("Tatkal") &&
                    requestTable.getTicketAvailability().equals(BookingStatus.YES)) {
                if (isTatkalAndPremiunTatkalClosed == false) {
                    tatkalService.book(null, request, requestTable);
                    requestTable.setBookingStatus(BookingStatus.CONFIRMED);
                    return new ResponseEntity<>("Success Booked", HttpStatus.ACCEPTED);
                } else {
                    throw new RuntimeException("Tatkal or Premium Tatkal Closed");
                }
            } else if (requestTable.getBookingMethod().equals("Premium Tatkal") &&
                    requestTable.getTicketAvailability().equals(BookingStatus.YES)) {
                if (isTatkalAndPremiunTatkalClosed == false) {
                    premiumTatkalService.book(null, request, requestTable);
                    requestTable.setBookingStatus(BookingStatus.CONFIRMED);
                    return new ResponseEntity<>("Success Booked", HttpStatus.ACCEPTED);
                } else {
                    throw new RuntimeException("Tatkal or Premium Tatkal Closed");
                }
            }
        } else {
            throw new RuntimeException("CONFIRMED Request Cannot Book or Please check the request again with Status:CONFIRMED");
        }
        if (requestTable.getBookingMethod().equals("Normal Reservation")) {
            normalReservationService.bookprocess2(request, requestTable);
            requestTable.setBookingStatus(BookingStatus.CONFIRMED);
            return new ResponseEntity<>("Success Booked", HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>("Success Failed", HttpStatus.BAD_REQUEST);
    }

//    public ResponseEntity<TicketsResponse> bookNormalReservation(BookingRequest request) {
//
//
//    }
//    public HashMap<Integer, List<PremiumAndTatkalDTO>> getNormalTicketsManually() {
//        HashMap<Integer, List<PremiumAndTatkalDTO>> trainDTO1 = trainFeign.sendNormalTicketsToBookingServiceManually().getHashMap();
//        addNormalTickets(trainDTO1);
//        return trainDTO1;
//    }

    //    private void addNormalTickets(HashMap<Integer, List<PremiumAndTatkalDTO>> trainDTO1) {
//        List<NormalReservationTickets> normalReservationTicketsList = new ArrayList<>();
//        for (Map.Entry<Integer, List<PremiumAndTatkalDTO>> listEntry : trainDTO1.entrySet()) {
//            List<PremiumAndTatkalDTO> dto1List = listEntry.getValue();
//            for (PremiumAndTatkalDTO dto1 : dto1List) {
//                NormalReservationTickets normalReservationTickets = new NormalReservationTickets
//                        (dto1.getTrain_number(), dto1.getCoach_name(), dto1.getArrivalDateTime(), dto1.getTravelDay(),
//                                dto1.getStation_name(), dto1.getBooking_type(), dto1.getTotal_no_of_seats(),
//                                dto1.getTotal_no_of_seats(), 0, dto1.getEach_seat_price());
//                normalReservationTicketsList.add(normalReservationTickets);
//            }
//        }
//        normalReservationRepo.saveAll(normalReservationTicketsList);
//    }
    public ResponseEntity<TicketsResponse> bookNormalReservation(BookingRequest request) throws PaymentFailedException {
        return normalReservationService.bookProcess1(request, null);
    }

    public List<NormalReservationTickets> findNormalTrainByTravelDate(LocalDate currentDate) {
        List<NormalReservationTickets> trainList = normalReservationRepo.findAllByTravelDate(currentDate);
        return trainList;
    }

    @Transactional
    public void closeNormalReservation(List<NormalReservationTickets> trainList) {
        log.info("Request in closeNormalReservation");
        if (trainList != null) {
            for (NormalReservationTickets train : trainList) {
                List<NormalReservationTickets> tickets = normalReservationRepo.findAllByTrainNumberAndTravelDate(train.getTrainNumber(), train.getTravelDate());
                for (NormalReservationTickets ticket : tickets) {
                    log.info("Train Found");
                    ticket.setIsBookingClosed(BookingStatus.YES);
                }
            }
            log.info("Some Train closed Normal Reservation");
        }
    }
}
