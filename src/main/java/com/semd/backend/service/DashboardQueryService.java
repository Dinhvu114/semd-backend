package com.semd.backend.service;

import com.semd.backend.dto.dashboard.DashboardFilter;
import com.semd.backend.dto.dashboard.DashboardResponse;
import com.semd.backend.entity.*;
import com.semd.backend.repository.*;
import com.semd.backend.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardQueryService {
    private static final Set<DispatchMissionStatus> ACTIVE_MISSIONS = EnumSet.of(
            DispatchMissionStatus.ACCEPTED, DispatchMissionStatus.EN_ROUTE,
            DispatchMissionStatus.ARRIVED_SCENE, DispatchMissionStatus.TRANSPORTING,
            DispatchMissionStatus.ARRIVED_HOSPITAL);
    private static final Set<DispatchRequestStatus> PROCESSING_REQUESTS = EnumSet.of(
            DispatchRequestStatus.CONFIRMED, DispatchRequestStatus.RECOMMENDING,
            DispatchRequestStatus.DISPATCHING, DispatchRequestStatus.DISPATCHED);
    private static final Set<DispatchResourceStatus> BUSY_RESOURCES = EnumSet.of(
            DispatchResourceStatus.DISPATCHED, DispatchResourceStatus.ON_MISSION,
            DispatchResourceStatus.RETURNING);

    private final UserRepository users;
    private final ProviderRepository providers;
    private final MedicalHospitalRepository hospitals;
    private final EmergencyCallRepository calls;
    private final DispatchRequestRepository requests;
    private final DispatchMissionRepository missions;
    private final DispatchResourceRepository resources;
    private final PaymentTransactionRepository payments;
    private final ProviderSettlementRepository settlements;

    public DashboardQueryService(UserRepository users, ProviderRepository providers,
            MedicalHospitalRepository hospitals, EmergencyCallRepository calls,
            DispatchRequestRepository requests, DispatchMissionRepository missions,
            DispatchResourceRepository resources, PaymentTransactionRepository payments,
            ProviderSettlementRepository settlements) {
        this.users = users; this.providers = providers; this.hospitals = hospitals;
        this.calls = calls; this.requests = requests; this.missions = missions;
        this.resources = resources; this.payments = payments; this.settlements = settlements;
    }

    public DashboardResponse admin(DashboardFilter filter, UserPrincipal principal) {
        Period p = resolve(filter);
        Provider provider = filter.providerId() == null ? null : providers.findById(filter.providerId())
                .orElseThrow(() -> new IllegalArgumentException("providerId không tồn tại"));
        List<DispatchResource> rs = resources.findAll().stream()
                .filter(r -> provider == null || providerId(r) == provider.getId()).toList();
        Set<Integer> resourceIds = rs.stream().map(DispatchResource::getId).collect(Collectors.toSet());
        List<DispatchMission> ms = missions.findAll().stream()
                .filter(m -> provider == null || resourceIds.contains(m.getResource().getId()))
                .filter(m -> within(m.getDispatchedAt(), p)).toList();
        List<DispatchRequest> reqs = requests.findAll().stream()
                .filter(r -> within(r.getCreatedAt(), p)).toList();
        List<EmergencyCall> cs = calls.findAll().stream()
                .filter(c -> within(c.getCreatedAt(), p)).toList();
        List<PaymentTransaction> paid = payments.findAll().stream()
                .filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus()))
                .filter(t -> provider == null || t.getProvider() != null && t.getProvider().getId().equals(provider.getId()))
                .filter(t -> within(paymentTime(t), p)).toList();

        Map<String,Object> kpi = map(
                "totalUsers", users.count(), "totalProviders", providers.count(),
                "totalHospitals", hospitals.count(), "totalEmergencyCalls", (long) cs.size(),
                "totalDispatchRequests", (long) reqs.size(), "totalMissions", (long) ms.size(),
                "availableResources", count(rs, r -> r.getStatus() == DispatchResourceStatus.AVAILABLE),
                "busyResources", count(rs, r -> BUSY_RESOURCES.contains(r.getStatus())),
                "missionSuccessRate", rate(count(ms, m -> m.getStatus() == DispatchMissionStatus.COMPLETED),
                        count(ms, m -> EnumSet.of(DispatchMissionStatus.COMPLETED,
                                DispatchMissionStatus.REJECTED, DispatchMissionStatus.CANCELLED).contains(m.getStatus()))),
                "platformRevenue", sum(paid, PaymentTransaction::getCommissionAmount));
        Map<String,List<Map<String,Object>>> breakdowns = new LinkedHashMap<>();
        breakdowns.put("requestStatus", group(reqs, r -> r.getStatus().name()));
        breakdowns.put("requestUrgency", group(reqs, this::urgency));
        breakdowns.put("resourceStatus", group(rs, r -> r.getStatus().name()));
        breakdowns.put("missionStatus", group(ms, m -> m.getStatus().name()));
        Map<String,List<Map<String,Object>>> details = new LinkedHashMap<>();
        details.put("missionDetails", missionDetails(ms));
        details.put("providerPerformance", providerPerformance(ms));
        return response(p, filter, provider, map("type", provider == null ? "SYSTEM" : "PROVIDER_FILTER"),
                kpi, trend(p, reqs, ms), breakdowns, details);
    }

    public DashboardResponse dispatcher(DashboardFilter filter, UserPrincipal principal) {
        rejectProviderId(filter);
        Period p = resolve(filter);
        List<DispatchRequest> reqs = requests.findAll().stream()
                .filter(r -> r.getCreatedByDispatcher() != null
                        && Objects.equals(r.getCreatedByDispatcher().getId(), principal.getId()))
                .filter(r -> within(r.getCreatedAt(), p)).toList();
        Set<Integer> requestIds = reqs.stream().map(DispatchRequest::getId).collect(Collectors.toSet());
        List<DispatchMission> ms = missions.findAll().stream()
                .filter(m -> requestIds.contains(m.getRequest().getId())).toList();
        List<DispatchResource> rs = resources.findAll();
        Map<String,Object> kpi = map(
                "totalRequests", (long) reqs.size(),
                "pendingRequests", count(reqs, r -> r.getStatus() == DispatchRequestStatus.PENDING),
                "processingRequests", count(reqs, r -> PROCESSING_REQUESTS.contains(r.getStatus())),
                "completedRequests", count(reqs, r -> r.getStatus() == DispatchRequestStatus.COMPLETED),
                // "failedRequests", count(reqs, r -> EnumSet.of(DispatchRequestStatus.REJECTED,
                //         DispatchRequestStatus.CANCELLED, DispatchRequestStatus.FAILED).contains(r.getStatus())),
                "criticalPendingRequests", count(reqs, r -> r.getStatus() == DispatchRequestStatus.PENDING
                        && "CRITICAL".equalsIgnoreCase(urgency(r))),
                "availableResources", count(rs, r -> r.getStatus() == DispatchResourceStatus.AVAILABLE),
                "averageDispatchTimeSeconds", average(ms, m -> m.getRequest().getCreatedAt(), DispatchMission::getDispatchedAt));
        Map<String,List<Map<String,Object>>> breakdowns = new LinkedHashMap<>();
        breakdowns.put("requestStatus", group(reqs, r -> r.getStatus().name()));
        breakdowns.put("requestUrgency", group(reqs, this::urgency));
        breakdowns.put("resourceStatus", group(rs, r -> r.getStatus().name()));
        return response(p, filter, null, map("type","DISPATCHER","userId",principal.getId()),
                kpi, trend(p, reqs, ms), breakdowns,
                mapList("requestDetails", requestDetails(reqs, ms), "resourceSnapshot", resourceDetails(rs)));
    }

    public DashboardResponse driver(DashboardFilter filter, UserPrincipal principal) {
        rejectProviderId(filter);
        Period p = resolve(filter);
        List<DispatchMission> ms = missions.findAll().stream()
                .filter(m -> m.getResource().getCurrentDriver() != null
                        && Objects.equals(m.getResource().getCurrentDriver().getId(), principal.getId()))
                .filter(m -> within(m.getDispatchedAt(), p)).toList();
        long accepted = count(ms, m -> m.getAcceptedAt() != null);
        long rejected = count(ms, m -> m.getStatus() == DispatchMissionStatus.REJECTED);
        Map<String,Object> kpi = map(
                "assignedMissions", (long) ms.size(),
                "pendingMissions", count(ms, m -> m.getStatus() == DispatchMissionStatus.DISPATCHED),
                "inProgressMissions", count(ms, m -> ACTIVE_MISSIONS.contains(m.getStatus())),
                "completedMissions", count(ms, m -> m.getStatus() == DispatchMissionStatus.COMPLETED),
                "rejectedMissions", rejected,
                "acceptanceRate", rate(accepted, accepted + rejected),
                "averageResponseTimeSeconds", average(ms, DispatchMission::getDispatchedAt, DispatchMission::getAcceptedAt),
                "averageMissionDurationSeconds", average(ms, DispatchMission::getAcceptedAt, DispatchMission::getCompletedAt));
        Map<String,List<Map<String,Object>>> breakdowns = mapList(
                "missionStatus", group(ms, m -> m.getStatus().name()),
                "missionUrgency", group(ms, m -> urgency(m.getRequest())));
        return response(p, filter, null, map("type","DRIVER","userId",principal.getId()),
                kpi, missionTrend(p, ms), breakdowns, mapList("missionDetails", missionDetails(ms)));
    }

    public DashboardResponse provider(DashboardFilter filter, UserPrincipal principal) {
        rejectProviderId(filter);
        Period p = resolve(filter);
        User current = users.findById(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy người dùng"));
        Provider provider = Optional.ofNullable(current.getProvider())
                .orElseThrow(() -> new AccessDeniedException("Tài khoản chưa được gắn provider"));
        List<DispatchResource> rs = resources.findAll().stream()
                .filter(r -> providerId(r) == provider.getId()).toList();
        Set<Integer> resourceIds = rs.stream().map(DispatchResource::getId).collect(Collectors.toSet());
        List<DispatchMission> ms = missions.findAll().stream()
                .filter(m -> resourceIds.contains(m.getResource().getId()))
                .filter(m -> within(m.getDispatchedAt(), p)).toList();
        List<PaymentTransaction> tx = payments.findAll().stream()
                .filter(t -> t.getProvider() != null && t.getProvider().getId().equals(provider.getId()))
                .filter(t -> within(paymentTime(t), p)).toList();
        List<ProviderSettlement> ss = settlements.findAll().stream()
                .filter(s -> s.getProvider().getId().equals(provider.getId())).toList();
        BigDecimal revenue = sum(tx.stream().filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus())).toList(),
                PaymentTransaction::getAmount);
        BigDecimal commission = sum(tx.stream().filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus())).toList(),
                PaymentTransaction::getCommissionAmount);
        long available = count(rs, r -> r.getStatus() == DispatchResourceStatus.AVAILABLE);
        long busy = count(rs, r -> BUSY_RESOURCES.contains(r.getStatus()));
        Map<String,Object> kpi = map(
                "totalAmbulances", (long) rs.size(), "availableAmbulances", available,
                "busyAmbulances", busy,
                "maintenanceAmbulances", count(rs, r -> r.getStatus() == DispatchResourceStatus.MAINTENANCE),
                "totalDrivers", rs.stream().map(DispatchResource::getCurrentDriver).filter(Objects::nonNull)
                        .map(User::getId).distinct().count(),
                "completedMissions", count(ms, m -> m.getStatus() == DispatchMissionStatus.COMPLETED),
                "collectedRevenue", revenue, "platformFees", commission,
                "netRevenue", revenue.subtract(commission),
                "fleetUtilization", rate(busy, available + busy),
                "pendingSettlement", ss.stream().filter(s -> "PENDING".equalsIgnoreCase(s.getStatus()))
                        .map(ProviderSettlement::getTotalCommission).filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        Map<String,List<Map<String,Object>>> breakdowns = mapList(
                "fleetStatus", group(rs, r -> r.getStatus().name()),
                "missionStatus", group(ms, m -> m.getStatus().name()),
                "paymentStatus", group(tx, PaymentTransaction::getStatus));
        return response(p, filter, provider, map("type","PROVIDER","providerId",provider.getId()),
                kpi, missionTrend(p, ms), breakdowns,
                mapList("missionDetails", missionDetails(ms), "fleetStatus", resourceDetails(rs),
                        "revenueLedger", paymentDetails(tx), "settlements", settlementDetails(ss)));
    }

    private DashboardResponse response(Period p, DashboardFilter f, Provider provider, Map<String,Object> scope,
            Map<String,Object> kpis, List<Map<String,Object>> series,
            Map<String,List<Map<String,Object>>> breakdowns, Map<String,List<Map<String,Object>>> details) {
        return new DashboardResponse(new DashboardResponse.Meta(Instant.now(), p.zone.getId(), scope),
                new DashboardResponse.ResolvedFilter(p.from,p.to,p.zone.getId(),
                        provider == null ? f.providerId() : provider.getId(),p.granularity),
                kpis,series,breakdowns,details);
    }
    private Period resolve(DashboardFilter f) {
        ZoneId zone;
        try { zone = ZoneId.of(f.timezone()==null||f.timezone().isBlank()?"Asia/Ho_Chi_Minh":f.timezone()); }
        catch (DateTimeException e) { throw new IllegalArgumentException("timezone không hợp lệ"); }
        LocalDateTime from=f.from()==null?LocalDate.now(zone).atStartOfDay():f.from();
        LocalDateTime to=f.to()==null?LocalDateTime.now(zone):f.to();
        if(from.isAfter(to)) throw new IllegalArgumentException("from phải trước hoặc bằng to");
        String g=f.granularity()==null?"AUTO":f.granularity().toUpperCase();
        if(!Set.of("AUTO","HOUR","DAY","WEEK","MONTH").contains(g)) throw new IllegalArgumentException("granularity không hợp lệ");
        if(g.equals("AUTO")) { long d=ChronoUnit.DAYS.between(from.toLocalDate(),to.toLocalDate()); g=d<=2?"HOUR":d<=62?"DAY":d<=370?"WEEK":"MONTH"; }
        return new Period(from,to,zone,g);
    }
    private void rejectProviderId(DashboardFilter f){if(f.providerId()!=null)throw new IllegalArgumentException("providerId chỉ dành cho ADMIN");}
    private boolean within(LocalDateTime t,Period p){return t!=null&&!t.isBefore(p.from)&&!t.isAfter(p.to);}
    private int providerId(DispatchResource r){return r.getProvider()==null?-1:r.getProvider().getId();}
    private String urgency(DispatchRequest r){return r.getConfirmedUrgencyLevel()!=null?r.getConfirmedUrgencyLevel():Objects.toString(r.getUrgencyLevel(),"UNKNOWN");}
    private LocalDateTime paymentTime(PaymentTransaction t){return t.getPaidAt()!=null?t.getPaidAt():t.getCreatedAt();}
    private <T> long count(List<T> list, java.util.function.Predicate<T> p){return list.stream().filter(p).count();}
    private BigDecimal rate(long n,long d){return d==0?BigDecimal.ZERO:BigDecimal.valueOf(n*100d/d).setScale(2,RoundingMode.HALF_UP);}
    private <T> long average(List<T> list,Function<T,LocalDateTime> a,Function<T,LocalDateTime>b){return Math.round(list.stream().filter(x->a.apply(x)!=null&&b.apply(x)!=null).mapToLong(x->Duration.between(a.apply(x),b.apply(x)).getSeconds()).filter(x->x>=0).average().orElse(0));}
    private <T> BigDecimal sum(List<T> list,Function<T,BigDecimal> f){return list.stream().map(f).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private <T> List<Map<String,Object>> group(List<T> list,Function<T,String> f){return list.stream().collect(Collectors.groupingBy(x->Objects.toString(f.apply(x),"UNKNOWN"),TreeMap::new,Collectors.counting())).entrySet().stream().map(e->map("key",e.getKey(),"count",e.getValue())).toList();}
    private List<Map<String,Object>> trend(Period p,List<DispatchRequest> r,List<DispatchMission>m){return buckets(p,b->map("bucketStart",b,"requests",count(r,x->bucket(x.getCreatedAt(),p).equals(b)),"missions",count(m,x->bucket(x.getDispatchedAt(),p).equals(b)),"completed",count(m,x->x.getStatus()==DispatchMissionStatus.COMPLETED&&x.getCompletedAt()!=null&&bucket(x.getCompletedAt(),p).equals(b))));}
    private List<Map<String,Object>> missionTrend(Period p,List<DispatchMission>m){return buckets(p,b->map("bucketStart",b,"assigned",count(m,x->bucket(x.getDispatchedAt(),p).equals(b)),"completed",count(m,x->x.getCompletedAt()!=null&&bucket(x.getCompletedAt(),p).equals(b))));}
    private List<Map<String,Object>> buckets(Period p,Function<LocalDateTime,Map<String,Object>> f){List<Map<String,Object>>o=new ArrayList<>();LocalDateTime c=bucket(p.from,p),e=bucket(p.to,p);while(!c.isAfter(e)&&o.size()<1000){o.add(f.apply(c));c=switch(p.granularity){case"HOUR"->c.plusHours(1);case"DAY"->c.plusDays(1);case"WEEK"->c.plusWeeks(1);default->c.plusMonths(1);};}return o;}
    private LocalDateTime bucket(LocalDateTime t,Period p){if(t==null)return LocalDateTime.MIN;return switch(p.granularity){case"HOUR"->t.truncatedTo(ChronoUnit.HOURS);case"DAY"->t.toLocalDate().atStartOfDay();case"WEEK"->t.toLocalDate().minusDays(t.getDayOfWeek().getValue()-1L).atStartOfDay();default->t.withDayOfMonth(1).toLocalDate().atStartOfDay();};}
    private List<Map<String,Object>> missionDetails(List<DispatchMission>ms){return ms.stream().map(m->map("missionId",m.getId(),"status",m.getStatus().name(),"urgency",urgency(m.getRequest()),"resourceCode",m.getResource().getResourceCode(),"driver",m.getResource().getCurrentDriver()==null?null:m.getResource().getCurrentDriver().getFullName(),"dispatchedAt",m.getDispatchedAt(),"acceptedAt",m.getAcceptedAt(),"completedAt",m.getCompletedAt(),"destination",m.getDestinationName())).toList();}
    private List<Map<String,Object>> resourceDetails(List<DispatchResource>rs){return rs.stream().map(r->map("resourceCode",r.getResourceCode(),"status",r.getStatus().name(),"driver",r.getCurrentDriver()==null?null:r.getCurrentDriver().getFullName(),"updatedAt",r.getUpdatedAt())).toList();}
    private List<Map<String,Object>> requestDetails(List<DispatchRequest>rs,List<DispatchMission>ms){Map<Integer,DispatchMission>by=ms.stream().collect(Collectors.toMap(x->x.getRequest().getId(),Function.identity(),(a,b)->a));return rs.stream().map(r->{DispatchMission m=by.get(r.getId());return map("requestId",r.getId(),"status",r.getStatus().name(),"urgency",urgency(r),"createdAt",r.getCreatedAt(),"missionId",m==null?null:m.getId());}).toList();}
    private List<Map<String,Object>> paymentDetails(List<PaymentTransaction>tx){return tx.stream().map(t->map("transactionId",t.getId(),"missionId",t.getMission()==null?null:t.getMission().getId(),"amount",t.getAmount(),"commission",t.getCommissionAmount(),"status",t.getStatus(),"paidAt",t.getPaidAt())).toList();}
    private List<Map<String,Object>> settlementDetails(List<ProviderSettlement>ss){return ss.stream().map(s->map("settlementId",s.getId(),"periodStart",s.getPeriodStart(),"periodEnd",s.getPeriodEnd(),"totalRevenue",s.getTotalRevenue(),"totalCommission",s.getTotalCommission(),"status",s.getStatus())).toList();}
    private List<Map<String,Object>> providerPerformance(List<DispatchMission>ms){return ms.stream().filter(m->m.getResource().getProvider()!=null).collect(Collectors.groupingBy(m->m.getResource().getProvider())).entrySet().stream().map(e->map("providerId",e.getKey().getId(),"providerName",e.getKey().getProviderName(),"missions",(long)e.getValue().size(),"completed",count(e.getValue(),m->m.getStatus()==DispatchMissionStatus.COMPLETED))).toList();}
    private Map<String,Object> map(Object...v){Map<String,Object>m=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)m.put((String)v[i],v[i+1]);return m;}
    @SuppressWarnings("unchecked") private Map<String,List<Map<String,Object>>> mapList(Object...v){Map<String,List<Map<String,Object>>>m=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)m.put((String)v[i],(List<Map<String,Object>>)v[i+1]);return m;}
    private record Period(LocalDateTime from,LocalDateTime to,ZoneId zone,String granularity){}
}
