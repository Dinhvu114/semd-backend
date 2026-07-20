package com.semd.backend.repository;

import com.semd.backend.entity.MedicalHospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalHospitalRepository extends JpaRepository<MedicalHospital, Integer>, JpaSpecificationExecutor<MedicalHospital> {
}
