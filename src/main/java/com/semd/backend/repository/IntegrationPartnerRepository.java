package com.semd.backend.repository;

import com.semd.backend.entity.IntegrationPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegrationPartnerRepository extends JpaRepository<IntegrationPartner, Integer> {
    List<IntegrationPartner> findByPartnerTypeAndIsActiveTrue(String partnerType);
}
