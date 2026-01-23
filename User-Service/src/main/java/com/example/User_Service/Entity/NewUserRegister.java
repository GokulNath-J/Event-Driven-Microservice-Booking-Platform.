package com.example.User_Service.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewUserRegister {

    @Id
    @SequenceGenerator(name = "users", sequenceName = "userseq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(generator = "users", strategy = GenerationType.SEQUENCE)
    private Long id;

    private String userId;

    @Column(nullable = false, unique = true)
    private String userName;

    @Column(nullable = false)
    private String password;

    private String email;

    @Column(nullable = false,unique = true, length = 10)
    private Long phoneNumber;

//    @OneToOne(cascade = CascadeType.ALL)
//    @JoinColumn(name = "EwalletNumber", referencedColumnName = "eWalletNumber")
//    private EWalletDTO eWallet;

    // You can add roles or other fields later
}