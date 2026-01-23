package com.example.Booking.Service.Controller;

import com.example.Booking.Service.DTO.*;
import com.example.Booking.Service.Entity.BookedTicketsAndStatus;
import com.example.Booking.Service.Entity.NormalReservationTickets;
import com.example.Booking.Service.Entity.PremiumTatkalTickets;
import com.example.Booking.Service.Entity.TatkalTickets;
import com.example.Booking.Service.ExceptionHandlerPackage.PaymentFailedException;
import com.example.Booking.Service.Service.BookingService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/booking")
public class BookingServiceController {

    private BookingService bookingService;

    public BookingServiceController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/getAllTatkalTickets")
    public List<TatkalTickets> getAllTatkalTickets() {
        return bookingService.getAllTatkalTickets();
    }

    @GetMapping("/getPremiumTatkalTickets")
    public List<PremiumTatkalTickets> getPremiumTatkalTickets() {
        return bookingService.getPremiumTatkalTickets();
    }

    @PostMapping("/bookPremiumAndTatkal")
    public ResponseEntity<String> bookPremiumAndTatkal(@RequestBody BookingRequest request) throws PaymentFailedException {
        return bookingService.bookPremiumAndTatkal(request);
    }

    @PostMapping("/bookNormalReservation")
    public ResponseEntity<String> bookNormalReservation(@RequestBody BookingRequest request) throws PaymentFailedException {
        return bookingService.bookNormalReservation(request);
    }

    @GetMapping("/getAllTatkalTicketsByTrainNumber")
    public List<TatkalTickets> getAllTatkalTicketsByTrainNumber(@RequestParam Integer trainNumber) {
        return bookingService.getAllTatkalTicketsByTrainNumber(trainNumber);
    }

    @GetMapping("/getPremiumAndTataklDTOManually")
    public List<PremiumAndTatkalDTO> getPremiumAndTataklDTOManually() {
        return bookingService.getPremiumAndTataklDTOManually();
    }

    @PostMapping("/addPrice")
    public String addPrice(@RequestBody TicketPrice ticketPrice) {
        bookingService.addPrice(ticketPrice);
        return "Success";
    }

    @GetMapping("/getTrainCoachNumberDTO")
    public String getTrainCoachNumberDTO(@RequestParam Integer trainNumber) {
        return bookingService.getTrainCoachNumberDTOManually(trainNumber);
    }

    @PostMapping("/sendNormalTickets")
    public void sendNormalReservationTickets(@RequestBody NormalTicketDTOWrapper normalTicketDTOWrapper) {
        bookingService.addNormalReservationTickets(normalTicketDTOWrapper);
    }

    @GetMapping("/getTrainByTrainNumber")
    public List<NormalReservationTickets> getTrainByTrainNumber(@RequestParam Integer trainNumber) {
        return bookingService.getTrainByTrainNumber(trainNumber);
    }

    @GetMapping("/callWaitingListTicketsManually")
    public void callWaitingListTicketsManually() {
        bookingService.getWaitingListTickets();
    }

    @GetMapping("/manuallyClosingTatkal")
    public String manuallyClosingTatkal() {
        BookingService.setIsTatkalAndPremiunTatkalClosed(true);
        return "Tatkal And PremiumTatkal Closed";
    }

    @GetMapping("/manuallyOpenningTatkal")
    public String manuallyOpenningTatkal() {
        BookingService.setIsTatkalAndPremiunTatkalClosed(false);
        return "Tatkal And PremiumTatkal Openned";
    }

    @PutMapping("/bookingCancelRequest")
    public ResponseEntity<String> bookingCancelRequest(@RequestBody BookingCancelRequestDTO bookingCancelRequestDTO) {
        return bookingService.bookingCancelRequest(bookingCancelRequestDTO);
    }

    @GetMapping("/getBookedTicketsAndStatusByPNR")
    public BookedTicketsAndStatus getBookedTicketsAndStatusByPNR(@RequestParam String pnr) {
        return bookingService.getBookedTicketsAndStatusByPNR(pnr);
    }

    @GetMapping("/getTrainNumberAndTravelDay")
    public Map<Integer, LocalDate> getTrainNumberAndTravelDay() {
        return bookingService.getTrainNumberAndTravelDay();
    }

    @GetMapping("/getNextNormalReservationTicketsManually")
    public ResponseEntity<String> getNextNormalReservationTickets() {
        return bookingService.getNextNormalReservationTickets();
    }

    @GetMapping("/getDistinctNormalReservationTickets")
    public void getDistinctNormalReservationTickets() {
        bookingService.getDistinctNormalReservationTickets();
    }

    @PostMapping("/getTrainForNormalBookingByTrainNumber")
    public List<NormalTicketDTO> getTrainForNormalBookingByTrainNumber(@RequestBody TrainDetailsRequest request) {
        return bookingService.getTrainForNormalBookingByTrainNumber(request);
    }

    @PostMapping("/getTrainForTatkalBookingByTrainNumber")
    public List<NormalTicketDTO> getTrainForTatkalBookingByTrainNumber(@RequestBody TrainDetailsRequest request) {
        return bookingService.getTrainForTatkalBookingByTrainNumber(request);
    }

    @PostMapping("/booking/getTrainForPremiumTatkalBookingByTrainNumber")
    public List<NormalTicketDTO> getTrainForPremiumTatkalBookingByTrainNumber(@RequestBody TrainDetailsRequest request) {
        return bookingService.getTrainForPremiumTatkalBookingByTrainNumber(request);
    }


    //    @PostMapping("/addtickets")
//    public String addTatkalTickets(){
//        bookingService.addTatkalAndPremiumTatkatTickets();
//        return "Tickets Saved";
//    }

    //    @GetMapping("/getNormalTicketsManually")
//    public HashMap<Integer, List<PremiumAndTatkalDTO>> getNormalTicketsManually() {
//        return bookingService.getNormalTicketsManually();
//    }


}
