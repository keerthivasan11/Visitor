package com.smartsecurity.system.service;

import com.smartsecurity.system.enums.VehicleStatus;
import com.smartsecurity.system.enums.VisitStatus;
import com.smartsecurity.system.repository.TenantRepository;
import com.smartsecurity.system.repository.VehicleHistoryRepository;
import com.smartsecurity.system.repository.VehicleRepository;
import com.smartsecurity.system.repository.VisitorHistoryRepository;
import com.smartsecurity.system.repository.StaffHistoryRepository;
import com.smartsecurity.system.repository.StaffRepository;
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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
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
import org.apache.poi.ss.usermodel.Sheet;
import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Cell;

@Service
@RequiredArgsConstructor
public class ReportService {

        private final VisitorRepository visitorRepository;
        private final VehicleRepository vehicleRepository;
        private final TenantRepository tenantRepository;
        private final VehicleHistoryRepository vehicleHistoryRepository;
        private final VisitorHistoryRepository visitorHistoryRepository;
        private final StaffHistoryRepository staffHistoryRepository;
        private final StaffRepository staffRepository;

        public Map<String, Object> getDashboardStats() {
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalTenants", tenantRepository.count());
                stats.put("totalVehicles", vehicleRepository.count());
                stats.put("totalStaffs", staffRepository.count());
                List<VisitStatus> activeStatuses = List.of(VisitStatus.CHECKED_IN,
                                VisitStatus.CHECKED_OUT);
                stats.put("visitorsToday", visitorRepository.countByVisitDateAndStatusIn(
                                LocalDate.now(),
                                activeStatuses));
                stats.put("vehiclesInside",
                                vehicleRepository.countByStatus(VehicleStatus.CHECKED_IN));
                stats.put("staffsInside",
                                staffRepository.countByStatus(VisitStatus.CHECKED_IN));
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
                Map<LocalDate, Long> dbData = visitorHistoryRepository
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

                Map<LocalDate, Long> dbData = visitorHistoryRepository
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
                Map<YearMonth, Long> dbData = visitorHistoryRepository
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
                Map<LocalDate, Long> dbData = vehicleHistoryRepository
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
                Map<LocalDate, Long> dbData = vehicleHistoryRepository
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

                Map<YearMonth, Long> dbData = vehicleHistoryRepository
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

        @Transactional(readOnly = true)
        public byte[] exportVisitorReport(
                        LocalDate startDate,
                        LocalDate endDate,
                        Long tenantId) throws Exception {

                List<VisitorHistory> list = visitorHistoryRepository.findByFiltersWithoutPagination(
                                tenantId, startDate, endDate);

                Workbook workbook = new SXSSFWorkbook();
                Sheet sheet = workbook.createSheet("Visitor Report");

                SXSSFSheet sxSheet = (SXSSFSheet) sheet;
                sxSheet.trackAllColumnsForAutoSizing();

                // Date format style
                CellStyle dateTimeStyle = workbook.createCellStyle();
                CreationHelper createHelper = workbook.getCreationHelper();
                dateTimeStyle.setDataFormat(
                                createHelper.createDataFormat()
                                                .getFormat("yyyy-mm-dd hh:mm"));

                // Header
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Visitor Name");
                header.createCell(1).setCellValue("Company");
                header.createCell(2).setCellValue("Visit Type");
                header.createCell(3).setCellValue("Status");
                header.createCell(4).setCellValue("Checked In");
                header.createCell(5).setCellValue("Checked Out");

                int rowNum = 1;

                for (VisitorHistory v : list) {

                        Row row = sheet.createRow(rowNum++);

                        row.createCell(0)
                                        .setCellValue(v.getVisitorName());

                        row.createCell(1)
                                        .setCellValue(
                                                        v.getTenant() != null
                                                                        ? v.getTenant().getCompanyName()
                                                                        : "-");

                        row.createCell(2)
                                        .setCellValue(v.getVisitType());

                        row.createCell(3)
                                        .setCellValue(
                                                        v.getStatus() != null
                                                                        ? v.getStatus().name()
                                                                        : "-");

                        // Check-in
                        if (v.getCheckInTime() != null) {
                                Cell cell = row.createCell(4);
                                cell.setCellValue(
                                                java.sql.Timestamp.valueOf(
                                                                v.getCheckInTime()));
                                cell.setCellStyle(dateTimeStyle);
                        }

                        // Check-out
                        if (v.getCheckOutTime() != null) {
                                Cell cell = row.createCell(5);
                                cell.setCellValue(
                                                java.sql.Timestamp.valueOf(
                                                                v.getCheckOutTime()));
                                cell.setCellStyle(dateTimeStyle);
                        }
                }

                // Auto size columns
                for (int i = 0; i < 6; i++) {
                        sheet.autoSizeColumn(i);
                }

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                workbook.write(bos);
                workbook.close();

                return bos.toByteArray();
        }

        @Transactional(readOnly = true)
        public byte[] exportVehicleReport(
                        LocalDate startDate,
                        LocalDate endDate,
                        Long tenantId) throws Exception {

                LocalDate start = (startDate != null)
                                ? startDate
                                : LocalDate.now().minusMonths(3);

                LocalDate end = (endDate != null)
                                ? endDate
                                : LocalDate.now();
                // Convert to LocalDateTime
                LocalDateTime startDateTime = start.atStartOfDay(); // 00:00
                LocalDateTime endDateTime = end.atTime(23, 59, 59); // End of day

                List<VehicleHistory> list = vehicleHistoryRepository.findByFiltersWithoutPagination(
                                tenantId, startDateTime, endDateTime);

                try (Workbook workbook = new SXSSFWorkbook();
                                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                        Sheet sheet = workbook.createSheet("Vehicle Report");

                        // Header Row
                        Row header = sheet.createRow(0);
                        String[] columns = {
                                        "Vehicle Number",
                                        "Type",
                                        "Driver Name",
                                        "Company",
                                        "Purpose",
                                        "Status",
                                        "Check In",
                                        "Check Out"
                        };

                        for (int i = 0; i < columns.length; i++) {
                                header.createCell(i).setCellValue(columns[i]);
                        }

                        // Data Rows
                        int rowNum = 1;
                        for (VehicleHistory v : list) {
                                Row row = sheet.createRow(rowNum++);

                                row.createCell(0).setCellValue(
                                                v.getVehicleNumber() != null ? v.getVehicleNumber() : "");

                                row.createCell(1).setCellValue(
                                                v.getVehicleType() != null ? v.getVehicleType().toString() : "");

                                row.createCell(2).setCellValue(
                                                v.getDriverName() != null ? v.getDriverName() : "");

                                row.createCell(3).setCellValue(
                                                v.getTenant() != null ? v.getTenant().getCompanyName() : "");

                                row.createCell(4).setCellValue(
                                                v.getPurpose() != null ? v.getPurpose() : "");

                                row.createCell(5).setCellValue(
                                                v.getStatus() != null ? v.getStatus().toString() : "");

                                row.createCell(6).setCellValue(
                                                v.getCheckInTime() != null ? v.getCheckInTime().toString() : "");

                                row.createCell(7).setCellValue(
                                                v.getCheckOutTime() != null ? v.getCheckOutTime().toString() : "");
                        }

                        workbook.write(bos);
                        return bos.toByteArray();
                }
        }

        @Transactional(readOnly = true)
        public byte[] exportStaffReport(
                        LocalDate startDate,
                        LocalDate endDate) throws Exception {

                LocalDate start = (startDate != null)
                                ? startDate
                                : LocalDate.now().minusMonths(3);

                LocalDate end = (endDate != null)
                                ? endDate
                                : LocalDate.now();

                // Convert to LocalDateTime
                LocalDateTime startDateTime = start.atStartOfDay(); // 00:00
                LocalDateTime endDateTime = end.atTime(23, 59, 59); // End of day

                List<StaffHistory> list = staffHistoryRepository.findByFiltersWithoutPagination(
                                startDateTime, endDateTime);

                try (Workbook workbook = new SXSSFWorkbook();
                                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                        Sheet sheet = workbook.createSheet("Staff Report");

                        // Header
                        Row header = sheet.createRow(0);
                        String[] columns = {
                                        "Staff Name",
                                        "Emp ID",
                                        "Phone",
                                        "Status",
                                        "Check In",
                                        "Check Out"
                        };

                        for (int i = 0; i < columns.length; i++) {
                                header.createCell(i).setCellValue(columns[i]);
                        }

                        // Data
                        int rowNum = 1;
                        for (StaffHistory v : list) {
                                Row row = sheet.createRow(rowNum++);

                                row.createCell(0).setCellValue(
                                                v.getName() != null ? v.getName() : "");

                                row.createCell(1).setCellValue(
                                                v.getEmployeeCode() != null ? v.getEmployeeCode() : "");

                                row.createCell(2).setCellValue(
                                                v.getMobileNumber() != null ? v.getMobileNumber() : "");

                                row.createCell(3).setCellValue(
                                                v.getStatus() != null ? v.getStatus().toString() : "");

                                row.createCell(4).setCellValue(
                                                v.getCheckInTime() != null ? v.getCheckInTime().toString() : "");

                                row.createCell(5).setCellValue(
                                                v.getCheckOutTime() != null ? v.getCheckOutTime().toString() : "");
                        }

                        workbook.write(bos);
                        return bos.toByteArray();
                }
        }

}
