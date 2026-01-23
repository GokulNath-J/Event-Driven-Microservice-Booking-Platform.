package com.example.User_Service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterNewUserDTO {

    private String userName;

    private String password;

    private String email;

    private Long phoneNumber;
}
