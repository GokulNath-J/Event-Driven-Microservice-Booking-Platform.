package com.example.Train_Service.Service;


import com.example.Train_Service.DTO.*;
import com.example.Train_Service.Entity.*;
import com.example.Train_Service.Feign.BookingServiceFeign;
import com.example.Train_Service.Repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;

@Service
public class ServiceClass {

    private static final Logger logger = LoggerFactory.getLogger(ServiceClass.class);

    private TrainDetailsRepo trainDetailsRepo;

    private StationDetailsRepo stationDetailsRepo;

    private TrainStoppingStationRepo trainStoppingStationRepo;

    private TrainCoachesRepo trainCoachesRepo;

    private TrainReservationSystemRepo trainReservationSystemRepo;

    private BookingServiceFeign bookingServiceFeign;

    private NextDayTrainTicketsRepo nextDayTrainTicketsRepo;

    public ServiceClass(TrainDetailsRepo trainDetailsRepo, StationDetailsRepo stationDetailsRepo,
                        TrainStoppingStationRepo trainStoppingStationRepo, TrainCoachesRepo trainCoachesRepo,
                        TrainReservationSystemRepo trainReservationSystemRepo, BookingServiceFeign bookingServiceFeign,
                        NextDayTrainTicketsRepo nextDayTrainTicketsRepo) {
        this.trainDetailsRepo = trainDetailsRepo;
        this.stationDetailsRepo = stationDetailsRepo;
        this.trainStoppingStationRepo = trainStoppingStationRepo;
        this.trainCoachesRepo = trainCoachesRepo;
        this.trainReservationSystemRepo = trainReservationSystemRepo;
        this.bookingServiceFeign = bookingServiceFeign;
        this.nextDayTrainTicketsRepo = nextDayTrainTicketsRepo;
    }

    private final List<Integer> trainNumberslist = new ArrayList<>();

    public ResponseEntity<String> AddOneTrain(TrainDetails trainDetails) {
        logger.info("Enter into the method");
        List<TrainStoppingStation> trainStoppingStation = trainDetails.getTrainStoppingStations();
        List<TrainCoaches> trainCoaches = trainDetails.getTrainCoachesList();
        ListIterator<TrainCoaches> list = trainDetails.getTrainCoachesList().listIterator();
        trainDetails.setNoOfStoppingstations(trainDetails.getTrainStoppingStations().size());
        trainDetails.setFromStation(trainStoppingStation.getFirst().getStationName());
        LocalDate date = trainStoppingStation.getFirst().getDepartureDateTime().toLocalDate();
        trainDetails.setFromStationDepartureDate(date);
        trainDetails.setFromStationArrivalDate(trainStoppingStation.getFirst().getArrivalDateTime().toLocalDate());
        trainDetails.setDestinationStation(trainDetails.getTrainStoppingStations().getLast().getStationName());
        List<String> bookingtype = List.of("Tatkal", "Premium Tatkal", "Normal Reservation");
        addTicketsToEachStations2(trainDetails.getTrainNumber(), trainStoppingStation, trainCoaches, bookingtype);
        addCoachNumber(trainDetails.getTrainNumber(), trainCoaches);
        trainDetailsRepo.save(trainDetails);
        addDatesToNormalTickets(trainDetails.getTrainNumber());
        return new ResponseEntity<>("One Train Added", HttpStatus.CREATED);
    }

    public ResponseEntity<List<TrainDetails>> GetAll() {
        List<TrainDetails> getall = trainDetailsRepo.findAll();
        return new ResponseEntity<>(getall, HttpStatus.ACCEPTED);
    }

    public List<PremiumAndTatkalDTO> sendTatkalAndPremiumTataklTickets() {
//        List<TrainDetails> trainDetails = trainDetailsRepo.findAllByFromStationDepartureDate(LocalDate.now().plusDays(1));
        List<NextDayTrainTickets> nextDayTrainTickets = nextDayTrainTicketsRepo.findAllByTravelDate(LocalDate.now().plusDays(1));
//        logger.info("trainDetails size:{}", trainDetails.size());
        String bookingName = "Normal Reservation";
        Integer trainNumber = null;
        List<PremiumAndTatkalDTO> trainDTOList = new ArrayList<>();
        for (NextDayTrainTickets nextDayTrainTicket : nextDayTrainTickets) {
            trainNumber = nextDayTrainTicket.getTrainNumber();
            if (!nextDayTrainTicket.getBookingType().equalsIgnoreCase(bookingName)) {
                PremiumAndTatkalDTO premiumAndTatkalDTO = new PremiumAndTatkalDTO();
                premiumAndTatkalDTO.setTrain_number(nextDayTrainTicket.getTrainNumber());
                premiumAndTatkalDTO.setBooking_type(nextDayTrainTicket.getBookingType());
                premiumAndTatkalDTO.setCoach_name(nextDayTrainTicket.getCoachName());
                premiumAndTatkalDTO.setStation_name(nextDayTrainTicket.getStationName());
                premiumAndTatkalDTO.setTotal_no_of_seats(nextDayTrainTicket.getTotalNoOfSeats());
                premiumAndTatkalDTO.setEach_seat_price(nextDayTrainTicket.getEachSeatPrice());
                premiumAndTatkalDTO.setArrivalDateTime(nextDayTrainTicket.getArrivalDateTime());
                premiumAndTatkalDTO.setTravelDay(nextDayTrainTicket.getArrivalDateTime().toLocalDate());
                premiumAndTatkalDTO.setDepartureDateTime(nextDayTrainTicket.getDepartureDateTime());
                trainDTOList.add(premiumAndTatkalDTO);
            }
        }
        return trainDTOList;
    }

    private List<LocalDate> createTravelDate(List<ArrivalAndDepartureDateTimeDTO> arrivalAndDepartureDateTimeDTOS) {
        List<LocalDate> localDates = new ArrayList<>();
        for (ArrivalAndDepartureDateTimeDTO arrivalAndDepartureDateTimeDTO : arrivalAndDepartureDateTimeDTOS) {
            localDates.add(arrivalAndDepartureDateTimeDTO.getArrivalDateTime().toLocalDate());
        }
        return localDates;
    }

    private List<ArrivalAndDepartureDateTimeDTO> dateCalculate(int looplength, List<TrainRunningMonths> trainRunningMonthsList, List<DayOfWeek> dayOfWeekList, LocalTime defaultArrivalTime, LocalTime defaultDepartureTime, boolean isFirstStation, List<LocalDate> travelDayList) {
        int i = 0;
        logger.info("Request in dateCalculate");
        List<ArrivalAndDepartureDateTimeDTO> dateTimeDTOS = new ArrayList<>();
        logger.info("looplength:{}", looplength);
        for (int month = 0; month < looplength; month++) {
            int loopday = 0;
            TrainRunningMonths trainRunningMonths = trainRunningMonthsList.get(month);
            int year = LocalDate.now().getYear();
            String monthname = trainRunningMonths.getMonth();
            Month month1 = Month.valueOf(monthname);
            LocalDate currentDate = LocalDate.now();
            LocalDate start = LocalDate.of(year, month1, 1);
            int lengthofMonth = start.lengthOfMonth();
            if (month1 == LocalDate.now().getMonth()) {
                loopday = LocalDate.now().getDayOfMonth();
                logger.info("loopday:{}", loopday);
            } else {
                loopday = 1;
                logger.info("loopday:{}", loopday);
            }
            for (int days = loopday; days <= lengthofMonth; days++) {
                LocalDate date = LocalDate.of(year, month1, days);
                if (dayOfWeekList.size() == 1) {
                    logger.info("(dayOfWeekList.size() == 1)");
                    if (dayOfWeekList.contains(date.getDayOfWeek())) {
                        ArrivalAndDepartureDateTimeDTO dateTimeDTO = new ArrivalAndDepartureDateTimeDTO();
                        dateTimeDTO.setArrivalDateTime(defaultArrivalTime.atDate(date));
                        dateTimeDTO.setDepartureDateTime(defaultDepartureTime.atDate(date));
                        dateTimeDTOS.add(dateTimeDTO);
                        if (isFirstStation) {
                            dateTimeDTO.setTravlDate(date);
                        } else {
                            dateTimeDTO.setTravlDate(travelDayList.get(i));
                            i++;
                        }
                    }
                } else {
                    if (dayOfWeekList.get(0).equals(date)) {
                        logger.info("(dayOfWeekList.get(0).equals(date)):{}", date);
                        ArrivalAndDepartureDateTimeDTO dateTimeDTO = new ArrivalAndDepartureDateTimeDTO();
                        dateTimeDTO.setArrivalDateTime(defaultArrivalTime.atDate(date));
                        dateTimeDTOS.add(dateTimeDTO);
                        if (isFirstStation) {
                            dateTimeDTO.setTravlDate(date);
                        } else {
                            dateTimeDTO.setTravlDate(travelDayList.get(i));
                            i++;
                        }
                    } else if (dayOfWeekList.get(1).equals(date)) {
                        logger.info("(dayOfWeekList.get(1).equals(date)):{}", date);
                        ArrivalAndDepartureDateTimeDTO dateTimeDTO = new ArrivalAndDepartureDateTimeDTO();
                        dateTimeDTO.setDepartureDateTime(defaultDepartureTime.atDate(date));
                        dateTimeDTOS.add(dateTimeDTO);
                    }
                }
                logger.info("ArrivalAndDepartureDateTimeDTO List:{}", dateTimeDTOS);

            }
        }
        return dateTimeDTOS;
    }

    private Queue<NormalTicketDTO> sendNormalTicketQueue(Queue<NormalTicketDTO> normalTicketDTOQueue) {
        Queue<NormalTicketDTO> normalTicketDTOQueue1 = new LinkedList<>();
        for (NormalTicketDTO normalTicketDTO : normalTicketDTOQueue) {
            if (normalTicketDTO.getBooking_type().equals("Normal Reservation")) {
                normalTicketDTOQueue1.add(normalTicketDTO);
            }
        }
        return normalTicketDTOQueue1;
    }

    //Currently Working
    public Queue<NormalTicketDTO> createNormalTickts(TrainDetails trainDetails, Map<String, List<ArrivalAndDepartureDateTimeDTO>> matchingDates) {
        logger.info("Inside createNormalTickts:");
        Queue<NormalTicketDTO> normalTicketDTOQueue = new LinkedList<>();
        List<NextDayTrainTickets> nextDayTrainTicketslist = new ArrayList<>();
        for (Map.Entry<String, List<ArrivalAndDepartureDateTimeDTO>> stringListEntry : matchingDates.entrySet()) {
            List<ArrivalAndDepartureDateTimeDTO> list = stringListEntry.getValue();
            for (ArrivalAndDepartureDateTimeDTO dateTimeDTO : list) {
                LocalDate travelDate = dateTimeDTO.getArrivalDateTime().toLocalDate();
                List<TrainStoppingStation> station = trainDetails.getTrainStoppingStations();
//            logger.info("TrainStoppingStation size:{}", station.size());
                LocalTime startingTime = station.getFirst().getArrivalDateTime().toLocalTime();
                for (TrainStoppingStation trainStoppingStation : station) {
//                logger.info("for (TrainStoppingStation trainStoppingStation : station):station_name:{}", trainStoppingStation.getStationName());
                    List<TicketsPerStation> tickets = trainStoppingStation.getTicketsPerStations();
//                logger.info("TicketsPerStation size:{}", tickets.size());
                    for (int i = 0; i < tickets.size(); i++) {
                        TicketsPerStation ticketsPerStation = tickets.get(i);
                        if (ticketsPerStation.getStationName().equalsIgnoreCase(trainStoppingStation.getStationName())) {
                            if (ticketsPerStation.getStationName().equals(stringListEntry.getKey())) {
                                NormalTicketDTO normalTicketDTO = new NormalTicketDTO(trainDetails.getTrainNumber(), ticketsPerStation.getBookingType(), dateTimeDTO.getTravlDate(), startingTime, dateTimeDTO.getArrivalDateTime(), dateTimeDTO.getDepartureDateTime(), trainStoppingStation.getStationName(), ticketsPerStation.getCoachName(), ticketsPerStation.getTotalNoOfSeats(), ticketsPerStation.getEachSeatPrice());
                                normalTicketDTOQueue.add(normalTicketDTO);
                                logger.info("TrainDTO:{}", normalTicketDTO);
                                NextDayTrainTickets next = new NextDayTrainTickets(trainDetails.getTrainNumber(), ticketsPerStation.getBookingType(), ticketsPerStation.getCoachName(), dateTimeDTO.getTravlDate(), startingTime, dateTimeDTO.getArrivalDateTime(), dateTimeDTO.getDepartureDateTime(), trainStoppingStation.getStationName(), ticketsPerStation.getTotalNoOfSeats(), ticketsPerStation.getEachSeatPrice());
                                nextDayTrainTicketslist.add(next);
                            }
                        }
                    }
                }
            }
        }
        nextDayTrainTicketsRepo.saveAll(nextDayTrainTicketslist);
        return sendNormalTicketQueue(normalTicketDTOQueue);
    }

    public void verifyTicketsPerStation() {
        List<TrainDetails> trainDetails = trainDetailsRepo.findAllByFromStationDepartureDate(LocalDate.now().plusDays(1));
        logger.info("trainDetails size:{}", trainDetails.size());
        for (TrainDetails trainDetail : trainDetails) {
            logger.info("for (TrainDetails trainDetail : trainDetails):train_number:{}", trainDetail.getTrainNumber());
            List<TrainStoppingStation> station = trainDetail.getTrainStoppingStations();
            logger.info("TrainStoppingStation size:{}", station.size());
            for (TrainStoppingStation trainStoppingStation : station) {
                logger.info("for (TrainStoppingStation trainStoppingStation : station):station_name:{}", trainStoppingStation.getStationName());
                List<TicketsPerStation> tickets = trainStoppingStation.getTicketsPerStations();
                logger.info("TicketsPerStation size:{}", tickets.size());
                for (int i = 0; i < tickets.size(); i++) {
                    TicketsPerStation ticketsPerStation = tickets.get(i);
                    logger.info("Train Number:{}", ticketsPerStation.getTrainNumber());
                    logger.info("Station Name:{}", trainStoppingStation.getStationName());
                    logger.info("Booking type:{}", ticketsPerStation.getBookingType());
                    logger.info("Coach:{}", ticketsPerStation.getCoachName());
                    logger.info("Total Seat:{}", ticketsPerStation.getTotalNoOfSeats());
                    logger.info("Each Seat Price:{}", ticketsPerStation.getEachSeatPrice());
                    logger.info("======================================================================");
                }
            }
        }
    }

    public void verifyTrain() {
        List<TrainDetails> trainDetails = trainDetailsRepo.findAllByFromStationDepartureDate(LocalDate.now().plusDays(1));
        logger.info("trainDetails size:{}", trainDetails.size());
        for (TrainDetails trainDetail : trainDetails) {
            System.out.println(trainDetail);
        }
    }

    //Third method written to add Number of tickets to each station by booking type (Method Successfully)
    private void addTicketsToEachStations2(int train_number, List<TrainStoppingStation> trainStoppingStation, List<TrainCoaches> trainCoaches, List<String> bookingtype) {
        int totalStations = trainStoppingStation.size();
        for (int i = 0; i < totalStations; i++) {
            TrainStoppingStation station = trainStoppingStation.get(i);
            station.setTrainNumber(train_number);
            List<TicketsPerStation> list = new ArrayList<>();
            for (TrainCoaches trainCoach : trainCoaches) {
                Integer ticketsDividedForEachBookingType = trainCoach.getTotalNoSeats() / 3;
                int divideTicketsPerStation = ticketsDividedForEachBookingType / totalStations;
                int remaining_tickets = ticketsDividedForEachBookingType % totalStations;
                for (String bookingname : bookingtype) {
                    TicketsPerStation ticketsPerStations = new TicketsPerStation(station.getStationName(), bookingname, trainCoach.getCoachName(), divideTicketsPerStation, trainCoach.getEachSeatPrice());
                    logger.info("ticketsPerStations object is Created ");
                    logger.info("To Verify ticketsPerStations object:{}", ticketsPerStations);
                    list.add(ticketsPerStations);
                    logger.info("TicketsPerStation is added to the List:");
                    logger.info("To Verify TicketsPerStation is added to the List size:{}", list.size());
                    logger.info("TicketsPerStation object is added to trainStoppingStation");
                }
            }
            trainStoppingStation.get(i).setTicketsPerStations(list);
        }
    }

    private void addCoachNumber(Integer trainNumber, List<TrainCoaches> trainCoaches) {
        int coachNumber = 1;
        for (int i = 0; i < trainCoaches.size(); i++) {
            TrainCoaches trainCoach = trainCoaches.get(i);
            trainCoach.setTrainNumber(trainNumber);
            logger.info("Coach Name:{}", trainCoach.getCoachName());
            logger.info("TrainNumbeer in Coach:{}", trainCoach.getTrainNumber());
            List<TrainCoachNumber> trainCoachNumbersList = new ArrayList<>();
            int totalCoach = trainCoach.getTotalNoOfCoaches();
            logger.info("totalCoach:{}", totalCoach);
            List<String> coachNumberString = new ArrayList<>();
            for (int j = 0; j < totalCoach; j++) {
                logger.info("j={}", j);
                coachNumberString.add("D" + coachNumber);
                TrainCoachNumber trainCoachNumber = new TrainCoachNumber();
                trainCoachNumber.setTrainNumber(trainNumber);
                trainCoachNumber.setCoachName(trainCoach.getCoachName());
                trainCoachNumber.setCoachNumber("D" + coachNumber);
                trainCoachNumber.setTotalNoOfSeats(trainCoach.getTotalNoSeats() / totalCoach);
                coachNumber++;
                trainCoachNumbersList.add(trainCoachNumber);
            }
            trainCoaches.get(i).setTrainCoachNumberList(trainCoachNumbersList);
//                trainCoach.setTrainCoachNumberList(trainCoachNumbersList);
        }
    }

    @Transactional
    public ResponseEntity<String> deleteTrainByTrainNumber(Integer trainNumber) {
        trainDetailsRepo.deleteByTrainNumber(trainNumber);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Train Deleted");
    }

    public List<TrainCoachNumberDTO> sendTrainCoachNumberDTO(Integer trainNumber) {
        TrainDetails trainDetails = trainDetailsRepo.findByTrainNumber(trainNumber);
        List<TrainCoaches> trainCoaches = trainDetails.getTrainCoachesList();
        List<TrainCoachNumberDTO> trainCoachNumberDTOList = new ArrayList<>();
        for (TrainCoaches trainCoach : trainCoaches) {
            logger.info("CoachName:{}", trainCoach.getCoachName());
            TrainCoachNumberDTO trainCoachNumberDTO = new TrainCoachNumberDTO();
            trainCoachNumberDTO.setTrainNumber(trainCoach.getTrainNumber());
            trainCoachNumberDTO.setCoachName(trainCoach.getCoachName());
            List<TrainCoachNumber> trainCoachNumberList = trainCoach.getTrainCoachNumberList();
            logger.info("Total TrainCoachNumber size{} in Coach:{}", trainCoachNumberList.size(), trainCoach.getCoachName());
            List<String> stringList = new ArrayList<>();
            for (TrainCoachNumber trainCoachNumber : trainCoachNumberList) {
//                if (trainCoachNumber.getCoachName().endsWith(trainCoach.getCoachName())){
                logger.info("CoachNumer:{}", trainCoachNumber.getCoachNumber());
                stringList.add(trainCoachNumber.getCoachNumber());
//                }
                trainCoachNumberDTO.setTotalNoOfSeats(trainCoachNumber.getTotalNoOfSeats());
            }
            trainCoachNumberDTO.setCoachNumber(stringList);
            trainCoachNumberDTOList.add(trainCoachNumberDTO);
        }
        return trainCoachNumberDTOList;
    }

    public TrainDetails getTrainByTrainNumber(Integer trainNumber) {
        return trainDetailsRepo.findByTrainNumber(trainNumber);
    }

    public NormalTicketDTOWrapper getNextNormalReservationTickets(Map<Integer, LocalDate> trainNumberAndLastTravelDay) {
        logger.info("Request In getNextNormalReservationTickets");
        NormalTicketDTOWrapper normalTicketDTOWrapper = new NormalTicketDTOWrapper();
        Queue<NormalTicketDTO> normalTicketDTOS = new LinkedList<>();
        List<NextDayTrainTickets> nextDayTrainTickets = new ArrayList<>();
        for (Map.Entry<Integer, LocalDate> i : trainNumberAndLastTravelDay.entrySet()) {
            logger.info("Key:{} And Value:{}", i.getKey(), i.getValue());
            List<NextDayTrainTickets>
                    list = nextDayTrainTicketsRepo.findAllByTrainNumberAndTravelDate(i.getKey(), i.getValue());
            logger.info("NormalReservationTickets:{}", list);
            DayOfWeek day = i.getValue().getDayOfWeek();
            logger.info("DayOfWeek:{}", day);
            LocalDate traveldate = i.getValue();
            logger.info("TravelDate:{}", traveldate);
            Month month = traveldate.getMonth();
            logger.info("TravelDate Month:{}", month);
            int length = traveldate.lengthOfMonth();
            logger.info("Travel Month Length:{}", length);
            int startday = traveldate.getDayOfMonth();
            ++startday;
            logger.info("Start Day:{}", startday);
            int year = LocalDate.now().getYear();
            LocalDate foundTravelDateTime = traveldate;
            boolean dateFound = false;
            logger.info("Date Found Outer Loop:{}", dateFound);
            while (!dateFound) {
                logger.info("Date Found Inside While Loop:{}", dateFound);
                for (int j = startday; j < length; j++) {
                    if (!dateFound) {
                        logger.info("Start Day:{} And Length:{}", j, length);
                        LocalDate date = LocalDate.of(year, month, j);
                        logger.info("LocalDate:{}", date);
                        if (date.getDayOfWeek().equals(day)) {
                            foundTravelDateTime = date;
                            logger.info("Date Found:{}", date);
                            dateFound = true;
                            break;
                        }
                    }
                }
                startday = 1;
                month = month.plus(1);
                LocalDate date = LocalDate.of(year, month, startday);
                length = date.lengthOfMonth();
                logger.info("Start Day:{} And Month:{} And Length:{} == LocalDate:{}", startday, month, length, date);
            }
            for (NextDayTrainTickets tickets : list) {
                NextDayTrainTickets newNR = new NextDayTrainTickets(
                        tickets.getTrainNumber(), tickets.getBookingType(), tickets.getCoachName(), foundTravelDateTime
                        , tickets.getStartingTime(), tickets.getArrivalDateTime(), tickets.getDepartureDateTime(), tickets.getStationName(), tickets.getTotalNoOfSeats()
                        , tickets.getEachSeatPrice());
                nextDayTrainTickets.add(newNR);
                if (tickets.getBookingType().equals("Normal Reservation")) {
                    NormalTicketDTO normalTicketDTO = new NormalTicketDTO(tickets.getTrainNumber(),
                            tickets.getBookingType(), foundTravelDateTime,
                            tickets.getStartingTime(), tickets.getArrivalDateTime(), tickets.getDepartureDateTime(),
                            tickets.getStationName(), tickets.getCoachName(),
                            tickets.getTotalNoOfSeats(), tickets.getEachSeatPrice());
                    normalTicketDTOS.add(normalTicketDTO);
                }
            }
        }
        addNextTicketsToDB(nextDayTrainTickets);
        normalTicketDTOWrapper.setNormalTicketDTOQueue(normalTicketDTOS);
        logger.info("NormalReservationTickets:{}", normalTicketDTOS);
        return normalTicketDTOWrapper;
    }

    private void addNextTicketsToDB(List<NextDayTrainTickets> nextDayTrainTickets) {
        nextDayTrainTicketsRepo.saveAll(nextDayTrainTickets);
    }

    public void addDatesToNormalTickets(int trainNumber) {
        logger.info("Inside addDatesToNormalTickets");
        TrainDetails trainDetails = trainDetailsRepo.findByTrainNumber(trainNumber);
        NormalTicketDTOWrapper normalTicketDTOWrapper = new NormalTicketDTOWrapper();
        List<TrainStoppingStation> trainStoppingStationList = trainDetails.getTrainStoppingStations();
        List<TrainRunningMonths> trainRunningMonthsList = trainDetails.getTrainRunningMonths();
        List<TrainRunningDays> trainRunningDaysList = trainDetails.getTrainRunningDays();
        int totalTrainRunningDays = trainRunningDaysList.size();
        int totalTrainRunningMonths = trainRunningMonthsList.size();
        String string = trainRunningMonthsList.getFirst().getMonth();
        Month MainMonth = Month.valueOf(string);
        int looplength = 0;
        if (totalTrainRunningMonths >= 3) {
            looplength = 3;
            logger.info("totalTrainRunningMonths:>=3");
        } else if (totalTrainRunningMonths == 2) {
            looplength = 2;
            logger.info("totalTrainRunningMonths: == 2");
        } else {
            looplength = 1;
            logger.info("totalTrainRunningMonths: == 1");
        }
        List<LocalDate> matchingDates = new ArrayList<>();
        List<ArrivalAndDepartureDateTimeDTO> dateTimeDTOS = new ArrayList<>();
        Map<String, List<ArrivalAndDepartureDateTimeDTO>> map = new HashMap<>();
        List<LocalDate> travelDayList = new ArrayList<>();
        for (TrainStoppingStation station : trainStoppingStationList) {
            boolean isFirstStation = true;
            logger.info("Station Name:{}", station.getStationName());
            DayOfWeek arrivalDay = station.getArrivalDateTime().getDayOfWeek();
            DayOfWeek departureDay = station.getArrivalDateTime().getDayOfWeek();
            LocalTime defaultArrivalTime = station.getArrivalDateTime().toLocalTime();
            LocalTime defaultDepartureTime = station.getDepartureDateTime().toLocalTime();
            if (arrivalDay == departureDay) {
                logger.info("arrivalDay == departureDay:True");
                List<DayOfWeek> dayOfWeekList = new ArrayList<>();
                dayOfWeekList.add(arrivalDay);
                map.put(station.getStationName(), dateCalculate(looplength, trainRunningMonthsList, dayOfWeekList, defaultArrivalTime, defaultDepartureTime, isFirstStation, travelDayList));
//                travelDayList = createTravelDate(map.get(station.getStationName()));
//                for (int i = 0; i < travelDayList.size(); i++) {
                if (isFirstStation) {
                    for (Map.Entry<String, List<ArrivalAndDepartureDateTimeDTO>> stringListEntry : map.entrySet()) {
                        List<ArrivalAndDepartureDateTimeDTO> list = stringListEntry.getValue();
                        for (ArrivalAndDepartureDateTimeDTO arrivalAndDepartureDateTimeDTO : list) {
                            travelDayList.add(arrivalAndDepartureDateTimeDTO.getTravlDate());
                        }
                    }
                }
            } else {
                logger.info("arrivalDay != departureDay:True");
                List<DayOfWeek> dayOfWeekList = new ArrayList<>();
                dayOfWeekList.add(arrivalDay);
                dayOfWeekList.add(departureDay);
                map.put(station.getStationName(), dateCalculate(looplength, trainRunningMonthsList, dayOfWeekList, defaultArrivalTime, defaultDepartureTime, isFirstStation, travelDayList));
            }
            isFirstStation = false;
        }
        normalTicketDTOWrapper.setNormalTicketDTOQueue(createNormalTickts(trainDetails, map));
        normalTicketDTOWrapper.setTrainCoachNumberDTOList(sendTrainCoachNumberDTO(trainDetails.getTrainNumber()));
        logger.info("Map<String, List<ArrivalAndDepartureDateTimeDTO>>:{}", map);
        logger.info("Normal Reservation Tickets Before Sending to booking Service:{}",
                normalTicketDTOWrapper.getNormalTicketDTOQueue());
        bookingServiceFeign.sendNormalReservationTickets(normalTicketDTOWrapper);
    }
//    public HashMap<Integer, List<PremiumAndTatkalDTO>> sendTatkalAndPremiumTataklTickets() {
//        List<TrainDetails> trainDetails = trainDetailsRepo.findAllByFromStationDepartureDate(LocalDate.now().plusDays(1));
//        List<NextDayTrainTickets> nextDayTrainTickets = nextDayTrainTicketsRepo.findAllByTravelDate(LocalDate.now());
//        logger.info("trainDetails size:{}", trainDetails.size());
//        HashMap<Integer, List<PremiumAndTatkalDTO>> listHashMap = new HashMap<>();
//        String bookingName = "Normal Reservation";
//        for (TrainDetails trainDetail : trainDetails) {
//            List<PremiumAndTatkalDTO> trainDTOList = new ArrayList<>();
//            logger.info("for (TrainDetails trainDetail : trainDetails):train_number:{}", trainDetail.getTrainNumber());
//            List<TrainStoppingStation> station = trainDetail.getTrainStoppingStations();
//            logger.info("TrainStoppingStation size:{}", station.size());
//            for (TrainStoppingStation trainStoppingStation : station) {
//                logger.info("for (TrainStoppingStation trainStoppingStation : station):station_name:{}", trainStoppingStation.getStationName());
//                List<TicketsPerStation> tickets = trainStoppingStation.getTicketsPerStations();
//                logger.info("TicketsPerStation size:{}", tickets.size());
//                for (int i = 0; i < tickets.size(); i++) {
//                    TicketsPerStation ticketsPerStation = tickets.get(i);
//                    if (ticketsPerStation.getBookingType().equalsIgnoreCase(bookingName)) {
//                    } else if (ticketsPerStation.getStationName().equalsIgnoreCase(trainStoppingStation.getStationName())) {
//                        PremiumAndTatkalDTO premiumAndTatkalDTO = new PremiumAndTatkalDTO();
//                        premiumAndTatkalDTO.setTrain_number(trainDetail.getTrainNumber());
//                        premiumAndTatkalDTO.setBooking_type(ticketsPerStation.getBookingType());
//                        premiumAndTatkalDTO.setCoach_name(ticketsPerStation.getCoachName());
//                        premiumAndTatkalDTO.setStation_name(trainStoppingStation.getStationName());
//                        premiumAndTatkalDTO.setTotal_no_of_seats(ticketsPerStation.getTotalNoOfSeats());
//                        premiumAndTatkalDTO.setEach_seat_price(ticketsPerStation.getEachSeatPrice());
//                        premiumAndTatkalDTO.setArrivalDateTime(trainStoppingStation.getArrivalDateTime());
//                        premiumAndTatkalDTO.setTravelDay(trainStoppingStation.getArrivalDateTime().toLocalDate());
//                        premiumAndTatkalDTO.setDepartureDateTime(trainStoppingStation.getDepartureDateTime());
//                        trainDTOList.add(premiumAndTatkalDTO);
//                    }
//                }
//            }
//            listHashMap.put(trainDetail.getTrainNumber(), trainDTOList);

    /// /        for (TrainDetails trainDetails : trainDetailsList) {
    /// /            Set<TrainRunningMonths> trainRunningMonthsSet = trainDetails.getTrainRunningMonths();
    /// /            Set<TrainRunningDays> trainRunningDaysSet = trainDetails.getTrainRunningDays();
    /// /            for (TrainRunningMonths trainRunningMonths : trainRunningMonthsSet) {
//        for (TrainDetails trainDetails : trainDetailsList) {
//            List<TrainRunningMonths> trainRunningMonthsList = trainDetails.getTrainRunningMonths();
//            List<TrainRunningDays> trainRunningDaysList = trainDetails.getTrainRunningDays();
//            List<DayOfWeek> dayOfWeekList = new ArrayList<>();
//            for (TrainRunningDays trainRunningDays : trainRunningDaysList) {
//                DayOfWeek day = DayOfWeek.valueOf(trainRunningDays.getDay());
//                dayOfWeekList.add(day);
//            }
//            for (TrainRunningMonths trainRunningMonths : trainRunningMonthsList) {
//                int year = LocalDate.now().getYear();
//                String monthname = trainRunningMonths.getMonth();
//                Month month = Month.valueOf(monthname);
//                LocalDate currentDate = LocalDate.now();
//                LocalDate start = LocalDate.of(year, month, 1);
//                int lengthofMonth = start.lengthOfMonth();
//                Month currentMonth = currentDate.getMonth();
//                // Next month
//                Month nextMonth = currentMonth.plus(1);
//                // Third month from now
//                Month thirdMonth = currentMonth.plus(2);
//                List<LocalDate> matchingDates = new ArrayList<>();
//                if (month == currentMonth || month == nextMonth || month == thirdMonth) {
//                    List<LocalDate> localDates = new ArrayList<>();
//                    for (int day = 1; day <= lengthofMonth; day++) {
//                        LocalDate date = LocalDate.of(year, month, day);
//                        if (dayOfWeekList.contains(date.getDayOfWeek())) {
//                            matchingDates.add(date);
//                        }
//                    }
//                }
//            }
//        }
//        addDatesToNormalTickets(trainDetailsList);
//    }

//    public Queue<NormalTicketDTO> createNormalTickts(TrainDetails trainDetails, Map<String, List<ArrivalAndDepartureDateTimeDTO>> matchingDates) {
//        logger.info("Inside createNormalTickts:");
//        Queue<NormalTicketDTO> normalTicketDTOQueue = new LinkedList<>();
//        List<NextDayTrainTickets> nextDayTrainTicketslist = new ArrayList<>();
//        for (Map.Entry<String, List<ArrivalAndDepartureDateTimeDTO>> stringListEntry : matchingDates.entrySet()) {
//            List<ArrivalAndDepartureDateTimeDTO> list = stringListEntry.getValue();
//            for (ArrivalAndDepartureDateTimeDTO dateTimeDTO : list) {
//                LocalDate travelDate = dateTimeDTO.getArrivalDateTime().toLocalDate();
//                List<TrainStoppingStation> station = trainDetails.getTrainStoppingStations();
////            logger.info("TrainStoppingStation size:{}", station.size());
//                LocalTime startingTime = station.getFirst().getArrivalDateTime().toLocalTime();
//                for (TrainStoppingStation trainStoppingStation : station) {
////                logger.info("for (TrainStoppingStation trainStoppingStation : station):station_name:{}", trainStoppingStation.getStationName());
//                    List<TicketsPerStation> tickets = trainStoppingStation.getTicketsPerStations();
////                logger.info("TicketsPerStation size:{}", tickets.size());
//                    for (int i = 0; i < tickets.size(); i++) {
//                        TicketsPerStation ticketsPerStation = tickets.get(i);
//                        if (ticketsPerStation.getBookingType().equalsIgnoreCase("Tatkal") ||
//                                ticketsPerStation.getBookingType().equalsIgnoreCase("Premium Tatkal")) {
//                        } else if (ticketsPerStation.getStationName().equalsIgnoreCase(trainStoppingStation.getStationName())) {
//                            if (ticketsPerStation.getStationName().equals(stringListEntry.getKey())) {
//                                NormalTicketDTO normalTicketDTO = new NormalTicketDTO(trainDetails.getTrainNumber(),
//                                        ticketsPerStation.getBookingType(),dateTimeDTO.getTravlDate().atTime(dateTimeDTO.getArrivalDateTime().toLocalTime())
//                                        ,dateTimeDTO.getArrivalDateTime(),dateTimeDTO.getDepartureDateTime(),trainStoppingStation.getStationName()
//                                ,ticketsPerStation.getCoachName(),ticketsPerStation.getTotalNoOfSeats(),ticketsPerStation.getEachSeatPrice());

    /// /                                normalTicketDTO.setTrain_number(trainDetails.getTrainNumber());
    /// /                                normalTicketDTO.setBooking_type(ticketsPerStation.getBookingType());
    /// /                                normalTicketDTO.setCoach_name(ticketsPerStation.getCoachName());
    /// /                                normalTicketDTO.setStation_name(trainStoppingStation.getStationName());
    /// /                                normalTicketDTO.setTotal_no_of_seats(ticketsPerStation.getTotalNoOfSeats());
    /// /                                normalTicketDTO.setEach_seat_price(ticketsPerStation.getEachSeatPrice());
    /// /                                normalTicketDTO.setTravelDateTime(dateTimeDTO.getTravlDate().atTime(dateTimeDTO.getArrivalDateTime().toLocalTime()));
    /// /                                normalTicketDTO.setArrivalDateTime(dateTimeDTO.getArrivalDateTime());
    /// /                                normalTicketDTO.setDepartureDateTime(dateTimeDTO.getDepartureDateTime());
//                                normalTicketDTOQueue.add(normalTicketDTO);
//                                logger.info("TrainDTO:{}", normalTicketDTO);
//                                NextDayTrainTickets next = new NextDayTrainTickets(
//                                        trainDetails.getTrainNumber(),ticketsPerStation.getBookingType(),ticketsPerStation.getCoachName()
//                                        , dateTimeDTO.getTravlDate(),startingTime,dateTimeDTO.getArrivalDateTime()
//                                        ,dateTimeDTO.getDepartureDateTime(),trainStoppingStation.getStationName()
//                                        ,ticketsPerStation.getTotalNoOfSeats(),ticketsPerStation.getEachSeatPrice());
//                                nextDayTrainTicketslist.add(next);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        nextDayTrainTicketsRepo.saveAll(nextDayTrainTicketslist);
//        return normalTicketDTOQueue;
//    }


    //    public HashMap<Integer, List<PremiumAndTatkalDTO>> createNormalTickts(int plusday) {
//        List<TrainDetails> trainDetails = trainDetailsRepo.findAllByFromStationDepartureDate(LocalDate.now().plusDays(30));
//        logger.info("trainDetails size:{}", trainDetails.size());
//        HashMap<Integer, List<PremiumAndTatkalDTO>> listHashMap = new HashMap<>();
//        for (TrainDetails trainDetail : trainDetails) {
//            List<PremiumAndTatkalDTO> trainDTOList = new ArrayList<>();
//            logger.info("for (TrainDetails trainDetail : trainDetails):train_number:{}", trainDetail.getTrainNumber());
//            List<TrainStoppingStation> station = trainDetail.getTrainStoppingStations();
//            logger.info("TrainStoppingStation size:{}", station.size());
//            for (TrainStoppingStation trainStoppingStation : station) {
//                logger.info("for (TrainStoppingStation trainStoppingStation : station):station_name:{}", trainStoppingStation.getStationName());
//                List<TicketsPerStation> tickets = trainStoppingStation.getTicketsPerStations();
//                logger.info("TicketsPerStation size:{}", tickets.size());
//                for (int i = 0; i < tickets.size(); i++) {
//                    TicketsPerStation ticketsPerStation = tickets.get(i);
//                    if (ticketsPerStation.getBookingType().equalsIgnoreCase("Tatkal") ||
//                            ticketsPerStation.getBookingType().equalsIgnoreCase("Premium Tatkal")) {
//                    } else if (ticketsPerStation.getStationName().equalsIgnoreCase(trainStoppingStation.getStationName())) {
//                        PremiumAndTatkalDTO trainDTO1 = new PremiumAndTatkalDTO();
//                        trainDTO1.setTrain_number(trainDetail.getTrainNumber());
//                        trainDTO1.setBooking_type(ticketsPerStation.getBookingType());
//                        trainDTO1.setCoach_name(ticketsPerStation.getCoachName());
//                        trainDTO1.setStation_name(trainStoppingStation.getStationName());
//                        trainDTO1.setTotal_no_of_seats(ticketsPerStation.getTotalNoOfSeats());
//                        trainDTO1.setEach_seat_price(ticketsPerStation.getEachSeatPrice());
//                        trainDTO1.setTravelDay(trainDetail.getFromStationArrivalDate());
//                        trainDTO1.setArrivalDate(trainStoppingStation.getArrivalDateTime().toLocalDate().plusDays(plusday));
//                        trainDTO1.setDepartureDateTime(trainStoppingStation.getDepartureDateTime().toLocalDate().plusDays(plusday));
//                        trainDTOList.add(trainDTO1);
//                    }
//                }
//            }
//            listHashMap.put(trainDetail.getTrainNumber(), trainDTOList);
//        }

//        return listHashMap;

//    }


//    public HashMap<Integer, List<TrainDTO>> sendTrainDTO() {
//        List<TrainDetails> trainDetails = trainDetailsRepo.findAllByFromStationDepartureDate(LocalDate.now().plusDays(1));
//        logger.info("trainDetails size:{}", trainDetails.size());
//        HashMap<Integer, List<TrainDTO>> listHashMap = new HashMap<>();
//        List<TrainDTO> trainDTOList = new ArrayList<>();
//        String bookingName = "Normal Reservation";
//        for (TrainDetails trainDetail : trainDetails) {
//            logger.info("for (TrainDetails trainDetail : trainDetails):train_number:{}", trainDetail.getTrain_number());
//            List<TrainStoppingStation> station = trainDetail.getTrainStoppingStations();
//            logger.info("TrainStoppingStation size:{}", station.size());
//            TrainDTO trainDTO1 = new TrainDTO();
//            for (TrainStoppingStation trainStoppingStation : station) {
//                logger.info("for (TrainStoppingStation trainStoppingStation : station):station_name:{}", trainStoppingStation.getStation_name());
//                List<TicketsPerStation> tickets = trainStoppingStation.getTicketsPerStations();
//                logger.info("TicketsPerStation size:{}", tickets.size());
//                List<TicketsPerStationDTO> ticketsPerStationDTOS = new ArrayList<>();
//                for (int i = 0; i < tickets.size(); i++) {
//                    TicketsPerStation ticketsPerStation = tickets.get(i);
//                    if (ticketsPerStation.getBooking_type().equalsIgnoreCase(bookingName)) {
//                    } else {
//                        TicketsPerStationDTO dto = new TicketsPerStationDTO();
//                        dto.setStation_name(trainStoppingStation.getStation_name());
//                        dto.setCoach_name(ticketsPerStation.getCoach_name());
//                        dto.setTotal_no_of_seats(ticketsPerStation.getTotal_no_of_seats());
//                        dto.setNo_of_seats_available(ticketsPerStation.getTotal_no_of_seats());
//                        dto.setEach_seat_price(ticketsPerStation.getEach_seat_price());
//                        //trainDTO1.setBooking_type(ticketsPerStation.getBooking_type());
//                        ticketsPerStationDTOS.add(dto);
//                    }
//                }
//                trainDTO1.setTrain_number(trainDetail.getTrain_number());
//                trainDTO1.setList(ticketsPerStationDTOS);
//                trainDTOList.add(trainDTO1);
//            }
//            listHashMap.put(trainDetail.getTrain_number(), trainDTOList);
//        }
//        return listHashMap;

//    }
//
//    public void testingSendTrainDTO4() {
//        HashMap<Integer, List<PremiumAndTatkalDTO>> hashMap = sendTatkalAndPremiumTataklTickets();
//        for (Integer i : hashMap.keySet()) {
//            System.out.println(i);
//        }
//    }
//
//    public void testingSendTrainDTO2() {
//        HashMap<Integer, List<PremiumAndTatkalDTO>> hashMap = sendTatkalAndPremiumTataklTickets();
//        for (List<PremiumAndTatkalDTO> value : hashMap.values()) {
//            System.out.println(value);
//        }
//    }
//
//    public void testingSendTrainDTO3() {
//        HashMap<Integer, List<PremiumAndTatkalDTO>> hashMap = sendTatkalAndPremiumTataklTickets();
//        for (Map.Entry<Integer, List<PremiumAndTatkalDTO>> entry : hashMap.entrySet()) {
//            Integer key = entry.getKey();
//            List<PremiumAndTatkalDTO> trains = entry.getValue();
//
//            System.out.println("Key (some integer): " + key);
//            System.out.println("Trains:");
//
//            for (PremiumAndTatkalDTO train : trains) {
//                System.out.println("  Train Train : " + train.getTrain_number());
//                System.out.println("  Train Booking-Type : " + train.getBooking_type());
//                System.out.println("  Train Coach : " + train.getCoach_name());
//                System.out.println("  Train Station : " + train.getStation_name());
//                System.out.println("  Total No Of Tickets : " + train.getTotal_no_of_seats());
//                System.out.println("  Each Seat Price : " + train.getEach_seat_price());
//                System.out.println("  ---------------------------");
//            }
//
//            System.out.println("================================");
//        }
//    }


//    public void AddAllRail(List<TrainDetails> trainDetails) {
//        trainDetailsRepo.saveAll(trainDetails);
//    }
//
//    public ResponseEntity<TrainDetails> GetTrainbyName(String trainname) {
//        return new ResponseEntity<>(trainDetailsRepo.findbytrain_name(trainname),HttpStatus.FOUND);
//    }
//
//    public ResponseEntity<TrainWrapper> Get_TrainWrapper_ByTrainNumber(int trainNumber) {
//        TrainDetails trainDetails = trainDetailsRepo.findByTrain_Number(trainNumber);
//        TrainWrapper trainWrapper = new TrainWrapper();
//        trainWrapper.setTrain_number(trainDetails.getTrain_number());
//        trainWrapper.setTrain_name(trainDetails.getTrain_name());
//        trainWrapper.setStarting_point(trainDetails.getStartingPoint());
//        trainWrapper.setDestination(trainDetails.getDestination());
//        List<TrainStoppingStationWrapper> stoppingStationWrappers = new ArrayList<>();
//        for (int i = 0; i < trainDetails.getTrainStoppingStations().size(); i++) {
//            String station_name = trainDetails.getTrainStoppingStations().get(i).getStation_name();
//            Integer platform_number = trainDetails.getTrainStoppingStations().get(i).getPlatform_no();
//            TrainStoppingStationWrapper trainStoppingStationWrapper = new TrainStoppingStationWrapper();
//            trainStoppingStationWrapper.setStation_name(station_name);
//            trainStoppingStationWrapper.setPlatform_no(platform_number);
//            stoppingStationWrappers.add(trainStoppingStationWrapper);
//        }
//
//        trainWrapper.setTrainStoppingStationWrapperList(stoppingStationWrappers);
//        trainWrapper.setNo_stopping_stations(trainDetails.getNo_of_stoppingstations());
//        return new ResponseEntity<>(trainWrapper,HttpStatus.OK);
//    }
//
//    public ResponseEntity<TrainDetails> GetDetailsTrainByNumber(int trainNumber) {
//        return new ResponseEntity<>(trainDetailsRepo.findByTrain_Number(trainNumber),HttpStatus.FOUND);
//    }
//
//    public ResponseEntity<TicketCheckingWrapper> check_all_coach_ticket_availability(int trainnumber){
//         TrainDetails trainDetails = trainDetailsRepo.findByTrain_Number(trainnumber);
//         TicketCheckingWrapper ticketCheckingWrapper = new TicketCheckingWrapper();
//         ticketCheckingWrapper.setTrain_name(trainDetails.getTrain_name());
//         ticketCheckingWrapper.setTrain_number(trainDetails.getTrain_number());
//         ticketCheckingWrapper.setNo_general_reserve_tickets(trainDetails.getTrainReservationSystem().getNo_nonac_reservation_tickets());
//         ticketCheckingWrapper.setNo_ac_tickets(trainDetails.getTrainReservationSystem().getNo_ac_coach_tickets());
//         ticketCheckingWrapper.setNo_sleeper_tickets(trainDetails.getTrainReservationSystem().getNo_sleeper_coach_tickets());
//         ticketCheckingWrapper.setReservation_closes_at(trainDetails.getTrainReservationSystem().getReservation_closes_at());
//         ticketCheckingWrapper.setReservation_opens_at(trainDetails.getTrainReservationSystem().getReservation_opens_at());
//         return new ResponseEntity<>(ticketCheckingWrapper,HttpStatus.OK);
//    }
//    public ResponseEntity<PassengerTicketBooking> ticket_booking(int trainNumber, String coach, int noOfTickets) {
//        TrainReservationSystem trainReservationSystem = trainReservationSystemRepo.findByTrain_Number(trainNumber);
//        PassengerTicketBooking passengerTicketBooking = new PassengerTicketBooking();
//        Integer ticket_Available;
//        String Message = "";
//        if(coach.trim().equalsIgnoreCase("AC")){
//             ticket_Available = trainReservationSystem.getNo_ac_coach_tickets();
//             Message = Ac_ticket_booking(trainNumber,ticket_Available,noOfTickets,passengerTicketBooking);
//             if(Message.equalsIgnoreCase("success")){
//                 passengerTicketBooking1(passengerTicketBooking,coach.toUpperCase()+" Ticket :"+noOfTickets,trainNumber);
//             }
//
//        } else if (coach.trim().equalsIgnoreCase("Sleeper")) {
//            ticket_Available = trainReservationSystem.getNo_sleeper_coach_tickets();
//
//            Message = Ac_ticket_booking(trainNumber,ticket_Available,noOfTickets,passengerTicketBooking);
//            if(Message.equalsIgnoreCase("success")){
//                passengerTicketBooking1(passengerTicketBooking,coach.toUpperCase()+" Ticket :"+noOfTickets,trainNumber);
//
//            }
//
//        }else {
//            ticket_Available = trainReservationSystem.getNo_nonac_reservation_tickets();
//
//            Message = Ac_ticket_booking(trainNumber,ticket_Available,noOfTickets,passengerTicketBooking);
//            if(Message.equalsIgnoreCase("success")){
//                passengerTicketBooking1(passengerTicketBooking,coach.toUpperCase()+" Ticket :"+noOfTickets,trainNumber);
//                //   passengerTicketBooking.setNo_of_tickets("Ac Ticket : "+noOfTickets);
//            }
//
//        }
//        if(Message.equalsIgnoreCase("Success")){
//            return new ResponseEntity<>(passengerTicketBooking,HttpStatus.OK);
//        }else {
//            return new ResponseEntity<>(passengerTicketBooking,HttpStatus.BAD_REQUEST);
//        }
//    }
//
//    private String Ac_ticket_booking(int trainNumber, Integer ticketAvailable, int noOfTickets,PassengerTicketBooking passengerTicketBooking) {
//        if (noOfTickets<=ticketAvailable){
//            noOfTickets = (ticketAvailable-noOfTickets);
//            trainReservationSystemRepo.updateAcTicket(trainNumber,noOfTickets);
//            passengerTicketBooking.setTicket_conformation("Success");
//            passengerTicketBooking.setReserved_on_datetime(LocalDateTime.now());
//            return "Success";
//        }else{
//            passengerTicketBooking.setTicket_conformation("Reservation Failed");
//            passengerTicketBooking.setReserved_on_datetime(LocalDateTime.now());
//            return "Reservation Failed";
//        }
//    }
//    private String Sleeper_ticket_booking(int trainNumber, Integer ticketAvailable, int noOfTickets,PassengerTicketBooking passengerTicketBooking) {
//        if (noOfTickets<=ticketAvailable){
//            noOfTickets = (ticketAvailable-noOfTickets);
//            trainReservationSystemRepo.updateSleeperTicket(trainNumber,noOfTickets);
//            passengerTicketBooking.setTicket_conformation("Success");
//            passengerTicketBooking.setReserved_on_datetime(LocalDateTime.now());
//            return "Success";
//        }else{
//            passengerTicketBooking.setTicket_conformation("Reservation Failed");
//            passengerTicketBooking.setReserved_on_datetime(LocalDateTime.now());
//            return "Reservation Failed";
//        }
//    }
//    private String General_reserve_ticket_booking(int trainNumber, Integer ticketAvailable, int noOfTickets,PassengerTicketBooking passengerTicketBooking) {
//        if (noOfTickets<=ticketAvailable){
//            noOfTickets = (ticketAvailable-noOfTickets);
//            trainReservationSystemRepo.updateGeneralReserveTicket(trainNumber,noOfTickets);
//            passengerTicketBooking.setTicket_conformation("Success");
//            passengerTicketBooking.setReserved_on_datetime(LocalDateTime.now());
//            return "Success";
//        }else{
//            passengerTicketBooking.setTicket_conformation("Reservation Failed");
//            passengerTicketBooking.setReserved_on_datetime(LocalDateTime.now());
//            return "Reservation Failed";
//        }
//    }
//
//    private void passengerTicketBooking1(PassengerTicketBooking passengerTicketBooking,String noOfTickets,int trainNumber) {
//        TrainDetails trainDetails = trainDetailsRepo.findByTrain_Number(trainNumber);
//       passengerTicketBooking.setTrain_name(trainDetails.getTrain_name());
//       passengerTicketBooking.setTrain_number(trainDetails.getTrain_number());
//       passengerTicketBooking.setStarting_point(trainDetails.getStartingPoint());
//       passengerTicketBooking.setDestination(trainDetails.getDestination());
//       passengerTicketBooking.setNo_of_tickets(noOfTickets);
//       passengerTicketBooking.setReserved_on_datetime(LocalDateTime.now());
//    }
//
//    public String delete_train_trainid(int trainNumber) {
//        trainDetailsRepo.deleteById(trainNumber);
//        return "Train Deleted";
//    }
//
//    public ResponseEntity<List<TrainWrapper>> check_train_from_startingpoint(String startingpoint) {
//        List<TrainDetails> traindetails = trainDetailsRepo.findByStartingPointIgnoreCase(startingpoint);
//        List<TrainWrapper> trainWrapperList = new ArrayList<>();
//        for (int i = 0; i < traindetails.size(); i++){
//            trainWrapperList.get(i).setTrain_number(traindetails.get(i).getTrain_number());
//            trainWrapperList.get(i).setTrain_name(traindetails.get(i).getTrain_name());
//            List<TrainStoppingStationWrapper> trainStoppingStationWrapper = new ArrayList<>();
//            for (int j = 0; j < traindetails.get(i).getTrainStoppingStations().size(); j++) {
//                String station_name = traindetails.get(i).getTrainStoppingStations().get(j).getStation_name();
//                Integer platform_number = traindetails.get(i).getTrainStoppingStations().get(j).getPlatform_no();
//                trainStoppingStationWrapper.get(j).setStation_name(station_name);
//                trainStoppingStationWrapper.get(j).setPlatform_no(platform_number);
//            }
//            trainWrapperList.get(i).setTrainStoppingStationWrapperList(trainStoppingStationWrapper);
//            trainWrapperList.get(i).setNo_stopping_stations(traindetails.get(i).getNo_of_stoppingstations());
//        }
//        return new ResponseEntity<>(trainWrapperList,HttpStatus.OK);
//    }
    public void bookTicket(Integer trainNumber, String coach, List<String> passengers_name, Integer noOfTickets, Integer amount, String bookingType, String date, String fromstation, String destination) {
//        TrainDetails trainDetails = trainDetailsRepo.findByTrainNumber(trainNumber);
//        if (trainDetails != null) {
//            logger.info("trainDetails:");
//        } else {
//            System.out.println("Throw Exception");
//            return;
//        }
//        ListIterator<TrainStoppingStation> list1 = trainDetails.getTrainStoppingStations().listIterator();
//        boolean check = checkTrainStationFromToDestination.checkTainFromToDestination(list1, fromstation, destination);
//        ListIterator<TrainCoaches> listIterator = trainDetails.getTrainCoachesList().listIterator();
//        Ticket ticket = null;
//        TrainCoaches trainCoaches = null;

        /// /        while (listIterator.hasNext()) {
        /// /            trainCoaches = listIterator.next();
        /// /            String Coaches = trainCoaches.getCoach_name();
        /// /            logger.info("First If {}", Coaches);
        /// /            if (Coaches.equalsIgnoreCase(coach)) {
        /// /                ticket = trainCoaches.getTicket();
        /// /                logger.info("{}", Coaches);
        /// /                break;
        /// /            }
        /// /        }
//        Integer availabletickets = ticket.getAvailableTickets();
//        boolean checkavailabletickets = validTicket.checkTicketAvailability(noOfTickets, availabletickets);
//        if (check && checkavailabletickets) {
//            Booking booking = bookingTypeCheck(bookingType);
//            booking.book(trainDetails, coach, noOfTickets, amount, bookingType, date, fromstation, destination);
//        }
//
//        System.out.println(trainCoaches.getCoachName());
//    }
//
//    Booking bookingTypeCheck(String bookingType) {
//        if (bookingType.equalsIgnoreCase("tatkal")) {
//            return new TatkalService();
//        } else if (bookingType.equalsIgnoreCase("GR")) {
//            return new GeneralReservationService();
//        }
//        return null;
//    }

    }
}
