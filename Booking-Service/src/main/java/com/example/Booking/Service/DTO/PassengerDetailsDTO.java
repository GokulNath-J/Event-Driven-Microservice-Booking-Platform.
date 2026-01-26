package com.example.Booking.Service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerDetailsDTO {
    private String passengerName;
    private String gender;
    private Integer age;
}
