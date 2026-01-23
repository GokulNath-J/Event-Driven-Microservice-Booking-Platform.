package com.example.User_Service.Controller;

import com.example.User_Service.DTO.*;

import com.example.User_Service.ExceptionHandlerPackage.PaymentFailedException;
import com.example.User_Service.ServicePackage.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Register new newUserRegister
    @PostMapping("/register")
    public String register(@RequestBody RegisterNewUserDTO newUserRegister) {
        return userService.registerUser(newUserRegister);
    }

    // Login user
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        boolean success = userService.loginUser(username, password);
        return success ? "Login Successful" : "Invalid Username or Password Incorrect or UserNotFound";
    }

    @PostMapping("/book")
    public ResponseEntity<String> booking(@RequestBody BookingRequest request) throws PaymentFailedException {
        return userService.confirmBooking(request);
    }

    @PostMapping("/createNewEWallet")
    public String createNewEWallet(@RequestParam String username, @RequestParam String password) {
        return userService.createNewEWallet(username, password);
    }

    @GetMapping("/gg")
    public String gen() {
        return UUID.randomUUID().toString().substring(0, 14).replace("-", "");
    }

    @PostMapping("/addMoneyToEWallet")
    public String addMoneyToEWallet(@RequestParam String username, @RequestParam double amount) {
        return userService.addMoneyToEWallet(username, amount);
    }

    @PutMapping("/bookingCancelRequest")
    public ResponseEntity<String> bookingCancelRequest(@RequestBody BookingCancelRequestDTO bookingCancelRequestDTO) {
        return userService.bookingCancelRequest(bookingCancelRequestDTO);
    }

    @PostMapping("/getTrainForNormalBookingByTrainNumber")
    public ResponseEntity<List<TicketDTO>> getTrainForNormalBookingByTrainNumber(@RequestBody TrainDetailsRequest request) {
        return userService.getTrainForNormalBookingByTrainNumber(request);
    }

    @PostMapping("/getTrainForTatkalBookingByTrainNumber")
    public ResponseEntity<List<TicketDTO>> getTrainForTatkalBookingByTrainNumber(@RequestBody TrainDetailsRequest request) {
        return userService.getTrainForTatkalBookingByTrainNumber(request);
    }

    @PostMapping("/getTrainForPremiumTatkalBookingByTrainNumber")
    public ResponseEntity<List<TicketDTO>> getTrainForPremiumTatkalBookingByTrainNumber(@RequestBody TrainDetailsRequest request) {
        return userService.getTrainForPremiumTatkalBookingByTrainNumber(request);
    }
}