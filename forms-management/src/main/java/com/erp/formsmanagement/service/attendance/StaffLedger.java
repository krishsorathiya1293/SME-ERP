package com.erp.formsmanagement.service.attendance;

import com.erp.api.attendancemanagement.model.StaffDay;
import com.erp.api.attendancemanagement.model.StaffDayStatus;
import com.erp.formsmanagement.domain.entity.attendance.AttendanceAdjustmentEntity;
import com.erp.formsmanagement.domain.entity.attendance.AttendanceEmployeeEntity;
import com.erp.formsmanagement.domain.entity.attendance.StaffBonusJamaEntity;
import com.erp.formsmanagement.domain.entity.attendance.StaffSalaryPaymentEntity;
import com.erp.formsmanagement.domain.entity.attendance.StaffUpadEntity;
import com.erp.formsmanagement.service.attendance.StaffDayCalculator.Rates;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One worker's money, worked out from attendance plus what was paid. Built per request from data
 * the service bulk-loads, so the employee list and dashboard can do every worker in one pass.
 *
 * <p>Two accounts, as on the client's statement:
 *
 * <ul>
 *   <li><b>Balance</b> (+ = company owes the worker, "Jama"; − = worker owes): opening balance +
 *       salary earned since {@code balanceFrom} + bonus − loan jama − paid. Bonus given between
 *       salaries counts on its own date, less what was handed over that day.
 *   <li><b>Upad</b> pending: opening loan + upad given − loan jama − upad given back in cash.
 * </ul>
 *
 * Settled periods use the payment's snapshot of the salary; days not yet settled are worked out
 * live from attendance. Loan jama belongs to the period it was taken from.
 */
final class StaffLedger {

  final AttendanceEmployeeEntity employee;
  final Rates rates;
  final LocalDate today;
  private final Map<LocalDate, List<LocalDateTime>> punchesByDay;
  private final Map<LocalDate, AttendanceAdjustmentEntity> adjustments;
  final List<StaffSalaryPaymentEntity> payments;
  final List<StaffUpadEntity> upads;
  final List<StaffBonusJamaEntity> bonusJama;

  StaffLedger(
      AttendanceEmployeeEntity employee,
      DayOfWeek weeklyOff,
      LocalDate today,
      Map<LocalDate, List<LocalDateTime>> punchesByDay,
      Map<LocalDate, AttendanceAdjustmentEntity> adjustments,
      List<StaffSalaryPaymentEntity> payments,
      List<StaffUpadEntity> upads,
      List<StaffBonusJamaEntity> bonusJama) {
    this.employee = employee;
    this.today = today;
    this.punchesByDay = punchesByDay == null ? Map.of() : punchesByDay;
    this.adjustments = adjustments == null ? Map.of() : adjustments;
    this.payments = payments == null ? List.of() : payments;
    this.upads = upads == null ? List.of() : upads;
    this.bonusJama = bonusJama == null ? List.of() : bonusJama;
    this.rates =
        new Rates(
            employee.getStaffDepartment(),
            employee.getPerHourSalary(),
            employee.getJoiningDate(),
            employee.getBalanceFrom(),
            weeklyOff);
  }

  StaffDay day(LocalDate date) {
    return StaffDayCalculator.calculate(
        date, punchesByDay.get(date), adjustments.get(date), rates, today);
  }

  List<StaffDay> days(LocalDate from, LocalDate to) {
    List<StaffDay> out = new ArrayList<>();
    for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) out.add(day(d));
    return out;
  }

  /** Salary earned on days in [from, to] that no settled payment in {@code settled} covers. */
  double liveSalary(LocalDate from, LocalDate to, List<StaffSalaryPaymentEntity> settled) {
    double sum = 0;
    LocalDate end = to.isAfter(today) ? today : to;
    for (LocalDate d = from; !d.isAfter(end); d = d.plusDays(1)) {
      if (covered(d, settled)) continue;
      StaffDay day = day(d);
      if (day.getStatus() == StaffDayStatus.PRESENT) sum += day.getPayable();
    }
    return sum;
  }

  /** Balance at the start of {@code date} (everything strictly before it). */
  double balanceBefore(LocalDate date) {
    List<StaffSalaryPaymentEntity> settled =
        payments.stream().filter(p -> p.getPeriodTo().isBefore(date)).toList();
    double balance = employee.getOpeningBalance();
    for (StaffSalaryPaymentEntity p : settled) {
      balance += p.getTotalSalary() + p.getBonus() - p.getLoanDeduction() - p.getPaidAmount();
    }
    for (StaffBonusJamaEntity b : bonusJama) {
      if (b.getEntryDate().isBefore(date)) balance += b.balanceEffect();
    }
    LocalDate start = employee.getBalanceFrom();
    if (start != null && start.isBefore(date)) {
      balance += liveSalary(start, date.minusDays(1), settled);
    }
    return StaffDayCalculator.round2(balance);
  }

  /** Upad pending at the start of {@code date}. */
  double loanBefore(LocalDate date) {
    double loan = employee.getOpeningLoan();
    for (StaffUpadEntity u : upads) if (u.getEntryDate().isBefore(date)) loan += u.getAmount();
    for (StaffSalaryPaymentEntity p : payments) {
      if (p.getPeriodTo().isBefore(date)) loan -= p.getLoanDeduction();
    }
    for (StaffBonusJamaEntity b : bonusJama) {
      if (b.getEntryDate().isBefore(date)) loan -= b.loanEffect();
    }
    return StaffDayCalculator.round2(loan);
  }

  double balanceNow() {
    return balanceBefore(today.plusDays(1));
  }

  double pendingLoanNow() {
    return loanBefore(today.plusDays(1));
  }

  LocalDate lastPaidTo() {
    return payments.stream()
        .map(StaffSalaryPaymentEntity::getPeriodTo)
        .max(LocalDate::compareTo)
        .orElse(null);
  }

  /** Bonus and jama entries dated in [from, to], oldest first. */
  List<StaffBonusJamaEntity> bonusJama(LocalDate from, LocalDate to) {
    return bonusJama.stream()
        .filter(b -> b.on(from, to))
        .sorted(
            java.util.Comparator.comparing(StaffBonusJamaEntity::getEntryDate)
                .thenComparing(StaffBonusJamaEntity::getId, java.util.Comparator.nullsLast(Long::compareTo)))
        .toList();
  }

  /** The settled payment whose period holds {@code date}, if any. */
  StaffSalaryPaymentEntity paymentCovering(LocalDate date) {
    for (StaffSalaryPaymentEntity p : payments) {
      if (!date.isBefore(p.getPeriodFrom()) && !date.isAfter(p.getPeriodTo())) return p;
    }
    return null;
  }

  static boolean covered(LocalDate d, List<StaffSalaryPaymentEntity> settled) {
    for (StaffSalaryPaymentEntity p : settled) {
      if (!d.isBefore(p.getPeriodFrom()) && !d.isAfter(p.getPeriodTo())) return true;
    }
    return false;
  }
}
