package com.example.User_Service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    private String userId;
    private Integer trainNumber;
    private LocalDate travelDate; // yyyy-MM-dd
    private String coachName;
    private String fromStationName;
    private String toStationName;
    private Integer numberOfTickets;
    private List<PassengerDetailsDTO> passengersList;
    private String bookingMethod; // Tatkal, Premium Tatkal, Normal
//    private double amount; // Final amount (only in confirm step)
}