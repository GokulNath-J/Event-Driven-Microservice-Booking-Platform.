package com.example.User_Service.Repository;

import com.example.User_Service.Entity.UserLoginStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLoginStatusRepo extends JpaRepository<UserLoginStatus,String> {
    Optional<UserLoginStatus> findByUserName(String username);

    Optional<UserLoginStatus> findByUserId(String userId);
}
