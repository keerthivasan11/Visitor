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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

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
                List<VisitStatus> activeStatuses = List.of(VisitStatus.CHECKED_IN,
                                VisitStatus.CHECKED_OUT);
                stats.put("visitorsToday", visitorRepository.countByVisitDateAndStatusIn(
                                LocalDate.now(),
                                activeStatuses));
                stats.put("vehiclesInside",
                                vehicleRepository.countByStatus(VehicleStatus.CHECKED_IN));
                return stats;
        }

        // Visitor Chart

        public Map<String, Object> getVisitorCharts() {

                Map<String, Object> response = new HashMap<>();

                response.put("5D", getDailyVisitors());
                response.put("1M", getWeeklyVisitors());
                response.put("6M", getMonthlyVisitors());

                return response;
        }

        private List<Map<String, Object>> getDailyVisitors() {

                LocalDate end = LocalDate.now();
                LocalDate start = end.minusDays(4); // last 5 days including today

                // DB result → Map<LocalDate, Count>
                Map<LocalDate, Long> dbData = visitorRepository
                                .countDailyVisitors(start)
                                .stream()
                                .collect(Collectors.toMap(
                                                r -> (LocalDate) r[0],
                                                r -> ((Number) r[1]).longValue()));

                List<Map<String, Object>> result = new ArrayList<>();

                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

                        Map<String, Object> map = new HashMap<>();
                        map.put("date", date.toString());
                        map.put("value", dbData.getOrDefault(date, 0L));

                        result.add(map);
                }

                return result;
        }

        private List<Map<String, Object>> getWeeklyVisitors() {

                LocalDate today = LocalDate.now();

                // current week start (Monday)
                LocalDate endWeekStart = today
                                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

                // go back 3 weeks → total = 4 weeks
                LocalDate startWeekStart = endWeekStart.minusWeeks(3);

                Map<LocalDate, Long> dbData = visitorRepository
                                .countWeeklyVisitors(startWeekStart)
                                .stream()
                                .collect(Collectors.toMap(
                                                r -> (LocalDate) r[0],
                                                r -> ((Number) r[1]).longValue()));

                List<Map<String, Object>> result = new ArrayList<>();

                for (LocalDate weekStart = startWeekStart; !weekStart.isAfter(endWeekStart); weekStart = weekStart
                                .plusWeeks(1)) {

                        LocalDate weekEnd = weekStart.plusDays(6);

                        Map<String, Object> map = new HashMap<>();
                        map.put("week", weekStart + " - " + weekEnd);
                        map.put("value", dbData.getOrDefault(weekStart, 0L));

                        result.add(map);
                }

                return result;
        }

        private List<Map<String, Object>> getMonthlyVisitors() {

                // last 6 months including current
                YearMonth end = YearMonth.now();
                YearMonth start = end.minusMonths(5);

                // DB result → Map<YearMonth, Count>
                Map<YearMonth, Long> dbData = visitorRepository
                                .countMonthlyVisitors(start.atDay(1))
                                .stream()
                                .collect(Collectors.toMap(
                                                r -> YearMonth.of(
                                                                ((Number) r[0]).intValue(),
                                                                ((Number) r[1]).intValue()),
                                                r -> ((Number) r[2]).longValue()));

                // Final result (always 6 entries)
                List<Map<String, Object>> result = new ArrayList<>();

                for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {

                        Map<String, Object> map = new HashMap<>();
                        map.put("month", ym.toString()); // 2026-01
                        map.put("value", dbData.getOrDefault(ym, 0L));

                        result.add(map);
                }

                return result;
        }

        // vehicle chart
        public Map<String, Object> getVehicleCharts() {

                Map<String, Object> response = new HashMap<>();

                response.put("5D", getDailyVehicles());
                response.put("1M", getWeeklyVehicles());
                response.put("6M", getMonthlyVehicles());

                return response;
        }

        private List<Map<String, Object>> getDailyVehicles() {

                LocalDate end = LocalDate.now();
                LocalDate start = end.minusDays(4); // last 5 days

                // DB result → Map<LocalDate, Count>
                Map<LocalDate, Long> dbData = vehicleRepository
                                .countDailyVehicles(start.atStartOfDay())
                                .stream()
                                .collect(Collectors.toMap(
                                                r -> toLocalDate(r[0]),
                                                r -> ((Number) r[1]).longValue()));

                List<Map<String, Object>> result = new ArrayList<>();

                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

                        Map<String, Object> map = new HashMap<>();
                        map.put("date", date.toString());
                        map.put("value", dbData.getOrDefault(date, 0L));

                        result.add(map);
                }

                return result;
        }

        private List<Map<String, Object>> getWeeklyVehicles() {

                LocalDate today = LocalDate.now();

                // current week (Monday)
                LocalDate endWeekStart = today
                                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

                // last 4 weeks only
                LocalDate startWeekStart = endWeekStart.minusWeeks(3);

                // DB result → Map<weekStart, count>
                Map<LocalDate, Long> dbData = vehicleRepository
                                .countWeeklyVehicles(startWeekStart.atStartOfDay())
                                .stream()
                                .collect(Collectors.toMap(
                                                r -> toLocalDate(r[0]),
                                                r -> ((Number) r[1]).longValue()));

                List<Map<String, Object>> result = new ArrayList<>();

                for (LocalDate weekStart = startWeekStart; !weekStart.isAfter(endWeekStart); weekStart = weekStart
                                .plusWeeks(1)) {

                        LocalDate weekEnd = weekStart.plusDays(6);

                        Map<String, Object> map = new HashMap<>();
                        map.put("week", weekStart + " - " + weekEnd);
                        map.put("value", dbData.getOrDefault(weekStart, 0L));

                        result.add(map);
                }

                return result;
        }

        private LocalDate toLocalDate(Object value) {

                if (value instanceof LocalDate ld) {
                        return ld;
                }

                if (value instanceof LocalDateTime ldt) {
                        return ldt.toLocalDate();
                }

                if (value instanceof java.sql.Date d) {
                        return d.toLocalDate();
                }

                if (value instanceof java.sql.Timestamp ts) {
                        return ts.toLocalDateTime().toLocalDate();
                }

                if (value instanceof String s) {

                        // yyyy-MM
                        if (s.matches("\\d{4}-\\d{2}")) {
                                return LocalDate.parse(s + "-01");
                        }

                        // yyyy-MM-dd
                        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
                                return LocalDate.parse(s);
                        }

                        // Feb 2026
                        try {
                                return YearMonth.parse(
                                                s,
                                                DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)).atDay(1);
                        } catch (DateTimeParseException ignored) {
                        }

                        // February 2026
                        try {
                                return YearMonth.parse(
                                                s,
                                                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)).atDay(1);
                        } catch (DateTimeParseException ignored) {
                        }

                        // Feb (assume current year)
                        try {
                                Month month = Month.valueOf(s.toUpperCase(Locale.ENGLISH));
                                return LocalDate.of(LocalDate.now().getYear(), month, 1);
                        } catch (Exception ignored) {
                        }
                }

                throw new IllegalArgumentException(
                                "Unsupported date type: " + value + " (" + value.getClass() + ")");
        }

        private List<Map<String, Object>> getMonthlyVehicles() {

                YearMonth end = YearMonth.now();
                YearMonth start = end.minusMonths(5);

                Map<YearMonth, Long> dbData = vehicleRepository
                                .countMonthlyVehicles(start.atDay(1).atStartOfDay())
                                .stream()
                                .collect(Collectors.toMap(
                                                r -> YearMonth.parse((String) r[0]),
                                                r -> ((Number) r[1]).longValue()));

                List<Map<String, Object>> result = new ArrayList<>();

                for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {

                        Map<String, Object> map = new HashMap<>();
                        map.put("month", ym.toString());
                        map.put("value", dbData.getOrDefault(ym, 0L));
                        result.add(map);
                }

                return result;
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
