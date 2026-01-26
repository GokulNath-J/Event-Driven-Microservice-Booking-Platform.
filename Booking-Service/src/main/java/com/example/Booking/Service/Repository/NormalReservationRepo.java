package com.example.Booking.Service.Repository;

import com.example.Booking.Service.Entity.NormalReservationTickets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NormalReservationRepo extends JpaRepository<NormalReservationTickets, Integer> {
    List<NormalReservationTickets> findAllByTrainNumber(Integer trainNumber);

    List<NormalReservationTickets> findAllByTrainNumberAndTravelDate(Integer trainNumber, LocalDate travelDate);

    void deleteAllByTravelDate(LocalDate now);

    @Query(value = "SELECT * FROM normal_reservation_tickets n " +
            "WHERE n.s_no IN (SELECT MIN(s_no) FROM normal_reservation_tickets GROUP BY train_number)",
            nativeQuery = true)
    List<NormalReservationTickets> findTopRowPerTrainNumber();


    void deleteAllByTrainNumberAndTravelDate(Integer trainNumber, LocalDate travelDate);

    List<NormalReservationTickets> findAllByTravelDate(LocalDate currentDate);
}
