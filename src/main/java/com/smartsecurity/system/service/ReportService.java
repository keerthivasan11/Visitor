package com.smartsecurity.system.service;

import com.smartsecurity.system.enums.VehicleStatus;
import com.smartsecurity.system.enums.VisitStatus;
import com.smartsecurity.system.repository.TenantRepository;
import com.smartsecurity.system.repository.VehicleHistoryRepository;
import com.smartsecurity.system.repository.VehicleRepository;
import com.smartsecurity.system.repository.VisitorHistoryRepository;
import com.smartsecurity.system.repository.StaffHistoryRepository;
import com.smartsecurity.system.repository.VisitorRepository;
import com.smartsecurity.system.entity.VehicleHistory;
import com.smartsecurity.system.entity.VisitorHistory;
import com.smartsecurity.system.entity.StaffHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
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
    private final StaffHistoryRepository staffHistoryRepository;

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
    public Page<VisitorHistory> getVisitorReport(LocalDate startDate, LocalDate endDate, Long tenantId, int page,
            int size) {
        // Defensive pagination
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        // Default date range (last 3 months)
        LocalDate start = startDate != null
                ? startDate
                : LocalDate.now().minusMonths(3);

        LocalDate end = endDate != null
                ? endDate
                : LocalDate.now();

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("visitDate").descending());

        return visitorHistoryRepository.findByFilters(
                tenantId,
                start,
                end,
                pageable);

    }

    @Transactional(readOnly = true)
    public Page<VehicleHistory> getVehicleReport(
            LocalDate startDate,
            LocalDate endDate,
            Long tenantId,
            int page,
            int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.of(1970, 1, 1, 0, 0);

        LocalDateTime end = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("checkInTime").descending());

        return vehicleHistoryRepository.findByFilters(
                tenantId,
                start,
                end,
                pageable);
    }

    @Transactional(readOnly = true)
    public Page<StaffHistory> getStaffReport(
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size) {

        // Defensive pagination
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.of(1970, 1, 1, 0, 0);

        LocalDateTime end = (endDate != null)
                ? endDate.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("checkInTime").descending() // or correct staff time field
        );

        return staffHistoryRepository.findByFilters(
                start,
                end,
                pageable);
    }

}
