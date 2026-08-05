package com.semd.backend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalHospitalRequestJsonTest {

    @Test
    void deserializesSwaggerPayload() throws Exception {
        String json = """
                {
                  "hospitalName": "Bệnh viện Bệnh Nhiệt đới Trung ương Cơ sở 2",
                  "hospitalAddress": "Bệnh viện nhiệt đới cơ sở 2",
                  "longitude": 105.772338,
                  "latitude": 21.1303731,
                  "capabilities": {
                    "additionalProp1": "CARDIOLOGY",
                    "additionalProp2": "TRAUMA"
                  },
                  "contactPhone": "0395135099",
                  "isActive": true
                }
                """;

        MedicalHospitalRequest request =
                new ObjectMapper().readValue(json, MedicalHospitalRequest.class);

        assertEquals(105.772338, request.longitude());
        assertEquals(21.1303731, request.latitude());
        assertTrue(request.isActive());
    }
}
