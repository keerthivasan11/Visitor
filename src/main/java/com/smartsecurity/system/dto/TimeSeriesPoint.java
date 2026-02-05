package com.smartsecurity.system.dto;
import java.time.LocalDate;
public class TimeSeriesPoint {
    private LocalDate time;
    private Long value;

    public TimeSeriesPoint(LocalDate time, Long value) {
        this.time = time;
        this.value = value;
    }

    public LocalDate getTime() {
        return time;
    }

    public Long getValue() {
        return value;
    }
}
