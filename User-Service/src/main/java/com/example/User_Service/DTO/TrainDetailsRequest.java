package com.example.User_Service.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainDetailsRequest {

    private Integer trainNumber;
    private String fromStation;
    private String destinationStation;
    private LocalDate travelDate;

}
