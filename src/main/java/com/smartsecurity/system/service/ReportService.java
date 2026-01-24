package com.smartsecurity.system.service;

import com.smartsecurity.system.enums.VehicleStatus;
import com.smartsecurity.system.enums.VisitStatus;
import com.smartsecurity.system.repository.TenantRepository;
import com.smartsecurity.system.repository.VehicleHistoryRepository;
import com.smartsecurity.system.repository.VehicleRepository;
import com.smartsecurity.system.repository.VisitorHistoryRepository;
import com.smartsecurity.system.repository.VisitorRepository;
import com.smartsecurity.system.entity.VehicleHistory;
import com.smartsecurity.system.entity.VisitorHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final VisitorRepository visitorRepository;
    private final VehicleRepository vehicleRepository;
    private final TenantRepository tenantRepository;
    private final VehicleHistoryRepository vehicleHistoryRepository;
    private final VisitorHistoryRepository visitorHistoryRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTenants", tenantRepository.count());
        stats.put("totalVehicles", vehicleRepository.count());
        List<VisitStatus> activeStatuses = List.of(VisitStatus.CHECKED_IN, VisitStatus.CHECKED_OUT);
        stats.put("visitorsToday", visitorRepository.countByVisitDateAndStatusIn(
                LocalDate.now(),
                activeStatuses));
        stats.put("vehiclesInside",
                vehicleRepository.countByStatus(VehicleStatus.CHECKED_IN));
        return stats;
    }

    @Transactional(readOnly = true)
    public List<VisitorHistory> getVisitorReport(LocalDate startDate, LocalDate endDate, Long tenantId) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();
        return visitorHistoryRepository.findByFilters(tenantId, start, end);
    }

    @Transactional(readOnly = true)
    public List<VehicleHistory> getVehicleReport(LocalDate startDate, LocalDate endDate, Long tenantId) {
        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        return vehicleHistoryRepository.findByFilters(tenantId, start, end);
    }

}
