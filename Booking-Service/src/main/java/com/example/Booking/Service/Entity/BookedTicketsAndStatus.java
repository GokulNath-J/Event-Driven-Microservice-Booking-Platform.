package com.example.Booking.Service.Entity;

import com.example.Booking.Service.DTO.BookingRequest;
import com.example.Booking.Service.DTO.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BookedTicketsAndStatus {

    @Id
    @SequenceGenerator(name = "bookedtickets", sequenceName = "seqbookedtickets", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bookedtickets")
    private Integer id;
    private String pnr;
    private String userName;
    private Integer trainNumber;
    private LocalDate travelDate;
    private LocalTime startingTime;
    private String fromStationName;
    private String toStationName;
    private Integer numberOfTickets;
    private String bookingMethod;
    private Double amount;
    private String waitingToConfirmTicket;
    private String transactionID;
    private String isCancellingTicketsClosed;

    @Enumerated(value = EnumType.STRING)
    private BookingStatus bookingStatus;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "pnr", referencedColumnName = "pnr")
    private List<PassengerDetails> passengersList;

    public BookedTicketsAndStatus(BookingRequestTable bookingRequest, BookingStatus bookingStatus, Double amount) {
        this.userName = bookingRequest.getUserId();
        this.trainNumber = bookingRequest.getTrainNumber();
        this.travelDate = bookingRequest.getTravelDate();
        this.fromStationName = bookingRequest.getFromStationName();
        this.toStationName = bookingRequest.getToStationName();
        this.numberOfTickets = 1;
        this.bookingMethod = bookingRequest.getBookingMethod();
        this.amount = amount;
        this.bookingStatus = bookingStatus;

    }


}
