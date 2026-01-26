package com.example.Booking.Service.DTO;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class BookingResponse {


    private String pnr;
    private String userId;
    private Integer trainNumber;
    private LocalDate travelDate;
    private String fromStationName;
    private String toStationName;
    private Integer numberOfTickets;
    private String bookingMethod;
    private Double amount;
    private String waitingToConfirmTicket;
    private String transactionID;

    private BookingStatus bookingStatus;

    private List<PassengerDetailsResponse> passengersResponse;


}
