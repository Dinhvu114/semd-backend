package com.semd.backend.repository;

import com.semd.backend.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Integer> {
    Optional<Provider> findByProviderName(String providerName);
    boolean existsByProviderName(String providerName);
    boolean existsByProviderNameAndIdNot(String providerName, Integer id);
}
