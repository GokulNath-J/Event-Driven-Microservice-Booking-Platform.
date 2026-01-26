package com.example.Booking.Service.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PassengerDetailsResponse {

    private String pnr;
    private String passengerName;
    private String gender;
    private Integer age;
    private String coachName;
    private String coachNumber;
    private Integer seatNumber;

    public PassengerDetailsResponse(String pnr, String passengerName, String gender, int age, String coachName) {
        this.pnr = pnr;
        this.passengerName = passengerName;
        this.gender = gender;
        this.age = age;
        this.coachName = coachName;
    }
}
