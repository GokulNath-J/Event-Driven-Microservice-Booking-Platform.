package com.example.Train_Service.Controller;

//
//import com.example.Train_Service.DTO.NextNormalReservationTicketsRequest;

import com.example.Train_Service.DTO.NormalTicketDTOWrapper;
import com.example.Train_Service.DTO.PremiumAndTatkalDTO;
import com.example.Train_Service.DTO.TrainCoachNumberDTO;
import com.example.Train_Service.Entity.TrainDetails;
import com.example.Train_Service.Service.ServiceClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/rail")
public class RailwayController {

    private static Logger log = LoggerFactory.getLogger(RailwayController.class);

    private ServiceClass serviceClass;

    public RailwayController(ServiceClass serviceClass) {
        this.serviceClass = serviceClass;
    }

    @GetMapping("/getallTrains")
    public ResponseEntity<List<TrainDetails>> getall() {
        return serviceClass.GetAll();
    }

    @PostMapping("/addonetrain")
    public ResponseEntity<String> addoneRail(@RequestBody TrainDetails trainDetails) {
        return serviceClass.AddOneTrain(trainDetails);
    }

    @GetMapping("/sendNormalTicketsToBookingServiceManually")
    public void sendNormalTicketsToBookingServiceManually(@RequestParam int trainNumber) {
        serviceClass.addDatesToNormalTickets(trainNumber);
    }

    @DeleteMapping("/deleteTrainByTrainNumber")
    public ResponseEntity<String> deleteTrainByTrainNumber(@RequestParam Integer trainNumber) {
        return serviceClass.deleteTrainByTrainNumber(trainNumber);
    }

    @GetMapping("/sendTrainCoachNumberDTO")
    public List<TrainCoachNumberDTO> sendTrainCoachNumberDTO(@RequestParam Integer trainNumber) {
        return serviceClass.sendTrainCoachNumberDTO(trainNumber);
    }

    @GetMapping("/getTrainByTrainNumber")
    public TrainDetails getTrainByTrainNumber(@RequestParam Integer trainNumber) {
        return serviceClass.getTrainByTrainNumber(trainNumber);
    }


    @PostMapping("/getNextNormalReservationTickets")
    public NormalTicketDTOWrapper getNextNormalReservationTickets(@RequestBody Map<Integer, LocalDate> trainNumberAndLastTravelDay) {
        log.info("Request in Controller");
        return serviceClass.getNextNormalReservationTickets(trainNumberAndLastTravelDay);
    }

    @GetMapping("/sendTatkalAndPremiumTataklTicketsToBookingServiceManually")
    public List<PremiumAndTatkalDTO> sendTatkalAndPremiumTataklTicketsToBookingServiceManually() {
        return serviceClass.sendTatkalAndPremiumTataklTickets();
    }

    @GetMapping("/verifyTicketsPerStation")
    public void testtickets() {
        serviceClass.verifyTicketsPerStation();
    }

    @GetMapping("/verifyTrain")
    public void verifyTrain() {
        serviceClass.verifyTrain();
    }


//    @PostMapping("/book")
//    public void bookTicket(@RequestParam Integer trainNumber, @RequestParam String coach,
//                           @RequestParam List<String> passengers_name,
//                           @RequestParam Integer noOfTickets, @RequestParam Integer amount,
//                           @RequestParam String bookingType, @RequestParam String date,
//                           @RequestParam String fromstation, @RequestParam String destination) {
//        serviceClass.bookTicket(trainNumber, coach, passengers_name, noOfTickets, amount, bookingType, date, fromstation, destination);
//    }


//    @GetMapping("/testSendingTrainDTO")
//    public void testSendingTrainDTO() {
//        serviceClass.testingSendTrainDTO();
//    }
//   @PostMapping("/addalltrain")
//   @ResponseStatus(HttpStatus.CREATED)
//   public String addallrail(@RequestBody List<TrainDetails> trainDetails){
//        serviceClass.AddAllRail(trainDetails);
//        return "All Rail Saved";
//   }
//
//    @GetMapping("/findbytrainnumberinrail/{train_Number}")
//    public ResponseEntity<TrainDetails> getdetailstrainbynumber(@PathVariable int train_Number){
//        return serviceClass.GetDetailsTrainByNumber(train_Number);
//    }
//
//    /*@GetMapping("/findbytrainname")
//    public ResponseEntity<RailwayNetwork> gettrainbyname(@RequestParam("name") String trainname){
//        return serviceClass.GetTrainbyName(trainname);
//
//    }*/
//
//    @GetMapping("/findtrainbynumber/{train_Number}")
//    public ResponseEntity<TrainWrapper> get_trainwrapper_bytrain_number(@PathVariable int train_Number){
//        return serviceClass.Get_TrainWrapper_ByTrainNumber(train_Number);
//    }
//
//    @GetMapping("/checkticketavailabiltiybytrainnumber/")
//    public ResponseEntity<TicketCheckingWrapper> Check_Allcoach_Ticket_Availability(@RequestParam int train_number){
//        return serviceClass.check_all_coach_ticket_availability(train_number);
//    }
//
//   @PutMapping("/ticket_booking/")
//   public ResponseEntity<PassengerTicketBooking> Ticket_Booking(@RequestParam int train_number, @RequestParam String coach,
//                                                                @RequestParam int no_of_tickets){
//        return serviceClass.ticket_booking(train_number,coach,no_of_tickets);
//   }
//
//   public String Delete_Train_TrainID(@RequestParam int train_number){
//        return serviceClass.delete_train_trainid(train_number);
//    }
//

//   @GetMapping("/Check_Train_From_Startinpoint")

//   public ResponseEntity<List<TrainWrapper>> Check_Train_From_Startinpoint(@RequestParam String startingpoint){

//        return serviceClass.check_train_from_startingpoint(startingpoint);
//   }
//    @GetMapping("/testSendingTrainDTO1")
//    public void testSendingTrainDTO1() {
//        serviceClass.testingSendTrainDTO4();
//    }
//
//    @GetMapping("/testSendingTrainDTO2")
//    public void testSendingTrainDTO2() {
//        serviceClass.testingSendTrainDTO2();
//    }
//
//    @GetMapping("/testSendingTrainDTO3")
//    public void testSendingTrainDTO3() {

//        serviceClass.testingSendTrainDTO3();

//    }
//    @GetMapping("/sendTatkalAndPremiumTataklTicketsToBookingServiceManually")
//    public PremiumAndTatkalDTOWrapper sendTrainDTOToBookingServiceManually() {
//        PremiumAndTatkalDTOWrapper premiumAndTatkalDTOWrapper = new PremiumAndTatkalDTOWrapper(serviceClass.sendTatkalAndPremiumTataklTickets());
//        return premiumAndTatkalDTOWrapper;

//    }
//    @GetMapping("/sendNormalTicketsToBookingServiceManually")
//    public PremiumAndTatkalDTOWrapper sendNormalTicketsToBookingServiceManually() {
//        PremiumAndTatkalDTOWrapper trainDTOWrapper = new PremiumAndTatkalDTOWrapper(serviceClass.createNormalTickts());
//        return trainDTOWrapper;

//    }


}
