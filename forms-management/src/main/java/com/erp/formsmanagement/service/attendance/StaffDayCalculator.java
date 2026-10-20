package com.erp.formsmanagement.service.attendance;

import com.erp.api.attendancemanagement.model.StaffDay;
import com.erp.api.attendancemanagement.model.StaffDayStatus;
import com.erp.formsmanagement.domain.entity.attendance.AttendanceAdjustmentEntity;
import com.erp.formsmanagement.domain.entity.attendance.StaffDepartmentEntity;
import com.erp.formsmanagement.domain.entity.attendance.WorkDay;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * One worker, one day: turns punches (or a manual edit) into the row of the client's salary
 * statement. Pure and stateless, so the statement, Salary Paid, payment history and dashboard all
 * read a day the same way.
 *
 * <p>The rule, which reproduces the client's PagarGuru statement to the paisa:
 *
 * <ul>
 *   <li>In = first punch, Out = last punch. Punches alternate in, out, in, out: with 4+ the
 *       break is every out→in gap (2nd→3rd, 4th→5th, ...), otherwise the department break if the
 *       day spans it. A hand-edited day is read the same way from its edited list.
 *   <li>Actual work = (Out − In) − break. Standard = the department's two halves.
 *   <li>Per-hour = per-day salary ÷ standard hours. Short of standard → (−) deduction at per-hour;
 *       beyond it → (+) addition at the OT rate (per-hour when not set).
 *   <li>Payable = per-day salary − deduction + addition.
 * </ul>
 *
 * Coming in after the department's in-time is "Late Commer"; it costs only the minutes actually
 * missed, so someone late who stays late still earns the full day (as in the PagarGuru rows).
 *
 * <p>{@code date} is a working day (7 AM to 7 AM, see {@link WorkDay}); punches are that working
 * day's, so an 8 PM in and 3 AM out are one shift on the first date.
 */
public final class StaffDayCalculator {

  private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

  private StaffDayCalculator() {}

  /** Everything known about the worker that a day's pay depends on. */
  public record Rates(
      StaffDepartmentEntity department,
      Double perDaySalary,
      Double otRatePerHour,
      LocalDate joiningDate,
      LocalDate balanceFrom,
      DayOfWeek weeklyOff) {

    public boolean salarySet() {
      return department != null
          && department.standardMinutes() > 0
          && perDaySalary != null
          && perDaySalary > 0;
    }

    public double perHour() {
      return salarySet() ? perDaySalary / (department.standardMinutes() / 60.0) : 0;
    }

    public double otPerHour() {
      return otRatePerHour != null && otRatePerHour > 0 ? otRatePerHour : perHour();
    }
  }

  /**
   * @param punches that day's device punches, any order
   * @param adjustment a manual edit for the day, or null
   */
  public static StaffDay calculate(
      LocalDate date,
      List<LocalDateTime> punches,
      AttendanceAdjustmentEntity adjustment,
      Rates rates,
      LocalDate today) {

    StaffDay day = new StaffDay().date(date).late(false).overtime(false).halfDay(false);
    day.manual(adjustment != null).note(adjustment == null ? null : adjustment.getNote());
    day.manualBreakMinutes(adjustment == null ? null : adjustment.getBreakMinutes());
    day.breakMinutes(0).excessBreakMinutes(0).workedMinutes(0);
    day.deduction(0.0).addition(0.0).payable(0.0);

    List<LocalTime> device = new ArrayList<>();
    if (punches != null) {
      punches.stream().sorted().forEach(p -> device.add(p.toLocalTime().withSecond(0).withNano(0)));
    }
    List<LocalTime> times = adjustment != null ? adjustment.times() : device;
    day.punches(times.stream().map(HM::format).toList());
    day.devicePunches(device.stream().map(HM::format).toList());

    if (date.isAfter(today)) return status(day, StaffDayStatus.UPCOMING, "");
    if (rates.joiningDate() != null && date.isBefore(rates.joiningDate()) && times.isEmpty()) {
      return status(day, StaffDayStatus.NOT_JOINED, "Not joined");
    }

    if (times.isEmpty()) {
      if (rates.weeklyOff() != null && date.getDayOfWeek() == rates.weeklyOff()) {
        return status(day, StaffDayStatus.WEEKLY_OFF, "Weekly Off");
      }
      return status(day, StaffDayStatus.ABSENT, adjustment != null ? "Absent (manual)" : "Absent");
    }

    LocalTime in = times.get(0);
    StaffDepartmentEntity dept = rates.department();
    day.morningIn(HM.format(in));
    day.late(dept != null && WorkDay.after(in, dept.getInTime1()));

    if (times.size() == 1) {
      return status(
          day,
          date.equals(today) ? StaffDayStatus.AT_WORK : StaffDayStatus.MISSED_PUNCH,
          date.equals(today) ? "At work" : "Missed punch");
    }

    LocalTime out = times.get(times.size() - 1);
    day.nightOut(HM.format(out));

    // Break: real gap when they punched out and back in for lunch, else the department's.
    int deptBreak = dept == null ? 0 : dept.breakMinutes();
    int breakMinutes = 0;
    if (adjustment != null && adjustment.getBreakMinutes() != null) {
      breakMinutes = Math.max(0, adjustment.getBreakMinutes());
    } else if (times.size() >= 4) {
      breakMinutes = gapMinutes(times);
      if (times.size() <= 5) day.breakFrom(HM.format(times.get(1))).breakTo(HM.format(times.get(2)));
    } else if (dept != null && dept.hasBreak()
        && WorkDay.after(dept.getInTime2(), in) && WorkDay.after(out, dept.getOutTime1())) {
      day.breakFrom(HM.format(dept.getOutTime1())).breakTo(HM.format(dept.getInTime2()));
      breakMinutes = deptBreak;
    }
    // Measured within the 7-to-7 working day: in 20:00, out 03:00 = 7 hours.
    int worked = Math.max(0, WorkDay.between(in, out) - breakMinutes);
    day.breakMinutes(breakMinutes)
        .excessBreakMinutes(Math.max(0, breakMinutes - deptBreak))
        .workedMinutes(worked);

    String label = "Present";
    if (rates.salarySet()) {
      int standard = dept.standardMinutes();
      double deduction = worked < standard ? (standard - worked) * rates.perHour() / 60.0 : 0;
      double addition = worked > standard ? (worked - standard) * rates.otPerHour() / 60.0 : 0;
      day.overtime(worked > standard).halfDay(worked < standard / 2);
      day.deduction(round2(deduction))
          .addition(round2(addition))
          .payable(round2(Math.max(0, rates.perDaySalary() - deduction + addition)));
    }
    if (Boolean.TRUE.equals(day.getHalfDay())) label = "Half Day";
    else if (Boolean.TRUE.equals(day.getLate())) label = "Present (Late Commer)";
    else if (Boolean.TRUE.equals(day.getOvertime())) label = "Present (Over Time)";

    if (rates.balanceFrom() != null && date.isBefore(rates.balanceFrom())) {
      // Shown so the times are visible, but paid by the old software, not the ERP.
      day.deduction(0.0).addition(0.0).payable(0.0);
      return status(day, StaffDayStatus.BEFORE_START, label + " · before ERP start");
    }
    return status(day, StaffDayStatus.PRESENT, label);
  }

  /**
   * Minutes spent out between an out and the next in: the 2nd→3rd, 4th→5th, ... punch. The last
   * punch is always the day's out, so a gap ending on it doesn't count.
   */
  static int gapMinutes(List<LocalTime> times) {
    int sum = 0;
    for (int i = 1; i + 1 < times.size() - 1; i += 2) {
      sum += WorkDay.between(times.get(i), times.get(i + 1));
    }
    return sum;
  }

  private static StaffDay status(StaffDay day, StaffDayStatus s, String label) {
    return day.status(s).statusLabel(label);
  }

  public static double round2(double v) {
    return Math.round(v * 100.0) / 100.0;
  }
}
