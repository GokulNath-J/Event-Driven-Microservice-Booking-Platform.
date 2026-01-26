package com.example.User_Service.Repository;

import com.example.User_Service.Entity.NewUserRegister;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<NewUserRegister, Long> {

    Optional<NewUserRegister> findByUserName(String username);

    Optional<NewUserRegister> findByUserId(String userId);
}