package com.example.User_Service.DTO;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class TicketDTO {

    private Integer train_number;
    private String booking_type;
    private LocalDate travelDate;
    private LocalTime startingTime;
    private LocalDateTime arrivalDateTime;
    private LocalDateTime departureDateTime;
    private String station_name;
    private String coach_name;
    private Integer total_no_of_seats;
    private Double each_seat_price;
}
