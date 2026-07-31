package com.semd.backend.controller;

import com.semd.backend.dto.dashboard.*;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name="Dashboard",description="Dashboard thống kê và xuất Excel theo vai trò")
public class DashboardController {
    private final DashboardQueryService query;
    private final DashboardExcelService excel;
    public DashboardController(DashboardQueryService query,DashboardExcelService excel){this.query=query;this.excel=excel;}

    @GetMapping("/admin") @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary="Dashboard Admin")
    public DashboardResponse admin(@RequestParam(name="from",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime from,
      @RequestParam(name="to",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime to,
      @RequestParam(name="timezone",defaultValue="Asia/Ho_Chi_Minh")String timezone,
      @RequestParam(name="providerId",required=false)Integer providerId,
      @RequestParam(name="granularity",defaultValue="AUTO")String granularity,@AuthenticationPrincipal UserPrincipal p){
        return query.admin(filter(from,to,timezone,providerId,granularity),p);}
    @GetMapping("/admin/export") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> adminExport(@RequestParam(name="from",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime from,
      @RequestParam(name="to",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime to,
      @RequestParam(name="timezone",defaultValue="Asia/Ho_Chi_Minh")String timezone,
      @RequestParam(name="providerId",required=false)Integer providerId,
      @RequestParam(name="granularity",defaultValue="AUTO")String granularity,@AuthenticationPrincipal UserPrincipal p){
        return file("admin",query.admin(filter(from,to,timezone,providerId,granularity),p));}

    @GetMapping("/dispatcher") @PreAuthorize("hasRole('DISPATCHER')")
    public DashboardResponse dispatcher(@RequestParam(name="from",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime from,
      @RequestParam(name="to",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime to,
      @RequestParam(name="timezone",defaultValue="Asia/Ho_Chi_Minh")String timezone,
      @RequestParam(name="granularity",defaultValue="AUTO")String granularity,@AuthenticationPrincipal UserPrincipal p){
        return query.dispatcher(filter(from,to,timezone,null,granularity),p);}
    @GetMapping("/dispatcher/export") @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<byte[]> dispatcherExport(@RequestParam(name="from",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime from,
      @RequestParam(name="to",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime to,
      @RequestParam(name="timezone",defaultValue="Asia/Ho_Chi_Minh")String timezone,
      @RequestParam(name="granularity",defaultValue="AUTO")String granularity,@AuthenticationPrincipal UserPrincipal p){
        return file("dispatcher",query.dispatcher(filter(from,to,timezone,null,granularity),p));}

    @GetMapping("/driver") @PreAuthorize("hasRole('DRIVER')")
    public DashboardResponse driver(@RequestParam(name="from",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime from,
      @RequestParam(name="to",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime to,
      @RequestParam(name="timezone",defaultValue="Asia/Ho_Chi_Minh")String timezone,
      @RequestParam(name="granularity",defaultValue="AUTO")String granularity,@AuthenticationPrincipal UserPrincipal p){
        return query.driver(filter(from,to,timezone,null,granularity),p);}
    @GetMapping("/driver/export") @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<byte[]> driverExport(@RequestParam(name="from",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime from,
      @RequestParam(name="to",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime to,
      @RequestParam(name="timezone",defaultValue="Asia/Ho_Chi_Minh")String timezone,
      @RequestParam(name="granularity",defaultValue="AUTO")String granularity,@AuthenticationPrincipal UserPrincipal p){
        return file("driver",query.driver(filter(from,to,timezone,null,granularity),p));}

    @GetMapping("/provider") @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public DashboardResponse provider(@RequestParam(name="from",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime from,
      @RequestParam(name="to",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime to,
      @RequestParam(name="timezone",defaultValue="Asia/Ho_Chi_Minh")String timezone,
      @RequestParam(name="granularity",defaultValue="AUTO")String granularity,@AuthenticationPrincipal UserPrincipal p){
        return query.provider(filter(from,to,timezone,null,granularity),p);}
    @GetMapping("/provider/export") @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    public ResponseEntity<byte[]> providerExport(@RequestParam(name="from",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime from,
      @RequestParam(name="to",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime to,
      @RequestParam(name="timezone",defaultValue="Asia/Ho_Chi_Minh")String timezone,
      @RequestParam(name="granularity",defaultValue="AUTO")String granularity,@AuthenticationPrincipal UserPrincipal p){
        return file("provider",query.provider(filter(from,to,timezone,null,granularity),p));}

    private DashboardFilter filter(LocalDateTime f,LocalDateTime t,String z,Integer p,String g){return new DashboardFilter(f,t,z,p,g);}
    private ResponseEntity<byte[]> file(String role,DashboardResponse data){
        String name=role+"-dashboard_"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))+".xlsx";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(name,StandardCharsets.UTF_8).build().toString())
          .body(excel.export(role,data));}
}
