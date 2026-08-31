package com.example.aiprojectmanager.scheduling.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Business Calendar engine — adds/subtracts working days while skipping
 * weekends (Saturday & Sunday) and an optional set of regional holidays.
 *
 * <p>Holiday sets are project-scoped and loaded on demand by the caller.
 * For the demo scope, a small set of global Indian public holidays for
 * 2025-2026 is provided as a default; production deployments would load
 * holidays from the {@code business_holidays} table via a repository.
 */
@Service
public class BusinessCalendarService {

    /** Default global holidays (Indian national calendar 2025-2026). */
    private static final Set<LocalDate> DEFAULT_HOLIDAYS;

    static {
        Set<LocalDate> h = new HashSet<>();
        // 2025
        h.add(LocalDate.of(2025, 1, 26));  // Republic Day
        h.add(LocalDate.of(2025, 3, 14));  // Holi
        h.add(LocalDate.of(2025, 4, 14));  // Dr. Ambedkar Jayanti
        h.add(LocalDate.of(2025, 4, 18));  // Good Friday
        h.add(LocalDate.of(2025, 8, 15));  // Independence Day
        h.add(LocalDate.of(2025, 10, 2));  // Gandhi Jayanti
        h.add(LocalDate.of(2025, 10, 24)); // Dussehra
        h.add(LocalDate.of(2025, 11, 5));  // Diwali
        h.add(LocalDate.of(2025, 12, 25)); // Christmas
        // 2026
        h.add(LocalDate.of(2026, 1, 26));
        h.add(LocalDate.of(2026, 3, 3));   // Holi
        h.add(LocalDate.of(2026, 4, 3));   // Good Friday
        h.add(LocalDate.of(2026, 8, 15));
        h.add(LocalDate.of(2026, 10, 2));
        h.add(LocalDate.of(2026, 10, 14)); // Dussehra
        h.add(LocalDate.of(2026, 10, 25)); // Diwali
        h.add(LocalDate.of(2026, 12, 25));
        DEFAULT_HOLIDAYS = Collections.unmodifiableSet(h);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns true if {@code date} is a working day (Mon–Fri, not a holiday).
     */
    public boolean isWorkingDay(LocalDate date) {
        return isWorkingDay(date, DEFAULT_HOLIDAYS);
    }

    public boolean isWorkingDay(LocalDate date, Set<LocalDate> holidays) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY
            && dow != DayOfWeek.SUNDAY
            && !holidays.contains(date);
    }

    /**
     * Adds {@code businessDays} working days to {@code start}, skipping
     * weekends and the default holiday set.
     */
    public LocalDate addBusinessDays(LocalDate start, int businessDays) {
        return addBusinessDays(start, businessDays, DEFAULT_HOLIDAYS);
    }

    public LocalDate addBusinessDays(LocalDate start, int businessDays, Set<LocalDate> holidays) {
        if (businessDays <= 0) return start;
        LocalDate result = start;
        int remaining    = businessDays;
        while (remaining > 0) {
            result = result.plusDays(1);
            if (isWorkingDay(result, holidays)) remaining--;
        }
        return result;
    }

    /**
     * Counts the number of business days between two dates (inclusive of start,
     * exclusive of end — same semantics as {@link java.time.temporal.ChronoUnit#DAYS}).
     */
    public int businessDaysBetween(LocalDate start, LocalDate end) {
        return businessDaysBetween(start, end, DEFAULT_HOLIDAYS);
    }

    public int businessDaysBetween(LocalDate start, LocalDate end, Set<LocalDate> holidays) {
        if (!end.isAfter(start)) return 0;
        int count = 0;
        LocalDate d = start;
        while (d.isBefore(end)) {
            if (isWorkingDay(d, holidays)) count++;
            d = d.plusDays(1);
        }
        return count;
    }

    /**
     * Converts a calendar-day duration to an approximate business-day duration
     * using a 5/7 ratio (suitable for planning estimates).
     */
    public int calendarToBusinessDays(int calendarDays) {
        return (int) Math.round(calendarDays * 5.0 / 7.0);
    }
}
