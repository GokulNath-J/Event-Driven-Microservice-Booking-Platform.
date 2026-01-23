package com.example.Booking.Service.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainDetailsRequest {

    private Integer trainNumber;
    private String fromStation;
    private String destinationStation;
    private LocalDate travelDate;

}
