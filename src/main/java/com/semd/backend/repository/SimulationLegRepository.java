package com.semd.backend.repository;

import com.semd.backend.entity.LegType;
import com.semd.backend.entity.SimulationLeg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimulationLegRepository extends JpaRepository<SimulationLeg, Long> {

    List<SimulationLeg> findBySimulationIdOrderBySequenceNo(Long simulationId);

    Optional<SimulationLeg> findBySimulationIdAndLegType(Long simulationId, LegType legType);
}