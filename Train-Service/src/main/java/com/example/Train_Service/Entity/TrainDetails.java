package com.example.Train_Service.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TrainDetails {

    @Id
    @SequenceGenerator(name = "traindetailsseq", sequenceName = "seqtraindetails", allocationSize = 1, initialValue = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "traindetailsseq")
    private Integer id;
    @Column(unique = true)
    private Integer trainNumber;
    private String trainName;
    private String fromStation;
    private String destinationStation;
    private Integer noOfStoppingstations;
    private LocalDate fromStationArrivalDate;
    @Column(name = "from_station_departure_date")
    private LocalDate fromStationDepartureDate;

    @Transient
    private static String name;


    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "trainNumber", referencedColumnName = "trainNumber")
    private List<TrainStoppingStation> trainStoppingStations;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "trainNumber", referencedColumnName = "trainNumber")
    private List<TrainCoaches> trainCoachesList;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "trainNumber", referencedColumnName = "trainNumber")
    private List<TrainRunningDays> trainRunningDays;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "trainNumber", referencedColumnName = "trainNumber")
    private List<TrainRunningMonths> trainRunningMonths;
}
