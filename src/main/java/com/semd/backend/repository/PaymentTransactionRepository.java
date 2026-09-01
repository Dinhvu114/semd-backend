package com.semd.backend.repository;

import com.semd.backend.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByMission_Id(Integer missionId);

    List<PaymentTransaction> findByPayer_IdOrderByCreatedAtDesc(Integer payerId);

    List<PaymentTransaction> findByDriver_IdOrderByCreatedAtDesc(Integer driverId);

    List<PaymentTransaction> findByProvider_IdOrderByCreatedAtDesc(Integer providerId);
}