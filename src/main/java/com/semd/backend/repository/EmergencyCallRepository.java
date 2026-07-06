package com.semd.backend.repository;

import com.semd.backend.entity.EmergencyCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface EmergencyCallRepository extends JpaRepository<EmergencyCall, Integer> {
    
    @Query("SELECT c.audioUrl FROM EmergencyCall c WHERE c.audioUrl IS NOT NULL")
    List<String> findAllAudioUrls();

    List<EmergencyCall> findByReporterPhoneOrderByCallStartTimeDesc(String reporterPhone);

    @Query("SELECT c FROM EmergencyCall c WHERE c.audioUrl LIKE %:objectKey%")
    java.util.Optional<EmergencyCall> findFirstByAudioUrlContaining(@org.springframework.data.repository.query.Param("objectKey") String objectKey);
}


