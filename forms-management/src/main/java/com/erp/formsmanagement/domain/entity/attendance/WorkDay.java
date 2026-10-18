package com.erp.formsmanagement.domain.entity.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * The factory's working day runs 7:00 AM to 7:00 AM, not midnight to midnight: a worker who punches
 * in at 8 PM on the 1st and out at 3 AM on the 2nd worked one shift, counted on the 1st. Every
 * place that turns a punch into "which day" or compares times within a day goes through here.
 */
public final class WorkDay {

  /** When a working day begins (and the previous one ends). */
  public static final LocalTime START = LocalTime.of(7, 0);

  private static final int START_MINUTE = START.getHour() * 60 + START.getMinute();
  private static final int DAY = 24 * 60;

  private WorkDay() {}

  /** The working day a punch belongs to: before 7 AM counts for the previous date. */
  public static LocalDate of(LocalDateTime t) {
    return t.toLocalTime().isBefore(START) ? t.toLocalDate().minusDays(1) : t.toLocalDate();
  }

  /** The working day happening right now. */
  public static LocalDate today() {
    return of(LocalDateTime.now());
  }

  /** First instant of a working day (7:00 AM on that date). */
  public static LocalDateTime startOf(LocalDate d) {
    return d.atTime(START);
  }

  /** Last instant of a working day (just before 7:00 AM the next date). */
  public static LocalDateTime endOf(LocalDate d) {
    return d.plusDays(1).atTime(START).minusNanos(1);
  }

  /**
   * Minutes since the working day started: 07:00 → 0, 20:00 → 780, 03:00 (next morning) → 1200.
   * Times compare correctly across midnight this way.
   */
  public static int minuteOf(LocalTime t) {
    int m = t.getHour() * 60 + t.getMinute() - START_MINUTE;
    return ((m % DAY) + DAY) % DAY;
  }

  /** Minutes from a to b within one working day (0 if b isn't after a). */
  public static int between(LocalTime a, LocalTime b) {
    if (a == null || b == null) return 0;
    return Math.max(0, minuteOf(b) - minuteOf(a));
  }

  /** b comes later in the working day than a. */
  public static boolean after(LocalTime b, LocalTime a) {
    return minuteOf(b) > minuteOf(a);
  }
}
