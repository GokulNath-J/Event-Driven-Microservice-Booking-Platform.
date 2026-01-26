package com.example.Booking.Service.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainCoachNumberBooking {

    @Id
    @SequenceGenerator(name = "seqcoachesnum", sequenceName = "seqtrainCoachesNumber", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqcoachesnum")
    private Integer id;
    private Integer trainNumber;
    private String coachName;
    private String coachNumber;
    private Integer totalNoOfSeats;
    private Integer seats = 1;

    public TrainCoachNumberBooking(Integer trainNumber, String coachName, Integer totalNoOfSeats) {
        this.trainNumber = trainNumber;
        this.coachName = coachName;
        this.totalNoOfSeats = totalNoOfSeats;
    }
}
