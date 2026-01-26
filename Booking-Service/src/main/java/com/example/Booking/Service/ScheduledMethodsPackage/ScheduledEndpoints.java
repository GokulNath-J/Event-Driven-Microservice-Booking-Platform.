package com.example.Booking.Service.ScheduledMethodsPackage;

import com.example.Booking.Service.DTO.TrainNumberTravelDateStartingTime;
import com.example.Booking.Service.Entity.NormalReservationTickets;
import com.example.Booking.Service.Service.BookingService;
import com.example.Booking.Service.Service.LocalNormalReservationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@ToString
@Component
public class ScheduledEndpoints {

    private final static Logger log = LoggerFactory.getLogger(ScheduledEndpoints.class);

    private BookingService bookingService;

    @Autowired
    private LocalNormalReservationService localNormalReservationService;

    public ScheduledEndpoints(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(cron = "0 0 10 * * * ")
    public String takalAndPremiumTatkalOpenning() {
        BookingService.setIsTatkalAndPremiunTatkalClosed(false);
        log.info("takalAndPremiumTatkalOpenned");
        return "takalAndPremiumTatkalOpenned";
    }

    @Scheduled(cron = "0 0 20 * * *")
    public String takalAndPremiumTatkalClosing() {
        BookingService.setIsTatkalAndPremiunTatkalClosed(true);
        log.info("takalAndPremiumTatkalClosed");
        return "takalAndPremiumTatkalClosed";
    }

    @Scheduled(cron = "0 55 9 * * *")
    public String getTatkalAndPremiunTatkal() {
        bookingService.getPremiumAndTataklDTOManually();
        log.info("getTatkalAndPremiunTatkal");
        return "PremiumAndTataklDTOManually";
    }

    @Scheduled(cron = "0 */10 * * * *")
    public String waitingTickets() {
        bookingService.getWaitingListTickets();
        log.info("getWaitingListTickets");
        return "Waiting List Completed";
    }


    @Scheduled(cron = "0 45 9 * * * ")
    public void clearTatkalAndPremiumTatkalRecord() {
        bookingService.clearTatkalAndPremiumTatkalRecord();
        log.info("clearTatkalAndPremiumTatkalRecord");
    }

    @Scheduled(cron = "0 08 15 * * *")
    public void callDistinctNormalReservationTickets() {
        bookingService.getDistinctNormalReservationTickets();
    }


    @Scheduled(cron = "0 */5 * * * *")
    public void closingNormalReservationPerTrain() {
        log.info("closingNormalReservationPerTrain");
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        List<NormalReservationTickets> listOfTrains = localNormalReservationService.getListOfTrains();
        List<NormalReservationTickets> trainList = new ArrayList<>();
        for (NormalReservationTickets normaltickets : listOfTrains) {
            LocalTime closingTime = normaltickets.getStartingTime().minusHours(4);
            if (normaltickets.getTravelDate().equals(currentDate) && currentTime.getHour() == closingTime.getHour()) {
                trainList.add(normaltickets);
            }
        }
        bookingService.closeNormalReservation(trainList);
    }

    @Scheduled(cron = "0 * */3 * * *")
    public void closingCancelletionTicketsPerTrain() {
        log.info("closingCancelletionTicketsPerTrain");
        //Here we are getting the Normal Reservation Tickets of a train which is going to start tomorrow.
        //How.? Because in the Normal Reservation Table we inserted a train in a correct order like (day by day),
        //and we are getting the first row of a particular train in the table
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        List<NormalReservationTickets> listOfTrains = localNormalReservationService.getListOfTrains();
        for (NormalReservationTickets normaltickets : listOfTrains) {
            LocalTime closingTime = normaltickets.getStartingTime().minusHours(1);
            if (normaltickets.getTravelDate().equals(currentDate) && currentTime.getHour() == closingTime.getHour()) {
                bookingService.closingExistingTickets(normaltickets.getTrainNumber(), normaltickets.getTravelDate());
                bookingService.clearNormalTickets(normaltickets.getTrainNumber(), normaltickets.getTravelDate());
            }
        }
    }

    @Scheduled(cron = "0 0 12 * * *")
    public void callToGetNextNormalReservationTickets() {
        bookingService.getNextNormalReservationTickets();
    }

}
