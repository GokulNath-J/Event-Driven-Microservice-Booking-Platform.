package com.example.Booking.Service.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PassengerDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String pnr;
    private String passengerName;
    private String gender;
    private Integer age;
    private String coachName;
    private String coachNumber;
    private Integer seatNumber;


    public PassengerDetails(String passengerName, String gender, Integer age) {
        this.passengerName = passengerName;
        this.gender = gender;
        this.age = age;
    }
}
