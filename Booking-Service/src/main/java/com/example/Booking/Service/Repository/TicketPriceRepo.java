package com.example.Booking.Service.Repository;

import com.example.Booking.Service.Entity.TicketPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketPriceRepo extends JpaRepository<TicketPrice,Integer> {
    Optional<TicketPrice> findByBookingTypeAndCoachName(String bookingMethod, String coachName);
}
