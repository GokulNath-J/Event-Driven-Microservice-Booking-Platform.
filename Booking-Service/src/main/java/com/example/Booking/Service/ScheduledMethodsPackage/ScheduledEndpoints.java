package com.example.Booking.Service.ScheduledMethodsPackage;

import com.example.Booking.Service.DTO.TrainNumberTravelDateStartingTime;
import com.example.Booking.Service.Service.BookingService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Map;

@Data
@ToString
@Component
public class ScheduledEndpoints {

    private final static Logger log = LoggerFactory.getLogger(ScheduledEndpoints.class);

    private BookingService bookingService;

    public ScheduledEndpoints(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(cron = "0 0 10 * * * ")
    public String takalAndPremiumTatkalOpenning() {
        BookingService.setIsTatkalAndPremiunTatkalClosed(false);
        log.info("takalAndPremiumTatkalOpenned");
        return "takalAndPremiumTatkalOpenned";
    }

    @Scheduled(cron = "0 0 12 * * *")
    public String takalAndPremiumTatkalClosing() {
        BookingService.setIsTatkalAndPremiunTatkalClosed(true);
        log.info("takalAndPremiumTatkalClosed");
        return "takalAndPremiumTatkalClosed";
    }

    @Scheduled(cron = "0 50 9 * * *")
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

//    public void closingExistingTickets() {
//        bookingService.closingExistingTickets();
//    }


    @Scheduled(cron = "0 */5 * * * *")
    public void closingNormalReservationPerTrain() {
        log.info("closingNormalReservationPerTrain");
        Map<Integer, TrainNumberTravelDateStartingTime> tn = bookingService.getTrainNumberTravelDateStartingTimes();
        for (Map.Entry<Integer, TrainNumberTravelDateStartingTime> map : tn.entrySet()) {
            TrainNumberTravelDateStartingTime tTS = map.getValue();
            LocalTime currentTime = LocalTime.now();
            LocalTime trainStartingTime = tTS.getStartingTime();
            LocalTime closingTime = trainStartingTime.minusHours(4);
            if (currentTime.getHour() == closingTime.getHour()) {
                tTS.setBookingClosed(true);
                log.info("TrainNumber:{}:Closed", map.getKey());
            }
        }
    }

    @Scheduled(cron = "0 * */1 * * *")
    public void closingCancelletionTicketsPerTrain() {
        log.info("closingCancelletionTicketsPerTrain");
        //Here we are getting the Normal Reservation Tickets of a train which is going to start tomorrow.
        //How.? Because in the Normal Reservation Table we inserted a train in a correct order like (day by day),
        //and we are getting the first row of a particular train in the table
        Map<Integer, TrainNumberTravelDateStartingTime> tn = bookingService.getTrainNumberTravelDateStartingTimes();
        for (Map.Entry<Integer, TrainNumberTravelDateStartingTime> map : tn.entrySet()) {
            TrainNumberTravelDateStartingTime tTS = map.getValue();
            LocalTime currentTime = LocalTime.now();
            LocalTime trainStartingTime = tTS.getStartingTime();
            LocalTime closingTime = trainStartingTime.minusHours(1);
            if (currentTime.getHour() == closingTime.getHour()) {
                tTS.setTicketCancellingClosed(true);
                bookingService.closingExistingTickets(tTS.getTrainNumber(), tTS.getTravelDate());
                bookingService.clearNormalTickets(tTS.getTrainNumber(), tTS.getTravelDate());
            }
        }
    }

    @Scheduled(cron = "0 0 12 * * *")
    public void callToGetNextNormalReservationTickets() {
        bookingService.getNextNormalReservationTickets();
    }

}
