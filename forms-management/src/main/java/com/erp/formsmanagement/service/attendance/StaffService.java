package com.erp.formsmanagement.service.attendance;

import static com.erp.formsmanagement.service.attendance.StaffDayCalculator.round2;

import com.erp.api.attendancemanagement.model.AttendanceAdjustmentInput;
import com.erp.api.attendancemanagement.model.Gender;
import com.erp.api.attendancemanagement.model.PayMode;
import com.erp.api.attendancemanagement.model.PaymentHistoryRow;
import com.erp.api.attendancemanagement.model.PaymentMode;
import com.erp.api.attendancemanagement.model.RecentTransaction;
import com.erp.api.attendancemanagement.model.SalaryPayment;
import com.erp.api.attendancemanagement.model.SalaryPaymentInput;
import com.erp.api.attendancemanagement.model.SalaryStatement;
import com.erp.api.attendancemanagement.model.StaffBrief;
import com.erp.api.attendancemanagement.model.StaffDashboard;
import com.erp.api.attendancemanagement.model.StaffDay;
import com.erp.api.attendancemanagement.model.StaffDayStatus;
import com.erp.api.attendancemanagement.model.StaffDepartment;
import com.erp.api.attendancemanagement.model.StaffDepartmentInput;
import com.erp.api.attendancemanagement.model.StaffImage;
import com.erp.api.attendancemanagement.model.StaffPhotosInput;
import com.erp.api.attendancemanagement.model.StaffProfile;
import com.erp.api.attendancemanagement.model.StaffProfileInput;
import com.erp.api.attendancemanagement.model.StaffSettings;
import com.erp.api.attendancemanagement.model.StaffSummary;
import com.erp.api.attendancemanagement.model.Upad;
import com.erp.api.attendancemanagement.model.UpadInput;
import com.erp.api.attendancemanagement.model.WeekDay;
import com.erp.formsmanagement.domain.entity.attendance.AttendanceAdjustmentEntity;
import com.erp.formsmanagement.domain.entity.attendance.AttendanceEmployeeEntity;
import com.erp.formsmanagement.domain.entity.attendance.AttendancePunchEntity;
import com.erp.formsmanagement.domain.entity.attendance.StaffDepartmentEntity;
import com.erp.formsmanagement.domain.entity.attendance.StaffSalaryPaymentEntity;
import com.erp.formsmanagement.domain.entity.attendance.StaffSettingEntity;
import com.erp.formsmanagement.domain.entity.attendance.StaffUpadEntity;
import com.erp.formsmanagement.domain.entity.attendance.WorkDay;
import com.erp.formsmanagement.domain.repository.attendance.AttendanceAdjustmentRepository;
import com.erp.formsmanagement.domain.repository.attendance.AttendanceEmployeeRepository;
import com.erp.formsmanagement.domain.repository.attendance.AttendancePunchRepository;
import com.erp.formsmanagement.domain.repository.attendance.StaffDepartmentRepository;
import com.erp.formsmanagement.domain.repository.attendance.StaffSalaryPaymentRepository;
import com.erp.formsmanagement.domain.repository.attendance.StaffSettingRepository;
import com.erp.formsmanagement.domain.repository.attendance.StaffUpadRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Staff & payroll, as laid out in the client's "Attendence Software.xlsx": Add Emp., Profile,
 * Salary Paid (Pagar Didho), Upad, Payment History, the PagarGuru-style statement and the
 * dashboard. Day pay comes from {@link StaffDayCalculator}; balances from {@link StaffLedger}.
 */
@Service
@Transactional
public class StaffService {

  private static final int MAX_IMAGE_CHARS = 2_000_000;
  private static final long MAX_RANGE_DAYS = 400;
  private static final DateTimeFormatter AMPM = DateTimeFormatter.ofPattern("hh:mma", Locale.ENGLISH);

  private final AttendanceEmployeeRepository employees;
  private final StaffDepartmentRepository departments;
  private final AttendancePunchRepository punches;
  private final AttendanceAdjustmentRepository adjustments;
  private final StaffUpadRepository upads;
  private final StaffSalaryPaymentRepository payments;
  private final StaffSettingRepository settings;

  public StaffService(
      AttendanceEmployeeRepository employees,
      StaffDepartmentRepository departments,
      AttendancePunchRepository punches,
      AttendanceAdjustmentRepository adjustments,
      StaffUpadRepository upads,
      StaffSalaryPaymentRepository payments,
      StaffSettingRepository settings) {
    this.employees = employees;
    this.departments = departments;
    this.punches = punches;
    this.adjustments = adjustments;
    this.upads = upads;
    this.payments = payments;
    this.settings = settings;
  }

  // ================================================================== settings

  @Transactional(readOnly = true)
  public StaffSettings getSettings() {
    return new StaffSettings()
        .weekStart(WeekDay.valueOf(weekStart().name()))
        .weeklyOff(weeklyOff() == null ? "NONE" : weeklyOff().name());
  }

  public StaffSettings updateSettings(StaffSettings in) {
    if (in.getWeekStart() != null) put(StaffSettingEntity.WEEK_START, in.getWeekStart().name());
    if (in.getWeeklyOff() != null) {
      String off = in.getWeeklyOff().trim().toUpperCase();
      if (!off.equals("NONE")) parseDay(off);
      put(StaffSettingEntity.WEEKLY_OFF, off);
    }
    return getSettings();
  }

  DayOfWeek weekStart() {
    String v = setting(StaffSettingEntity.WEEK_START);
    return v == null ? DayOfWeek.SATURDAY : parseDay(v);
  }

  /** The factory's weekly holiday, or null. */
  public DayOfWeek weeklyOff() {
    String v = setting(StaffSettingEntity.WEEKLY_OFF);
    return v == null || v.equals("NONE") ? null : parseDay(v);
  }

  private String setting(String key) {
    return settings.findById(key).map(StaffSettingEntity::getValue).orElse(null);
  }

  private void put(String key, String value) {
    StaffSettingEntity e = settings.findById(key).orElseGet(StaffSettingEntity::new);
    e.setKey(key);
    e.setValue(value);
    settings.save(e);
  }

  private static DayOfWeek parseDay(String v) {
    try {
      return DayOfWeek.valueOf(v.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw bad("Unknown day: " + v);
    }
  }

  // ================================================================== departments

  @Transactional(readOnly = true)
  public List<StaffDepartment> listDepartments() {
    return departments.findAllByOrderByNameAsc().stream().map(this::toDepartment).toList();
  }

  public StaffDepartment createDepartment(StaffDepartmentInput in) {
    departments
        .findByNameIgnoreCase(in.getName().trim())
        .ifPresent(d -> {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists");
        });
    StaffDepartmentEntity e = new StaffDepartmentEntity();
    applyDepartment(e, in);
    return toDepartment(departments.save(e));
  }

  public StaffDepartment updateDepartment(Long id, StaffDepartmentInput in) {
    StaffDepartmentEntity e = department(id);
    departments
        .findByNameIgnoreCase(in.getName().trim())
        .filter(other -> !other.getId().equals(id))
        .ifPresent(d -> {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists");
        });
    applyDepartment(e, in);
    return toDepartment(departments.save(e));
  }

  public void deleteDepartment(Long id) {
    StaffDepartmentEntity e = department(id);
    long used = employees.countByStaffDepartmentIdAndDeletedFalse(id);
    if (used > 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, used + " employee(s) are in this department - move them first");
    }
    departments.delete(e);
  }

  private void applyDepartment(StaffDepartmentEntity e, StaffDepartmentInput in) {
    if (isBlank(in.getName())) throw bad("Department name is required");
    LocalTime in1 = time(in.getInTime1(), "In time");
    LocalTime out1 = time(in.getOutTime1(), "Out time");
    LocalTime in2 = isBlank(in.getInTime2()) ? null : time(in.getInTime2(), "Second in time");
    LocalTime out2 = isBlank(in.getOutTime2()) ? null : time(in.getOutTime2(), "Second out time");
    // Within the 7 AM - 7 AM working day, so a night shift like 20:00 -> 04:00 is valid.
    if (!WorkDay.after(out1, in1)) throw bad("Out time must be after in time");
    if ((in2 == null) != (out2 == null)) throw bad("Give both times after the break, or neither");
    if (in2 != null && (!WorkDay.after(in2, out1) || !WorkDay.after(out2, in2))) {
      throw bad("Times after the break must come after the first half");
    }
    e.setName(in.getName().trim());
    e.setInTime1(in1);
    e.setOutTime1(out1);
    e.setInTime2(in2);
    e.setOutTime2(out2);
  }

  private StaffDepartmentEntity department(Long id) {
    return departments
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
  }

  StaffDepartment toDepartment(StaffDepartmentEntity e) {
    if (e == null) return null;
    return new StaffDepartment()
        .id(e.getId())
        .name(e.getName())
        .inTime1(hm(e.getInTime1()))
        .outTime1(hm(e.getOutTime1()))
        .inTime2(hm(e.getInTime2()))
        .outTime2(hm(e.getOutTime2()))
        .breakMinutes(e.breakMinutes())
        .standardMinutes(e.standardMinutes())
        .employeeCount((int) employees.countByStaffDepartmentIdAndDeletedFalse(e.getId()));
  }

  // ================================================================== employees

  @Transactional(readOnly = true)
  public List<StaffSummary> listEmployees(boolean includeInactive) {
    List<AttendanceEmployeeEntity> list =
        employees.findByDeletedFalse().stream()
            .filter(e -> includeInactive || e.isActive())
            .toList();
    LocalDate today = WorkDay.today();
    Map<String, StaffLedger> ledgers = ledgers(list, earliestStart(list, today), today);
    Map<String, LocalDateTime> lastPunch = lastPunches();
    return list.stream()
        .map(
            e -> {
              StaffLedger l = ledgers.get(e.getEmpCode());
              LocalDateTime last = lastPunch.get(e.getEmpCode());
              return new StaffSummary()
                  .empCode(e.getEmpCode())
                  .name(e.getFullName())
                  .phone(e.getPhone())
                  .departmentName(
                      e.getStaffDepartment() == null ? null : e.getStaffDepartment().getName())
                  .photoThumb(e.getPhotoThumb())
                  .active(e.isActive())
                  .perDaySalary(e.getPerDaySalary())
                  .payMode(payMode(e))
                  .balance(l.balanceNow())
                  .pendingLoan(l.pendingLoanNow())
                  .salarySet(e.isSalarySet())
                  .lastPunchTime(last == null ? null : last.toString())
                  .todayStatus(l.day(today).getStatus());
            })
        .sorted(
            Comparator.comparing(
                (StaffSummary s) -> s.getName() == null ? "~" + s.getEmpCode() : s.getName(),
                String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @Transactional(readOnly = true)
  public StaffProfile getProfile(String empCode) {
    return toProfile(employee(empCode));
  }

  /** Add Employee. A code the device already knows just gets its profile filled in. */
  public StaffProfile createEmployee(StaffProfileInput in) {
    if (isBlank(in.getEmpCode())) throw bad("Device code is required");
    String code = in.getEmpCode().trim();
    AttendanceEmployeeEntity e = employees.findByEmpCode(code).orElse(null);
    if (e != null && !e.isDeleted() && !isBlank(e.getName())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Code " + code + " already belongs to " + e.getFullName());
    }
    if (e == null) {
      e = new AttendanceEmployeeEntity();
      e.setEmpCode(code);
    }
    e.setDeleted(false);
    e.setActive(true);
    applyProfile(e, in);
    return toProfile(employees.save(e));
  }

  public StaffProfile updateEmployee(String empCode, StaffProfileInput in) {
    AttendanceEmployeeEntity e = employee(empCode);
    if (!isBlank(in.getEmpCode()) && !in.getEmpCode().trim().equals(e.getEmpCode())) {
      throw bad("The device code can't be changed - add a new employee instead");
    }
    applyProfile(e, in);
    return toProfile(employees.save(e));
  }

  public StaffProfile setActive(String empCode, boolean active) {
    AttendanceEmployeeEntity e = employee(empCode);
    e.setActive(active);
    return toProfile(employees.save(e));
  }

  /** Hidden from the ERP from now on; punches, payments and upad history stay. */
  public void deleteEmployee(String empCode) {
    AttendanceEmployeeEntity e = employee(empCode);
    e.setDeleted(true);
    e.setActive(false);
    employees.save(e);
  }

  public void updatePhotos(String empCode, StaffPhotosInput in) {
    AttendanceEmployeeEntity e = employee(empCode);
    if (in.getPhoto() != null) e.setPhoto(image(in.getPhoto(), "Photo"));
    if (in.getPhotoThumb() != null) e.setPhotoThumb(image(in.getPhotoThumb(), "Photo"));
    if (in.getAadharPhoto() != null) e.setAadharPhoto(image(in.getAadharPhoto(), "Aadhar photo"));
    employees.save(e);
  }

  @Transactional(readOnly = true)
  public StaffImage aadharPhoto(String empCode) {
    return new StaffImage().dataUrl(employee(empCode).getAadharPhoto());
  }

  private static String image(String dataUrl, String what) {
    if (dataUrl.isEmpty()) return null;
    if (!dataUrl.startsWith("data:image/")) throw bad(what + " must be an image");
    if (dataUrl.length() > MAX_IMAGE_CHARS) throw bad(what + " is too large");
    return dataUrl;
  }

  private void applyProfile(AttendanceEmployeeEntity e, StaffProfileInput in) {
    if (isBlank(in.getName())) throw bad("Worker name is required");
    if (in.getPerDaySalary() != null && in.getPerDaySalary() < 0) throw bad("Salary can't be negative");
    e.setName(in.getName().trim());
    e.setGender(in.getGender() == null ? null : in.getGender().name());
    e.setPhone(trim(in.getPhone()));
    e.setAadharNo(trim(in.getAadharNo()));
    e.setBirthDate(in.getBirthDate());
    e.setReference(trim(in.getReference()));
    e.setJoiningDate(in.getJoiningDate());
    e.setStaffDepartment(in.getDepartmentId() == null ? null : department(in.getDepartmentId()));
    e.setPerDaySalary(in.getPerDaySalary());
    e.setOtRatePerHour(in.getOtRatePerHour());
    e.setPayMode(in.getPayMode() == null ? PayMode.WEEKLY.name() : in.getPayMode().name());
    e.setOpeningBalance(in.getOpeningBalance() == null ? 0 : in.getOpeningBalance());
    e.setOpeningLoan(in.getOpeningLoan() == null ? 0 : in.getOpeningLoan());
    LocalDate from = in.getBalanceFrom();
    if (from == null && e.getBalanceFrom() == null && e.isSalarySet()) from = WorkDay.today();
    if (from != null) e.setBalanceFrom(from);
  }

  private StaffProfile toProfile(AttendanceEmployeeEntity e) {
    LocalDate today = WorkDay.today();
    StaffLedger l = ledger(e, earliestStart(List.of(e), today), today);
    StaffDayCalculator.Rates r = l.rates;
    LocalDate lastPaid = l.lastPaidTo();
    LocalDate[] suggested = suggestedPeriod(e, lastPaid, today);
    LocalDateTime last = punches.findLatestPunchFor(e.getEmpCode());
    StaffDepartmentEntity d = e.getStaffDepartment();
    return new StaffProfile()
        .empCode(e.getEmpCode())
        .name(e.getFullName())
        .deviceName(e.getDeviceName())
        .gender(e.getGender() == null ? null : Gender.valueOf(e.getGender()))
        .phone(e.getPhone())
        .aadharNo(e.getAadharNo())
        .birthDate(e.getBirthDate())
        .reference(e.getReference())
        .joiningDate(e.getJoiningDate())
        .department(toDepartment(d))
        .perDaySalary(e.getPerDaySalary())
        .perDayHours(d == null ? null : round2(d.standardMinutes() / 60.0))
        .perHourSalary(r.salarySet() ? round2(r.perHour()) : null)
        .otRatePerHour(e.getOtRatePerHour())
        .payMode(payMode(e))
        .openingBalance(e.getOpeningBalance())
        .openingLoan(e.getOpeningLoan())
        .balanceFrom(e.getBalanceFrom())
        .active(e.isActive())
        .photo(e.getPhoto())
        .photoThumb(e.getPhotoThumb())
        .hasAadharPhoto(e.getAadharPhoto() != null)
        .balance(l.balanceNow())
        .pendingLoan(l.pendingLoanNow())
        .lastPaidTo(lastPaid)
        .suggestedPeriodFrom(suggested[0])
        .suggestedPeriodTo(suggested[1])
        .lastPunchTime(last == null ? null : last.toString());
  }

  private AttendanceEmployeeEntity employee(String empCode) {
    return employees
        .findByEmpCode(empCode)
        .filter(e -> !e.isDeleted())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
  }

  private static PayMode payMode(AttendanceEmployeeEntity e) {
    return e.getPayMode() == null ? PayMode.WEEKLY : PayMode.valueOf(e.getPayMode());
  }

  // ================================================================== days & manual edits

  @Transactional(readOnly = true)
  public List<StaffDay> days(String empCode, LocalDate from, LocalDate to) {
    checkRange(from, to);
    AttendanceEmployeeEntity e = employee(empCode);
    return ledger(e, from, to).days(from, to);
  }

  public StaffDay adjustDay(String empCode, LocalDate date, AttendanceAdjustmentInput in) {
    AttendanceEmployeeEntity e = employee(empCode);
    if (date.isAfter(WorkDay.today())) throw bad("Can't edit a future day");
    boolean absent = Boolean.TRUE.equals(in.getAbsent());
    List<LocalTime> times = absent ? List.of() : adjustedTimes(in);
    if (!absent && times.isEmpty()) throw bad("Give the in time, or mark the day absent");
    if (in.getBreakMinutes() != null && in.getBreakMinutes() < 0) throw bad("Break can't be negative");
    AttendanceAdjustmentEntity a =
        adjustments.findByEmpCodeAndWorkDate(e.getEmpCode(), date).orElseGet(AttendanceAdjustmentEntity::new);
    a.setEmpCode(e.getEmpCode());
    a.setWorkDate(date);
    a.setAbsent(absent);
    a.setInTime(times.isEmpty() ? null : times.get(0));
    a.setOutTime(times.size() < 2 ? null : times.get(times.size() - 1));
    a.setPunchTimes(
        times.isEmpty() ? null : times.stream().map(LocalTime::toString).collect(Collectors.joining(",")));
    a.setBreakMinutes(absent ? null : in.getBreakMinutes());
    a.setNote(trim(in.getNote()));
    adjustments.save(a);
    return ledger(e, date, date).day(date);
  }

  /**
   * The edited day's in/outs, in working-day order: the punches list, or in + out from older
   * clients. An out after midnight (in 20:00, out 03:00) is the same working day.
   */
  private static List<LocalTime> adjustedTimes(AttendanceAdjustmentInput in) {
    List<LocalTime> times = new ArrayList<>();
    if (in.getPunches() != null && !in.getPunches().isEmpty()) {
      for (String p : in.getPunches()) if (!isBlank(p)) times.add(time(p, "Punch time"));
    } else {
      if (!isBlank(in.getInTime())) times.add(time(in.getInTime(), "In time"));
      if (!isBlank(in.getOutTime())) {
        if (times.isEmpty()) throw bad("Give the in time, or mark the day absent");
        LocalTime out = time(in.getOutTime(), "Out time");
        if (!WorkDay.after(out, times.get(0))) throw bad("Out time must be after in time");
        times.add(out);
      }
    }
    if (times.size() > 20) throw bad("At most 20 punches in a day");
    times.sort(Comparator.comparingInt(WorkDay::minuteOf));
    for (int i = 1; i < times.size(); i++) {
      if (times.get(i).equals(times.get(i - 1))) throw bad("Punch " + times.get(i) + " is there twice");
    }
    return times;
  }

  public StaffDay clearAdjustment(String empCode, LocalDate date) {
    AttendanceEmployeeEntity e = employee(empCode);
    adjustments.findByEmpCodeAndWorkDate(e.getEmpCode(), date).ifPresent(adjustments::delete);
    adjustments.flush();
    return ledger(e, date, date).day(date);
  }

  // ================================================================== statement

  @Transactional(readOnly = true)
  public SalaryStatement statement(String empCode, LocalDate from, LocalDate to) {
    checkRange(from, to);
    AttendanceEmployeeEntity e = employee(empCode);
    LocalDate loadFrom = min(from, e.getBalanceFrom());
    return statement(e, ledger(e, loadFrom, to), from, to);
  }

  private SalaryStatement statement(
      AttendanceEmployeeEntity e, StaffLedger l, LocalDate from, LocalDate to) {
    List<StaffDay> days = l.days(from, to);
    StaffSalaryPaymentEntity paid =
        l.payments.stream()
            .filter(p -> p.getPeriodFrom().equals(from) && p.getPeriodTo().equals(to))
            .findFirst()
            .orElse(null);
    boolean overlaps =
        l.payments.stream()
            .anyMatch(p -> p != paid && !p.getPeriodFrom().isAfter(to) && !p.getPeriodTo().isBefore(from));

    int present = 0, half = 0, absent = 0, off = 0, late = 0, worked = 0;
    double deduction = 0, addition = 0, live = 0;
    for (StaffDay d : days) {
      switch (d.getStatus()) {
        case PRESENT -> {
          if (Boolean.TRUE.equals(d.getHalfDay())) half++;
          else present++;
          live += d.getPayable();
          deduction += d.getDeduction();
          addition += d.getAddition();
        }
        case ABSENT, MISSED_PUNCH -> absent++;
        case WEEKLY_OFF -> off++;
        default -> {}
      }
      if (Boolean.TRUE.equals(d.getLate())) late++;
      worked += d.getWorkedMinutes() == null ? 0 : d.getWorkedMinutes();
    }

    double opening = paid != null ? paid.getOpeningBalance() : l.balanceBefore(from);
    double total = paid != null ? paid.getTotalSalary() : round2(live);
    double bonus = paid != null ? paid.getBonus() : 0;
    double loanDed = paid != null ? paid.getLoanDeduction() : 0;
    double toPay = round2(opening + total + bonus - loanDed);
    double paidAmount = paid != null ? paid.getPaidAmount() : 0;

    double loanGiven = e.getOpeningLoan();
    for (StaffUpadEntity u : l.upads) if (!u.getEntryDate().isAfter(to)) loanGiven += u.getAmount();
    double loanDeducted = 0;
    for (StaffSalaryPaymentEntity p : l.payments) {
      if (!p.getPeriodFrom().isAfter(to)) loanDeducted += p.getLoanDeduction();
    }

    StaffDepartmentEntity d = e.getStaffDepartment();
    StaffDayCalculator.Rates r = l.rates;
    return new SalaryStatement()
        .empCode(e.getEmpCode())
        .name(e.getFullName())
        .photoThumb(e.getPhotoThumb())
        .departmentName(d == null ? null : d.getName())
        .timing(timing(d))
        .joiningDate(e.getJoiningDate())
        .payMode(payMode(e))
        .perDaySalary(e.getPerDaySalary())
        .perHourSalary(r.salarySet() ? round2(r.perHour()) : null)
        .otRatePerHour(r.salarySet() ? round2(r.otPerHour()) : null)
        .standardMinutes(d == null ? null : d.standardMinutes())
        .salarySet(r.salarySet())
        .periodFrom(from)
        .periodTo(to)
        .totalDays((int) ChronoUnit.DAYS.between(from, to) + 1)
        .presentDays(present + half * 0.5)
        .halfDays(half)
        .absentDays(absent)
        .weeklyOffDays(off)
        .lateDays(late)
        .totalWorkedMinutes(worked)
        .totalDeduction(round2(deduction))
        .totalAddition(round2(addition))
        .totalSalary(total)
        .openingBalance(opening)
        .bonus(bonus)
        .loanDeduction(loanDed)
        .salaryToPay(toPay)
        .paidAmount(paidAmount)
        .closingBalance(round2(toPay - paidAmount))
        .loanBefore(l.loanBefore(from))
        .totalLoanGiven(round2(loanGiven))
        .totalLoanDeducted(round2(loanDeducted))
        .pendingLoan(round2(loanGiven - loanDeducted))
        .payment(paid == null ? null : toPayment(paid, e))
        .overlapsPayment(overlaps)
        .days(days);
  }

  /** "08:00am TO 07:20pm (10:20 Hours / Break Hours 01:00)", as on the PagarGuru statement. */
  private static String timing(StaffDepartmentEntity d) {
    if (d == null) return null;
    return AMPM.format(d.getInTime1()).toLowerCase(Locale.ENGLISH)
        + " TO "
        + AMPM.format(d.dayEnd()).toLowerCase(Locale.ENGLISH)
        + " ("
        + hhmm(d.standardMinutes())
        + " Hours / Break Hours "
        + hhmm(d.breakMinutes())
        + ")";
  }

  // ================================================================== salary paid (Pagar Didho)

  @Transactional(readOnly = true)
  public List<SalaryPayment> listPayments(String empCode) {
    AttendanceEmployeeEntity e = employee(empCode);
    return payments.findByEmpCodeOrderByPeriodFromDescIdDesc(e.getEmpCode()).stream()
        .map(p -> toPayment(p, e))
        .toList();
  }

  public SalaryPayment createPayment(String empCode, SalaryPaymentInput in) {
    AttendanceEmployeeEntity e = employee(empCode);
    LocalDate from = in.getPeriodFrom();
    LocalDate to = in.getPeriodTo();
    checkRange(from, to);
    if (!e.isSalarySet()) throw bad("Set the department and per-day salary first");
    if (in.getPaymentMode() == null) throw bad("Choose how the salary was paid");
    double paid = nz(in.getPaidAmount());
    double bonus = nz(in.getBonus());
    double loanDed = nz(in.getLoanDeduction());
    if (paid < 0 || bonus < 0 || loanDed < 0) throw bad("Amounts can't be negative");

    StaffLedger l = ledger(e, min(from, e.getBalanceFrom()), to);
    StaffSalaryPaymentEntity clash =
        l.payments.stream()
            .filter(p -> !p.getPeriodFrom().isAfter(to) && !p.getPeriodTo().isBefore(from))
            .findFirst()
            .orElse(null);
    if (clash != null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Salary for " + clash.getPeriodFrom() + " to " + clash.getPeriodTo() + " is already paid");
    }
    double pendingLoan = l.loanBefore(to.plusDays(1));
    if (loanDed > pendingLoan + 0.01) {
      throw bad("Loan jama (" + loanDed + ") is more than the pending upad (" + pendingLoan + ")");
    }

    SalaryStatement s = statement(e, l, from, to);
    StaffSalaryPaymentEntity p = new StaffSalaryPaymentEntity();
    p.setEmpCode(e.getEmpCode());
    p.setPeriodFrom(from);
    p.setPeriodTo(to);
    p.setOpeningBalance(s.getOpeningBalance());
    p.setTotalSalary(s.getTotalSalary());
    p.setBonus(bonus);
    p.setLoanDeduction(loanDed);
    p.setPayable(round2(s.getOpeningBalance() + s.getTotalSalary() + bonus - loanDed));
    p.setPaidAmount(paid);
    p.setPaymentMode(in.getPaymentMode().name());
    p.setPaidOn(in.getPaidOn() == null ? LocalDate.now() : in.getPaidOn());
    p.setNote(trim(in.getNote()));
    return toPayment(payments.save(p), e);
  }

  public void deletePayment(Long id) {
    payments.delete(
        payments
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found")));
  }

  private SalaryPayment toPayment(StaffSalaryPaymentEntity p, AttendanceEmployeeEntity e) {
    return new SalaryPayment()
        .id(p.getId())
        .empCode(p.getEmpCode())
        .employeeName(e == null ? null : e.getFullName())
        .periodFrom(p.getPeriodFrom())
        .periodTo(p.getPeriodTo())
        .openingBalance(p.getOpeningBalance())
        .totalSalary(p.getTotalSalary())
        .bonus(p.getBonus())
        .loanDeduction(p.getLoanDeduction())
        .payable(p.getPayable())
        .paidAmount(p.getPaidAmount())
        .balanceAfter(round2(p.balanceAfter()))
        .paymentMode(PaymentMode.valueOf(p.getPaymentMode()))
        .paidOn(p.getPaidOn())
        .note(p.getNote());
  }

  // ================================================================== upad

  @Transactional(readOnly = true)
  public List<Upad> listUpads(String empCode) {
    AttendanceEmployeeEntity e = employee(empCode);
    return upads.findByEmpCodeOrderByEntryDateDescIdDesc(e.getEmpCode()).stream()
        .map(u -> toUpad(u, e))
        .toList();
  }

  public Upad createUpad(String empCode, UpadInput in) {
    AttendanceEmployeeEntity e = employee(empCode);
    if (in.getAmount() == null || in.getAmount() <= 0) throw bad("Enter the upad amount");
    if (in.getPaymentMode() == null) throw bad("Choose how the upad was given");
    StaffUpadEntity u = new StaffUpadEntity();
    u.setEmpCode(e.getEmpCode());
    u.setEntryDate(in.getEntryDate() == null ? LocalDate.now() : in.getEntryDate());
    u.setAmount(in.getAmount());
    u.setPaymentMode(in.getPaymentMode().name());
    u.setNote(trim(in.getNote()));
    return toUpad(upads.save(u), e);
  }

  public void deleteUpad(Long id) {
    upads.delete(
        upads
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upad not found")));
  }

  private static Upad toUpad(StaffUpadEntity u, AttendanceEmployeeEntity e) {
    return new Upad()
        .id(u.getId())
        .empCode(u.getEmpCode())
        .employeeName(e == null ? null : e.getFullName())
        .entryDate(u.getEntryDate())
        .amount(u.getAmount())
        .paymentMode(PaymentMode.valueOf(u.getPaymentMode()))
        .note(u.getNote());
  }

  // ================================================================== payment history

  /** One row per pay period (by the worker's pay mode) in [from, to], newest first. */
  @Transactional(readOnly = true)
  public List<PaymentHistoryRow> paymentHistory(String empCode, LocalDate from, LocalDate to) {
    checkRange(from, to);
    AttendanceEmployeeEntity e = employee(empCode);
    StaffLedger l = ledger(e, from, to);
    PayMode mode = payMode(e);
    List<PaymentHistoryRow> rows = new ArrayList<>();
    // Nothing to show before the ERP took over this worker's salary.
    LocalDate first = e.getBalanceFrom() != null && e.getBalanceFrom().isAfter(from) ? e.getBalanceFrom() : from;
    for (LocalDate start = periodStart(first, mode); !start.isAfter(to); ) {
      LocalDate end = periodEnd(start, mode);
      LocalDate pFrom = start.isBefore(first) ? first : start;
      LocalDate pTo = end.isAfter(to) ? to : end;
      List<StaffSalaryPaymentEntity> inPeriod =
          l.payments.stream()
              .filter(p -> !p.getPeriodFrom().isBefore(pFrom) && !p.getPeriodFrom().isAfter(pTo))
              .toList();
      double salary = l.liveSalary(pFrom, pTo, inPeriod);
      double paid = 0, loanDed = 0;
      Set<String> modes = new LinkedHashSet<>();
      for (StaffSalaryPaymentEntity p : inPeriod) {
        salary += p.getTotalSalary();
        paid += p.getPaidAmount();
        loanDed += p.getLoanDeduction();
        modes.add(modeLabel(p.getPaymentMode()));
      }
      double loanGiven = 0;
      for (StaffUpadEntity u : l.upads) {
        if (!u.getEntryDate().isBefore(pFrom) && !u.getEntryDate().isAfter(pTo)) {
          loanGiven += u.getAmount();
          modes.add(modeLabel(u.getPaymentMode()));
        }
      }
      boolean settled = true;
      for (LocalDate d = pFrom; !d.isAfter(pTo); d = d.plusDays(1)) {
        if (!StaffLedger.covered(d, inPeriod)) {
          settled = false;
          break;
        }
      }
      rows.add(
          new PaymentHistoryRow()
              .periodFrom(pFrom)
              .periodTo(pTo)
              .totalSalary(round2(salary))
              .loanGiven(round2(loanGiven))
              .loanDeducted(round2(loanDed))
              .paidSalary(round2(paid))
              .settled(settled)
              .paymentModes(String.join(", ", modes)));
      start = end.plusDays(1);
    }
    rows.sort(Comparator.comparing(PaymentHistoryRow::getPeriodFrom).reversed());
    return rows;
  }

  private static String modeLabel(String mode) {
    return switch (mode) {
      case "CASH" -> "Cash";
      case "UPI" -> "UPI";
      case "BANK_TRANSFER" -> "Bank";
      case "CHEQUE" -> "Cheque";
      default -> mode;
    };
  }

  // ================================================================== dashboard

  @Transactional(readOnly = true)
  public StaffDashboard dashboard() {
    LocalDate today = WorkDay.today();
    LocalDate weekFrom = today.with(TemporalAdjusters.previousOrSame(weekStart()));
    LocalDate weekTo = weekFrom.plusDays(6);
    LocalDate yesterday = today.minusDays(1);
    LocalDateTime recentCutoff = WorkDay.startOf(today.minusDays(AttendanceService.ACTIVE_WINDOW_DAYS));

    List<AttendanceEmployeeEntity> all = employees.findByDeletedFalse();
    Map<String, LocalDateTime> lastPunch = lastPunches();
    // On the floor = active and either set up for salary or seen on the device lately.
    List<AttendanceEmployeeEntity> staff =
        all.stream()
            .filter(AttendanceEmployeeEntity::isActive)
            .filter(
                e -> e.isSalarySet()
                    || (lastPunch.get(e.getEmpCode()) != null
                        && lastPunch.get(e.getEmpCode()).isAfter(recentCutoff)))
            .toList();
    Map<String, StaffLedger> ledgers =
        ledgers(staff, min(min(earliestStart(staff, today), weekFrom), yesterday), today);

    int present = 0, atWork = 0, late = 0, absent = 0, missed = 0, notSet = 0;
    double weekSalary = 0, balance = 0, loan = 0;
    List<StaffBrief> lateList = new ArrayList<>();
    List<StaffBrief> absentList = new ArrayList<>();
    List<StaffBrief> missedList = new ArrayList<>();
    List<StaffBrief> balances = new ArrayList<>();
    for (AttendanceEmployeeEntity e : staff) {
      StaffLedger l = ledgers.get(e.getEmpCode());
      StaffDay d = l.day(today);
      switch (d.getStatus()) {
        case PRESENT -> present++;
        case AT_WORK -> {
          present++;
          atWork++;
        }
        case ABSENT -> {
          absent++;
          absentList.add(brief(e, null, null));
        }
        default -> {}
      }
      if (Boolean.TRUE.equals(d.getLate())) {
        late++;
        lateList.add(brief(e, d.getMorningIn(), null));
      }
      StaffDay y = l.day(yesterday);
      if (y.getStatus() == StaffDayStatus.MISSED_PUNCH) {
        missed++;
        missedList.add(brief(e, y.getMorningIn(), null));
      }
      if (!e.isSalarySet()) {
        notSet++;
        continue;
      }
      weekSalary += l.liveSalary(weekFrom, today, List.of());
      double b = l.balanceNow();
      if (b > 0) balance += b;
      loan += l.pendingLoanNow();
      balances.add(brief(e, null, b));
    }
    lateList.sort(Comparator.comparing(StaffBrief::getTime, Comparator.nullsLast(String::compareTo)).reversed());
    balances.sort(Comparator.comparing(StaffBrief::getAmount).reversed());

    double paidWeek = 0, upadWeek = 0;
    Map<String, AttendanceEmployeeEntity> byCode =
        all.stream().collect(Collectors.toMap(AttendanceEmployeeEntity::getEmpCode, Function.identity(), (a, b) -> a));
    List<RecentTransaction> recent = new ArrayList<>();
    for (StaffSalaryPaymentEntity p : payments.findAll()) {
      if (!p.getPaidOn().isBefore(weekFrom) && !p.getPaidOn().isAfter(weekTo)) paidWeek += p.getPaidAmount();
    }
    for (StaffUpadEntity u : upads.findAll()) {
      if (!u.getEntryDate().isBefore(weekFrom) && !u.getEntryDate().isAfter(weekTo)) upadWeek += u.getAmount();
    }
    for (StaffSalaryPaymentEntity p : payments.findTop10ByOrderByPaidOnDescIdDesc()) {
      AttendanceEmployeeEntity e = byCode.get(p.getEmpCode());
      recent.add(
          new RecentTransaction()
              .kind(RecentTransaction.KindEnum.SALARY)
              .id(p.getId())
              .empCode(p.getEmpCode())
              .employeeName(e == null ? p.getEmpCode() : e.getFullName())
              .photoThumb(e == null ? null : e.getPhotoThumb())
              .date(p.getPaidOn())
              .amount(p.getPaidAmount())
              .paymentMode(PaymentMode.valueOf(p.getPaymentMode()))
              .periodFrom(p.getPeriodFrom())
              .periodTo(p.getPeriodTo()));
    }
    for (StaffUpadEntity u : upads.findTop10ByOrderByEntryDateDescIdDesc()) {
      AttendanceEmployeeEntity e = byCode.get(u.getEmpCode());
      recent.add(
          new RecentTransaction()
              .kind(RecentTransaction.KindEnum.UPAD)
              .id(u.getId())
              .empCode(u.getEmpCode())
              .employeeName(e == null ? u.getEmpCode() : e.getFullName())
              .photoThumb(e == null ? null : e.getPhotoThumb())
              .date(u.getEntryDate())
              .amount(u.getAmount())
              .paymentMode(PaymentMode.valueOf(u.getPaymentMode())));
    }
    recent.sort(Comparator.comparing(RecentTransaction::getDate).reversed());

    DayOfWeek off = weeklyOff();
    return new StaffDashboard()
        .date(today)
        .activeStaff(staff.size())
        .present(present)
        .atWork(atWork)
        .late(late)
        .absent(absent)
        .missedPunch(missed)
        .weeklyOff(off != null && today.getDayOfWeek() == off)
        .weekFrom(weekFrom)
        .weekTo(weekTo)
        .salaryThisWeek(round2(weekSalary))
        .paidThisWeek(round2(paidWeek))
        .upadThisWeek(round2(upadWeek))
        .totalBalance(round2(balance))
        .totalPendingLoan(round2(loan))
        .salaryNotSetCount(notSet)
        .lateToday(lateList)
        .absentToday(absentList)
        .missedPunchYesterday(missedList)
        .topBalances(balances.stream().filter(b -> b.getAmount() > 0).limit(8).toList())
        .recent(recent.stream().limit(10).toList());
  }

  private static StaffBrief brief(AttendanceEmployeeEntity e, String time, Double amount) {
    return new StaffBrief()
        .empCode(e.getEmpCode())
        .name(e.getFullName())
        .photoThumb(e.getPhotoThumb())
        .departmentName(e.getStaffDepartment() == null ? null : e.getStaffDepartment().getName())
        .time(time)
        .amount(amount == null ? null : round2(amount));
  }

  // ================================================================== loading

  private StaffLedger ledger(AttendanceEmployeeEntity e, LocalDate from, LocalDate to) {
    return ledgers(List.of(e), from, to).get(e.getEmpCode());
  }

  /** Bulk-load punches, edits, payments and upads for these workers over [from, to]. */
  private Map<String, StaffLedger> ledgers(
      Collection<AttendanceEmployeeEntity> list, LocalDate from, LocalDate to) {
    Set<String> codes = list.stream().map(AttendanceEmployeeEntity::getEmpCode).collect(Collectors.toSet());
    LocalDate today = WorkDay.today();
    LocalDate end = to.isAfter(today) ? to : today;
    Map<String, Map<LocalDate, List<LocalDateTime>>> punchMap = new HashMap<>();
    Map<String, Map<LocalDate, AttendanceAdjustmentEntity>> adjMap = new HashMap<>();
    Map<String, List<StaffSalaryPaymentEntity>> payMap = new HashMap<>();
    Map<String, List<StaffUpadEntity>> upadMap = new HashMap<>();
    if (!codes.isEmpty()) {
      List<AttendancePunchEntity> rows =
          codes.size() == 1
              ? punches.findByEmpCodeAndPunchTimeBetweenOrderByPunchTimeAsc(
                  codes.iterator().next(), WorkDay.startOf(from), WorkDay.endOf(end))
              : punches.findByPunchTimeBetweenOrderByPunchTimeAsc(
                  WorkDay.startOf(from), WorkDay.endOf(end));
      for (AttendancePunchEntity p : rows) {
        if (!codes.contains(p.getEmpCode())) continue;
        punchMap
            .computeIfAbsent(p.getEmpCode(), k -> new HashMap<>())
            .computeIfAbsent(WorkDay.of(p.getPunchTime()), k -> new ArrayList<>())
            .add(p.getPunchTime());
      }
      for (AttendanceAdjustmentEntity a : adjustments.findByEmpCodeInAndWorkDateBetween(codes, from, end)) {
        adjMap.computeIfAbsent(a.getEmpCode(), k -> new HashMap<>()).put(a.getWorkDate(), a);
      }
      for (StaffSalaryPaymentEntity p : payments.findByEmpCodeIn(codes)) {
        payMap.computeIfAbsent(p.getEmpCode(), k -> new ArrayList<>()).add(p);
      }
      for (StaffUpadEntity u : upads.findByEmpCodeIn(codes)) {
        upadMap.computeIfAbsent(u.getEmpCode(), k -> new ArrayList<>()).add(u);
      }
    }
    DayOfWeek off = weeklyOff();
    Map<String, StaffLedger> out = new HashMap<>();
    for (AttendanceEmployeeEntity e : list) {
      String c = e.getEmpCode();
      out.put(
          c,
          new StaffLedger(
              e, off, today, punchMap.get(c), adjMap.get(c), payMap.get(c), upadMap.get(c)));
    }
    return out;
  }

  /** Earliest date any of these workers' balances need attendance from. */
  private static LocalDate earliestStart(List<AttendanceEmployeeEntity> list, LocalDate today) {
    LocalDate earliest = today;
    for (AttendanceEmployeeEntity e : list) {
      if (e.getBalanceFrom() != null && e.getBalanceFrom().isBefore(earliest)) earliest = e.getBalanceFrom();
    }
    return earliest;
  }

  private Map<String, LocalDateTime> lastPunches() {
    Map<String, LocalDateTime> out = new HashMap<>();
    for (Object[] r : punches.findLatestPunchPerEmployee()) out.put((String) r[0], (LocalDateTime) r[1]);
    return out;
  }

  // ================================================================== pay periods

  private LocalDate[] suggestedPeriod(AttendanceEmployeeEntity e, LocalDate lastPaid, LocalDate today) {
    // First unpaid day: right after the last settled period, else where the ERP took over.
    PayMode mode = payMode(e);
    LocalDate start =
        lastPaid != null
            ? lastPaid.plusDays(1)
            : e.getBalanceFrom() != null ? e.getBalanceFrom() : periodStart(today, mode);
    return new LocalDate[] {start, periodEnd(start, mode)};
  }

  LocalDate periodStart(LocalDate d, PayMode mode) {
    return switch (mode) {
      case DAILY -> d;
      case WEEKLY -> d.with(TemporalAdjusters.previousOrSame(weekStart()));
      case FIFTEEN_DAYS -> d.getDayOfMonth() <= 15 ? d.withDayOfMonth(1) : d.withDayOfMonth(16);
      case MONTHLY -> d.withDayOfMonth(1);
    };
  }

  LocalDate periodEnd(LocalDate start, PayMode mode) {
    return switch (mode) {
      case DAILY -> start;
      case WEEKLY ->
          start.with(TemporalAdjusters.previousOrSame(weekStart())).plusDays(6);
      case FIFTEEN_DAYS ->
          start.getDayOfMonth() <= 15 ? start.withDayOfMonth(15) : start.with(TemporalAdjusters.lastDayOfMonth());
      case MONTHLY -> start.with(TemporalAdjusters.lastDayOfMonth());
    };
  }

  // ================================================================== helpers

  private static void checkRange(LocalDate from, LocalDate to) {
    if (from == null || to == null) throw bad("Pick a start and end date");
    if (to.isBefore(from)) throw bad("End date is before the start date");
    if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) throw bad("Pick a shorter date range");
  }

  private static LocalDate min(LocalDate a, LocalDate b) {
    if (a == null) return b;
    if (b == null) return a;
    return a.isBefore(b) ? a : b;
  }

  private static LocalTime time(String v, String what) {
    if (isBlank(v)) throw bad(what + " is required");
    try {
      return LocalTime.parse(v.trim().length() == 4 ? "0" + v.trim() : v.trim()).withSecond(0).withNano(0);
    } catch (DateTimeParseException e) {
      throw bad(what + " must look like 08:30");
    }
  }

  private static String hm(LocalTime t) {
    return t == null ? null : String.format("%02d:%02d", t.getHour(), t.getMinute());
  }

  private static String hhmm(int minutes) {
    return String.format("%02d:%02d", minutes / 60, minutes % 60);
  }

  private static double nz(Double v) {
    return v == null ? 0 : v;
  }

  private static String trim(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
