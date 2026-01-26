package com.example.User_Service.ServicePackage;


import com.example.User_Service.DTO.*;
import com.example.User_Service.Entity.NewUserRegister;
import com.example.User_Service.Entity.UserLoginStatus;
import com.example.User_Service.ExceptionHandlerPackage.PaymentFailedException;
import com.example.User_Service.OpenFeign.BookingFeign;
import com.example.User_Service.OpenFeign.PaymentFeign;
import com.example.User_Service.Repository.UserLoginStatusRepo;
import com.example.User_Service.Repository.UserRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private UserRepository userRepository;

    private PaymentFeign paymentFeign;

    private BookingFeign bookingFeign;

    private UserLoginStatusRepo userLoginStatusRepo;

    public UserService(UserRepository userRepository, PaymentFeign paymentFeign, BookingFeign bookingFeign, UserLoginStatusRepo userLoginStatusRepo) {
        this.userRepository = userRepository;
        this.paymentFeign = paymentFeign;
        this.bookingFeign = bookingFeign;
        this.userLoginStatusRepo = userLoginStatusRepo;
    }

    // Register new newUserRegister
    public String registerUser(RegisterNewUserDTO newUserRegister) {
        String value = String.valueOf(newUserRegister.getPhoneNumber());
        NewUserRegister userRegister = NewUserRegister.builder()
                .userId(newUserRegister.getUserName() + value.substring(value.length() - 4))
                .userName(newUserRegister.getUserName())
                .phoneNumber(newUserRegister.getPhoneNumber())
                .email(newUserRegister.getEmail())
                .password(newUserRegister.getPassword())
                .build();
        log.info("UserName from the PostMan:{}", newUserRegister.getUserName());
        log.info("PhoneNumber from the PostMan:{}", newUserRegister.getPhoneNumber());
//        log.info("EWalletDTO from PostMan:{}", newUserRegister.getEWallet());
        userRepository.save(userRegister);
        return "Success";

    }

    // Login user
    public boolean loginUser(String username, String password) {
        Optional<UserLoginStatus> loginStatus = userLoginStatusRepo.findByUserName(username);
        log.info("loginStatus:{}", loginStatus.isPresent());
        if (loginStatus.isPresent()) {
            if (loginStatus.get().getLoggedInStatus().equals(LoginStatus.ACTIVE)) {
                log.info("UserName:{} is Already LoggedIn", username);
                return true;
            }
        } else {
            log.info("First Else");
            Optional<NewUserRegister> user = userRepository.findByUserName(username);
            NewUserRegister newUserRegister = user.get();
            boolean result = user.isPresent() && newUserRegister.getPassword().equals(password);
            if (result) {
                log.info("Second If ");
                UserLoginStatus userLoginStatus = new UserLoginStatus
                        (newUserRegister.getUserId(), newUserRegister.getUserName(), LoginStatus.ACTIVE, LocalDateTime.now(), null);
                userLoginStatusRepo.save(userLoginStatus);
                return true;
            } else {
                log.info("Password incorrect");
                return false;
            }

//        if(loginStatus.isPresent()) {
//            if (loginStatus.get().getLoggedInStatus().equals(LoginStatus.ACTIVE)) {
//                log.info("First If ");
//                log.info("UserName:{} is Already LoggedIn", username);
//                return true;
//            } else {
//                log.info("First Else");
//                Optional<NewUserRegister> user = userRepository.findByUserName(username);
//                NewUserRegister newUserRegister = user.get();
//                boolean result = user.isPresent() && newUserRegister.getPassword().equals(password);
//                if (result) {
//                    log.info("Second If ");
//                    UserLoginStatus userLoginStatus = new UserLoginStatus
//                            (newUserRegister.getUserId(), newUserRegister.getUserName(), LoginStatus.ACTIVE, LocalDateTime.now(), null);
//                    userLoginStatusRepo.save(userLoginStatus);
//                    return true;
//                } else {
//                    log.info("Second Else");
//                    return false;
//                }
//            }
        }
        return false;
    }

    public ResponseEntity<TicketsResponse> confirmBooking(BookingRequest request) throws FeignException, PaymentFailedException {
        log.info("request in the UserService");
        if (checkUserIsActive(request.getUserId())) {
            String result = "";
            String bookingtype = request.getBookingMethod();
            TicketsResponse response = null;
            if (bookingtype.equals("Normal Reservation")) {
                response = bookingFeign.bookNormalReservation(request).getBody();
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            } else if (bookingtype.equals("Tatkal") || bookingtype.equals("Premium Tatkal")) {
                response = bookingFeign.bookPremiumAndTatkal(request).getBody();
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            }
        } else {
            throw new RuntimeException("User Not ACTIVE");
        }
        throw new RuntimeException("Check the request again");
    }

    public boolean checkUserIsActive(String userId) {
        Optional<UserLoginStatus> loginStatus = userLoginStatusRepo.findByUserId(userId);
        if (loginStatus.isPresent() && loginStatus.get().getLoggedInStatus().equals(LoginStatus.ACTIVE)) {
            return true;
        }
        return false;
    }

    public String createNewEWallet(String userId, String password) {
        if (checkUserIsActive(userId)) {
            Optional<NewUserRegister> user = userRepository.findByUserId(userId);
            return paymentFeign.createNewEWallet(userId, user.get().getUserId(), password);
        } else {
            return "User Not Found";
        }
    }

    public String addMoneyToEWallet(String userId, double amount) {
        if (checkUserIsActive(userId)) {
            Optional<NewUserRegister> user = userRepository.findByUserId(userId);
            return paymentFeign.addMoneyToEWallet(user.get().getUserId(), amount);
        } else {
            return "User Not Found";
        }
    }

    public ResponseEntity<String> bookingCancelRequest(BookingCancelRequestDTO bookingCancelRequestDTO) {
        return bookingFeign.bookingCancelRequest(bookingCancelRequestDTO);
    }

    public ResponseEntity<List<TicketDTO>> getTrainForNormalBookingByTrainNumber(TrainDetailsRequest request) {
        log.info("In the UserService");
        List<TicketDTO> ticketDTO = bookingFeign.getTrainForNormalBookingByTrainNumber(request);
        return new ResponseEntity<>(ticketDTO, HttpStatus.FOUND);
    }

    public ResponseEntity<List<TicketDTO>> getTrainForTatkalBookingByTrainNumber(TrainDetailsRequest request) {
        List<TicketDTO> ticketDTO = bookingFeign.getTrainForTatkalBookingByTrainNumber(request);
        return new ResponseEntity<>(ticketDTO, HttpStatus.FOUND);
    }

    public ResponseEntity<List<TicketDTO>> getTrainForPremiumTatkalBookingByTrainNumber(TrainDetailsRequest request) {
        List<TicketDTO> ticketDTO = bookingFeign.getTrainForPremiumTatkalBookingByTrainNumber(request);
        return new ResponseEntity<>(ticketDTO, HttpStatus.FOUND);
    }

    public ResponseEntity<String> confirmOrCancelRequest(ConfirmOrCancelRequest request) {
        return bookingFeign.confirmOrCancelRequest(request);
    }

//    public String addEWallet(String username, EWalletDTO eWallet){
//        Optional<NewUserRegister> user = userRepository.findByUserName(username);
//        user.get().setEWallet(eWallet);
//        eWalletRepo.save(eWallet);
//        return "Success";
//    }


}