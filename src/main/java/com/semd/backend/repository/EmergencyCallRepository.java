package com.semd.backend.repository;

import com.semd.backend.entity.EmergencyCall;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyCallRepository extends JpaRepository<EmergencyCall, Integer> {
    
    @Query("SELECT c.audioUrl FROM EmergencyCall c WHERE c.audioUrl IS NOT NULL")
    List<String> findAllAudioUrls();

    List<EmergencyCall> findByReporterPhoneOrderByCallStartTimeDesc(String reporterPhone);

    @Query("SELECT c FROM EmergencyCall c WHERE c.audioUrl LIKE %:objectKey%")
    java.util.Optional<EmergencyCall> findFirstByAudioUrlContaining(@org.springframework.data.repository.query.Param("objectKey") String objectKey);

    // Tìm call theo callId và reporter phone để check ownership
    @Query("SELECT c FROM EmergencyCall c WHERE c.id = :callId")
    Optional<EmergencyCall> findByIdForTracking(@Param("callId") Integer callId);
}


