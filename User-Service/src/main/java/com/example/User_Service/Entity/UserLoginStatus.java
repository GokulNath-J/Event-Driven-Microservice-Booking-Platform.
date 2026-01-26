package com.example.User_Service.Entity;

import com.example.User_Service.DTO.BookingStatus;
import com.example.User_Service.DTO.LoginStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginStatus {

    @Id
    private String userId;
    private String userName;

    @Enumerated(EnumType.STRING)
    private LoginStatus loggedInStatus = LoginStatus.INACTIVE;
    private LocalDateTime loggedInDateTime;
    private LocalDateTime loggedOutDateTime;


}
