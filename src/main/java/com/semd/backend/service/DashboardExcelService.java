package com.semd.backend.service;

import com.semd.backend.dto.dashboard.DashboardResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
public class DashboardExcelService {
    public byte[] export(String role, DashboardResponse data) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = wb.createCellStyle();
            Font font = wb.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(font); header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Sheet summary = wb.createSheet("Summary");
            int row = 0;
            row(summary,row++,"Role",role,header);
            row(summary,row++,"From",data.filters().from(),header);
            row(summary,row++,"To",data.filters().to(),header);
            row(summary,row++,"Timezone",data.filters().timezone(),header);
            row(summary,row++,"Generated At",data.meta().generatedAt(),header);
            row++;
            for (var e:data.kpis().entrySet()) row(summary,row++,e.getKey(),e.getValue(),header);
            write(wb,"Trend",data.series(),header);
            for(var e:data.details().entrySet()) write(wb,safe(e.getKey()),e.getValue(),header);
            wb.write(out); return out.toByteArray();
        } catch (Exception e) { throw new IllegalStateException("Không thể tạo Excel dashboard",e); }
    }
    private void row(Sheet s,int i,String k,Object v,CellStyle h){Row r=s.createRow(i);Cell c=r.createCell(0);c.setCellValue(k);c.setCellStyle(h);value(r.createCell(1),v);s.setColumnWidth(0,32*256);s.setColumnWidth(1,36*256);}
    private void write(Workbook wb,String name,List<Map<String,Object>> rows,CellStyle h){
        Sheet s=wb.createSheet(name);if(rows.isEmpty()){s.createRow(0).createCell(0).setCellValue("Không có dữ liệu");return;}
        List<String> keys=new ArrayList<>(rows.getFirst().keySet());Row hr=s.createRow(0);
        for(int i=0;i<keys.size();i++){Cell c=hr.createCell(i);c.setCellValue(keys.get(i));c.setCellStyle(h);}
        int ri=1;for(var item:rows){Row r=s.createRow(ri++);for(int i=0;i<keys.size();i++)value(r.createCell(i),item.get(keys.get(i)));}
        s.createFreezePane(0,1);for(int i=0;i<keys.size();i++){s.autoSizeColumn(i);s.setColumnWidth(i,Math.min(s.getColumnWidth(i)+512,50*256));}
    }
    private void value(Cell c,Object v){if(v==null)c.setBlank();else if(v instanceof Number n)c.setCellValue(n.doubleValue());else c.setCellValue(v.toString());}
    private String safe(String n){String s=org.apache.poi.ss.util.WorkbookUtil.createSafeSheetName(n.replaceAll("([a-z])([A-Z])","$1 $2"));return s.length()>31?s.substring(0,31):s;}
}
