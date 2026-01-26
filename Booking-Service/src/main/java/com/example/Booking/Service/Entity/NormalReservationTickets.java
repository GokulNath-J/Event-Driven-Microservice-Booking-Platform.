package com.example.Booking.Service.Entity;


import com.example.Booking.Service.DTO.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.Queue;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NormalReservationTickets {

    @Transient
    private static Queue<NormalReservationTickets> normalReservationTicketsQueue = new LinkedList<>();

    @Id
    @SequenceGenerator(name = "nrtickets", sequenceName = "seqnrtickets", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nrtickets")
    private Integer sNo;
    private Integer trainNumber;
    private String bookingType;
    private String coachName;
    private LocalDate travelDate;
    private LocalTime startingTime;
    private LocalDateTime arrivalDateTime;
    private LocalDateTime departureDateTime;
    private String stationName;
    private Integer totalNoOfSeats;
    private Integer noOfSeatsAvailable;
    private Integer noOfSeatsBooked;
    private Double eachSeatPrice;

    @Enumerated(value = EnumType.STRING)
    private BookingStatus isBookingClosed = BookingStatus.NO;

    public NormalReservationTickets(Integer trainNumber, String bookingType, String coachName,
                                    LocalDate travelDate, LocalDateTime arrivalDateTime,
                                    LocalDateTime departureDateTime, String stationName, Integer totalNoOfSeats,
                                    Integer noOfSeatsAvailable, Integer noOfSeatsBooked, Double eachSeatPrice, LocalTime startingTime) {
        this.trainNumber = trainNumber;
        this.bookingType = bookingType;
        this.coachName = coachName;
        this.travelDate = travelDate;
        this.arrivalDateTime = arrivalDateTime;
        this.departureDateTime = departureDateTime;
        this.stationName = stationName;
        this.totalNoOfSeats = totalNoOfSeats;
        this.noOfSeatsAvailable = noOfSeatsAvailable;
        this.noOfSeatsBooked = noOfSeatsBooked;
        this.eachSeatPrice = eachSeatPrice;
        this.startingTime = startingTime;
    }

    public static Queue<NormalReservationTickets> getNormalReservationTicketsQueue() {
        return normalReservationTicketsQueue;
    }

    public static void setNormalReservationTicketsQueue(Queue<NormalReservationTickets> normalReservationTicketsQueue) {
        NormalReservationTickets.normalReservationTicketsQueue = normalReservationTicketsQueue;
    }
}
