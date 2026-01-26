package com.example.Booking.Service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TrainNumberTravelDateStartingTime {


    private Integer trainNumber;
    private LocalDate travelDate;
    private LocalTime startingTime;
    private Boolean isBookingClosed = false;
    private Boolean isTicketCancellingClosed;


    public TrainNumberTravelDateStartingTime(Integer trainNumber, LocalDate travelDate, LocalTime startingTime) {
        this.trainNumber = trainNumber;
        this.travelDate = travelDate;
        this.startingTime = startingTime;
    }
}
