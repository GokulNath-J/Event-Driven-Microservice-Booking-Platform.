package com.example.Payment_Service.Repository;

import com.example.Payment_Service.Entity.EWalletDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EWalletDetailsRepo extends JpaRepository<EWalletDetails,Integer> {

    Optional<EWalletDetails> findByUserId(String userId);

    EWalletDetails findByUserName(String userName);
}
