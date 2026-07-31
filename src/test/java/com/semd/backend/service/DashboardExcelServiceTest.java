package com.semd.backend.service;

import com.semd.backend.dto.dashboard.DashboardResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DashboardExcelServiceTest {
    @Test
    void exportsSameDashboardPayloadToWorkbook() throws Exception {
        DashboardResponse data = new DashboardResponse(
                new DashboardResponse.Meta(Instant.now(),"Asia/Ho_Chi_Minh",Map.of("type","ADMIN")),
                new DashboardResponse.ResolvedFilter(LocalDateTime.now().minusDays(1),LocalDateTime.now(),
                        "Asia/Ho_Chi_Minh",null,"DAY"),
                Map.of("totalMissions",3L),
                List.of(Map.of("bucketStart","2026-07-31","completed",2L)),
                Map.of(),
                Map.of("missionDetails",List.of(Map.of("missionId",1,"status","COMPLETED"))));
        byte[] bytes = new DashboardExcelService().export("admin",data);
        assertTrue(bytes.length>1000);
        try(XSSFWorkbook wb=new XSSFWorkbook(new ByteArrayInputStream(bytes))){
            assertNotNull(wb.getSheet("Summary"));
            assertNotNull(wb.getSheet("Trend"));
            assertNotNull(wb.getSheet("mission Details"));
        }
    }
}
