package com.erp.formsmanagement.service.attendance;

import com.erp.api.attendancemanagement.model.AttendanceDay;
import com.erp.api.attendancemanagement.model.AttendanceDayStatus;
import com.erp.api.attendancemanagement.model.AttendanceEmployee;
import com.erp.api.attendancemanagement.model.AttendancePunch;
import com.erp.api.attendancemanagement.model.DailyAttendance;
import com.erp.api.attendancemanagement.model.EmployeeRecord;
import com.erp.api.attendancemanagement.model.MonthlyAttendance;
import com.erp.api.attendancemanagement.model.MonthlyAttendanceRow;
import com.erp.api.attendancemanagement.model.PunchRecord;
import com.erp.api.attendancemanagement.model.SyncResult;
import com.erp.api.attendancemanagement.model.SyncStatus;
import com.erp.api.attendancemanagement.model.StaffDay;
import com.erp.formsmanagement.domain.entity.attendance.AttendanceAdjustmentEntity;
import com.erp.formsmanagement.domain.entity.attendance.AttendanceEmployeeEntity;
import com.erp.formsmanagement.domain.entity.attendance.AttendancePunchEntity;
import com.erp.formsmanagement.domain.entity.attendance.WorkDay;
import com.erp.formsmanagement.domain.repository.attendance.AttendanceAdjustmentRepository;
import com.erp.formsmanagement.domain.repository.attendance.AttendanceEmployeeRepository;
import com.erp.formsmanagement.domain.repository.attendance.AttendancePunchRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Face-recognition attendance copied from the factory's EasyTime database by the Ishita HRMS
 * desktop app. EasyTime stays the source of truth: punches arrive in batches, are deduplicated on
 * (empCode, punchTime, terminalSn), and are only read here — never edited.
 *
 * <p>A day is read as: first punch = in, last punch = out. The device's in/out state is ignored —
 * workers routinely leave it on the wrong setting. A single punch is AT_WORK today and MISSED_PUNCH
 * on any earlier day. A day hand-edited in the staff profile replaces that day's punches here too,
 * and hours come from {@link StaffDayCalculator}, so this view and the profile agree. Days are the factory's 7 AM - 7 AM working days ({@link WorkDay}), so a
 * night shift (in 8 PM, out 3 AM) is one day on the date it started.
 *
 * <p>"Active" employees (the ones a missing punch makes ABSENT) are those who punched at least
 * once in the {@value #ACTIVE_WINDOW_DAYS} days up to the period — otherwise everyone who ever
 * left would show as absent forever. Days before someone's first-ever punch are NOT_JOINED.
 */
@Slf4j
@Service
@Transactional
public class AttendanceService {

  private static final long MAX_RANGE_DAYS = 62;
  static final int ACTIVE_WINDOW_DAYS = 30;

  private final AttendancePunchRepository punchRepository;
  private final AttendanceEmployeeRepository employeeRepository;
  private final AttendanceAdjustmentRepository adjustmentRepository;
  private final AttendanceAgentHeartbeat heartbeat;

  public AttendanceService(
      AttendancePunchRepository punchRepository,
      AttendanceEmployeeRepository employeeRepository,
      AttendanceAdjustmentRepository adjustmentRepository,
      AttendanceAgentHeartbeat heartbeat) {
    this.punchRepository = punchRepository;
    this.employeeRepository = employeeRepository;
    this.adjustmentRepository = adjustmentRepository;
    this.heartbeat = heartbeat;
  }

  // ------------------------------------------------------------------ sync (factory PC)

  public SyncResult syncPunches(List<PunchRecord> punches) {
    // JVM clock, like the audit listener uses for every other table, not the DB session's now().
    LocalDateTime now = LocalDateTime.now();
    int inserted = 0;
    for (PunchRecord p : punches) {
      if (isBlank(p.getEmpCode()) || isBlank(p.getPunchTime())) continue;
      inserted +=
          punchRepository.insertIgnoringDuplicate(
              p.getEmpCode().trim(),
              parseLocalDateTime(p.getPunchTime()),
              p.getPunchState(),
              p.getVerifyType(),
              p.getTerminalSn() == null ? "" : p.getTerminalSn().trim(),
              p.getTerminalAlias(),
              p.getSourceId(),
              now);
    }
    if (inserted > 0) log.info("Attendance sync: {} of {} punches new", inserted, punches.size());
    return new SyncResult().received(punches.size()).inserted(inserted);
  }

  public SyncResult syncEmployees(List<EmployeeRecord> employees) {
    List<String> codes =
        employees.stream()
            .map(EmployeeRecord::getEmpCode)
            .filter(c -> !isBlank(c))
            .map(String::trim)
            .toList();
    Map<String, AttendanceEmployeeEntity> existing =
        employeeRepository.findByEmpCodeIn(codes).stream()
            .collect(Collectors.toMap(AttendanceEmployeeEntity::getEmpCode, Function.identity()));

    List<AttendanceEmployeeEntity> changed = new ArrayList<>();
    for (EmployeeRecord r : employees) {
      if (isBlank(r.getEmpCode())) continue;
      String code = r.getEmpCode().trim();
      AttendanceEmployeeEntity e = existing.get(code);
      if (e == null) {
        e = new AttendanceEmployeeEntity();
        e.setEmpCode(code);
        existing.put(code, e);
      } else if (Objects.equals(e.getFirstName(), r.getFirstName())
          && Objects.equals(e.getLastName(), r.getLastName())
          && Objects.equals(e.getDepartment(), r.getDepartment())) {
        continue;
      }
      e.setFirstName(r.getFirstName());
      e.setLastName(r.getLastName());
      e.setDepartment(r.getDepartment());
      changed.add(e);
    }
    employeeRepository.saveAll(changed);
    return new SyncResult().received(employees.size()).inserted(changed.size());
  }

  @Transactional(readOnly = true)
  public SyncStatus status() {
    LocalDateTime lastPunch = punchRepository.findLatestPunchTime();
    LocalDateTime lastSynced = punchRepository.findLatestCreatedAt();
    return new SyncStatus()
        .totalPunches(punchRepository.count())
        .totalEmployees(employeeRepository.count())
        .lastPunchTime(lastPunch == null ? null : lastPunch.toString())
        .lastSyncedAt(
            lastSynced == null ? null : lastSynced.truncatedTo(ChronoUnit.SECONDS).toString())
        .agentLastSeenSecondsAgo(heartbeat.secondsAgo());
  }

  // ------------------------------------------------------------------ reads (ERP users)

  @Transactional(readOnly = true)
  public List<AttendancePunch> punches(LocalDate from, LocalDate to, String empCode) {
    if (to.isBefore(from)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'to' is before 'from'");
    }
    if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Date range is limited to " + MAX_RANGE_DAYS + " days");
    }
    List<AttendancePunchEntity> rows =
        isBlank(empCode)
            ? punchRepository.findByPunchTimeBetweenOrderByPunchTimeAsc(
                startOf(from), endOf(to))
            : punchRepository.findByEmpCodeAndPunchTimeBetweenOrderByPunchTimeAsc(
                empCode.trim(), startOf(from), endOf(to));
    Map<String, AttendanceEmployeeEntity> employees = employeesFor(codesOf(rows));
    return rows.stream()
        .map(
            p ->
                new AttendancePunch()
                    .id(p.getId())
                    .empCode(p.getEmpCode())
                    .employeeName(nameOf(employees.get(p.getEmpCode())))
                    .punchTime(p.getPunchTime().toString())
                    .punchState(p.getPunchState())
                    .verifyType(p.getVerifyType())
                    .terminalSn(p.getTerminalSn())
                    .terminalAlias(p.getTerminalAlias()))
        .toList();
  }

  /** Every active employee for one day, present or not. */
  @Transactional(readOnly = true)
  public List<DailyAttendance> daily(LocalDate date, String weeklyOff) {
    DayOfWeek off = parseWeeklyOff(weeklyOff);
    Map<String, Map<LocalDate, List<LocalDateTime>>> byEmp = groupByEmployeeAndDay(date, date);
    Map<String, Map<LocalDate, AttendanceAdjustmentEntity>> edits = adjustmentsFor(date, date);
    Set<String> codes = activeCodes(date, date, presentIn(byEmp, edits));
    Map<String, AttendanceEmployeeEntity> employees = employeesFor(codes);
    Map<String, LocalDate> joined = firstPunchDates(codes);

    List<DailyAttendance> result = new ArrayList<>();
    for (String code : codes) {
      AttendanceEmployeeEntity emp = employees.get(code);
      DayRead day = read(date, emp, byEmp.get(code), edits.get(code));
      result.add(
          new DailyAttendance()
              .empCode(code)
              .employeeName(nameOf(emp))
              .department(emp == null ? null : emp.getDepartmentName())
              .status(statusOf(date, day, off, joined.get(code)))
              .firstIn(day.firstIn())
              .lastOut(day.lastOut())
              .punchCount(day.count())
              .workedMinutes(day.workedMinutes())
              .punches(day.times())
              .manual(day.manual()));
    }
    result.sort(
        Comparator.comparing(
            (DailyAttendance d) -> sortName(d.getEmployeeName(), d.getEmpCode()),
            String.CASE_INSENSITIVE_ORDER));
    return result;
  }

  /** The monthly register: every active employee x every day of the month. */
  @Transactional(readOnly = true)
  public MonthlyAttendance monthly(String month, String weeklyOff) {
    YearMonth ym = parseMonth(month);
    DayOfWeek off = parseWeeklyOff(weeklyOff);
    LocalDate first = ym.atDay(1);
    LocalDate last = ym.atEndOfMonth();

    Map<String, Map<LocalDate, List<LocalDateTime>>> byEmp = groupByEmployeeAndDay(first, last);
    Map<String, Map<LocalDate, AttendanceAdjustmentEntity>> edits = adjustmentsFor(first, last);
    Set<String> codes = activeCodes(first, last, presentIn(byEmp, edits));
    Map<String, AttendanceEmployeeEntity> employees = employeesFor(codes);
    Map<String, LocalDate> joined = firstPunchDates(codes);

    List<MonthlyAttendanceRow> rows = new ArrayList<>();
    for (String code : codes) {
      AttendanceEmployeeEntity emp = employees.get(code);
      MonthlyAttendanceRow row =
          new MonthlyAttendanceRow()
              .empCode(code)
              .employeeName(nameOf(emp))
              .department(emp == null ? null : emp.getDepartmentName());
      int present = 0, missed = 0, absent = 0, minutes = 0;
      List<AttendanceDay> days = new ArrayList<>();
      for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
        DayRead day = read(d, emp, byEmp.get(code), edits.get(code));
        AttendanceDayStatus st = statusOf(d, day, off, joined.get(code));
        switch (st) {
          case PRESENT, AT_WORK -> present++;
          case MISSED_PUNCH -> missed++;
          case ABSENT -> absent++;
          default -> {}
        }
        minutes += day.workedMinutes();
        days.add(
            new AttendanceDay()
                .date(d)
                .status(st)
                .firstIn(day.firstIn())
                .lastOut(day.lastOut())
                .punchCount(day.count())
                .workedMinutes(day.workedMinutes())
                .manual(day.manual()));
      }
      rows.add(
          row.presentDays(present)
              .missedPunchDays(missed)
              .absentDays(absent)
              .totalWorkedMinutes(minutes)
              .days(days));
    }
    rows.sort(
        Comparator.comparing(
            (MonthlyAttendanceRow r) -> sortName(r.getEmployeeName(), r.getEmpCode()),
            String.CASE_INSENSITIVE_ORDER));
    return new MonthlyAttendance().month(ym.toString()).daysInMonth(ym.lengthOfMonth()).rows(rows);
  }

  /** Everyone the device knows (synced roster + anyone who punched without being in it). */
  @Transactional(readOnly = true)
  public List<AttendanceEmployee> employees() {
    Map<String, LocalDateTime> latest = new HashMap<>();
    for (Object[] r : punchRepository.findLatestPunchPerEmployee()) {
      latest.put((String) r[0], (LocalDateTime) r[1]);
    }
    Map<String, AttendanceEmployee> out = new TreeMap<>();
    for (AttendanceEmployeeEntity e : employeeRepository.findAll()) {
      LocalDateTime t = latest.get(e.getEmpCode());
      out.put(
          e.getEmpCode(),
          new AttendanceEmployee()
              .empCode(e.getEmpCode())
              .employeeName(e.getFullName())
              .department(e.getDepartmentName())
              .lastPunchTime(t == null ? null : t.toString()));
    }
    latest.forEach(
        (code, t) ->
            out.computeIfAbsent(
                code,
                c -> new AttendanceEmployee().empCode(c).lastPunchTime(t.toString())));
    return out.values().stream()
        .sorted(
            Comparator.comparing(
                (AttendanceEmployee a) -> sortName(a.getEmployeeName(), a.getEmpCode()),
                String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  // ------------------------------------------------------------------ helpers

  /**
   * One employee's day as the ERP reads it: the device punches, or the hand edit that replaces
   * them. {@code times} are HH:mm in order; worked is after the break, as on the profile.
   */
  record DayRead(List<String> times, int workedMinutes, boolean manual, boolean markedAbsent) {
    static final DayRead NONE = new DayRead(List.of(), 0, false, false);

    int count() {
      return times.size();
    }

    String firstIn() {
      return times.isEmpty() ? null : times.get(0);
    }

    String lastOut() {
      return times.size() < 2 ? null : times.get(times.size() - 1);
    }
  }

  private static DayRead read(
      LocalDate date,
      AttendanceEmployeeEntity emp,
      Map<LocalDate, List<LocalDateTime>> punches,
      Map<LocalDate, AttendanceAdjustmentEntity> edits) {
    AttendanceAdjustmentEntity edit = edits == null ? null : edits.get(date);
    List<LocalDateTime> dayPunches = punches == null ? null : punches.get(date);
    if (edit == null && dayPunches == null) return DayRead.NONE;
    StaffDay d = StaffDayCalculator.calculate(date, dayPunches, edit, ratesOf(emp), WorkDay.today());
    return new DayRead(
        d.getPunches(),
        d.getWorkedMinutes() == null ? 0 : d.getWorkedMinutes(),
        edit != null,
        edit != null && edit.isAbsent());
  }

  /** Only the department matters for hours (its break); pay isn't shown here. */
  private static StaffDayCalculator.Rates ratesOf(AttendanceEmployeeEntity e) {
    return new StaffDayCalculator.Rates(
        e == null ? null : e.getStaffDepartment(), null, null, null, null, null);
  }

  private Map<String, Map<LocalDate, List<LocalDateTime>>> groupByEmployeeAndDay(
      LocalDate from, LocalDate to) {
    Map<String, Map<LocalDate, List<LocalDateTime>>> byEmp = new HashMap<>();
    for (AttendancePunchEntity p :
        punchRepository.findByPunchTimeBetweenOrderByPunchTimeAsc(startOf(from), endOf(to))) {
      byEmp
          .computeIfAbsent(p.getEmpCode(), k -> new HashMap<>())
          .computeIfAbsent(WorkDay.of(p.getPunchTime()), k -> new ArrayList<>())
          .add(p.getPunchTime());
    }
    return byEmp;
  }

  private Map<String, Map<LocalDate, AttendanceAdjustmentEntity>> adjustmentsFor(
      LocalDate from, LocalDate to) {
    Map<String, Map<LocalDate, AttendanceAdjustmentEntity>> out = new HashMap<>();
    for (AttendanceAdjustmentEntity a : adjustmentRepository.findByWorkDateBetween(from, to)) {
      out.computeIfAbsent(a.getEmpCode(), k -> new HashMap<>()).put(a.getWorkDate(), a);
    }
    return out;
  }

  /** Who was at work in the period: punched, or given times by hand. */
  private static Set<String> presentIn(
      Map<String, Map<LocalDate, List<LocalDateTime>>> byEmp,
      Map<String, Map<LocalDate, AttendanceAdjustmentEntity>> edits) {
    Set<String> codes = new LinkedHashSet<>(byEmp.keySet());
    edits.forEach(
        (code, days) -> {
          if (days.values().stream().anyMatch(a -> !a.isAbsent())) codes.add(code);
        });
    return codes;
  }

  /** Employees who punched in the period, plus those who punched in the 30 days before it. */
  private Set<String> activeCodes(LocalDate from, LocalDate to, Set<String> punchedInPeriod) {
    Set<String> codes = new LinkedHashSet<>(punchedInPeriod);
    codes.addAll(
        punchRepository.findEmpCodesWithPunchesBetween(
            startOf(from.minusDays(ACTIVE_WINDOW_DAYS)), endOf(to)));
    // Deleted profiles never show; de-activated ones only on days they actually punched.
    for (AttendanceEmployeeEntity e : employeeRepository.findByEmpCodeIn(codes)) {
      if (e.isDeleted() || (!e.isActive() && !punchedInPeriod.contains(e.getEmpCode()))) {
        codes.remove(e.getEmpCode());
      }
    }
    return codes;
  }

  private static AttendanceDayStatus statusOf(
      LocalDate date, DayRead day, DayOfWeek off, LocalDate joinedOn) {
    LocalDate today = WorkDay.today();
    if (day.markedAbsent()) return AttendanceDayStatus.ABSENT;
    if (day.count() > 0) {
      if (day.count() >= 2) return AttendanceDayStatus.PRESENT;
      // One punch today just means they haven't left yet.
      return date.equals(today) ? AttendanceDayStatus.AT_WORK : AttendanceDayStatus.MISSED_PUNCH;
    }
    if (date.isAfter(today)) return AttendanceDayStatus.UPCOMING;
    if (joinedOn == null || date.isBefore(joinedOn)) return AttendanceDayStatus.NOT_JOINED;
    if (off != null && date.getDayOfWeek() == off) return AttendanceDayStatus.WEEKLY_OFF;
    return AttendanceDayStatus.ABSENT;
  }

  private Map<String, LocalDate> firstPunchDates(Set<String> codes) {
    if (codes.isEmpty()) return Map.of();
    Map<String, LocalDate> out = new HashMap<>();
    for (Object[] r : punchRepository.findFirstPunchFor(codes)) {
      out.put((String) r[0], WorkDay.of((LocalDateTime) r[1]));
    }
    return out;
  }

  private Map<String, AttendanceEmployeeEntity> employeesFor(Set<String> codes) {
    if (codes.isEmpty()) return Map.of();
    return employeeRepository.findByEmpCodeIn(codes).stream()
        .collect(Collectors.toMap(AttendanceEmployeeEntity::getEmpCode, Function.identity()));
  }

  private static Set<String> codesOf(List<AttendancePunchEntity> rows) {
    return rows.stream()
        .map(AttendancePunchEntity::getEmpCode)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static String nameOf(AttendanceEmployeeEntity e) {
    return e == null ? null : e.getFullName();
  }

  private static String sortName(String name, String code) {
    return name == null ? "~" + code : name; // unnamed codes after the named people
  }

  /** A day here is the factory's 7 AM - 7 AM working day ({@link WorkDay}). */
  private static LocalDateTime startOf(LocalDate d) {
    return WorkDay.startOf(d);
  }

  private static LocalDateTime endOf(LocalDate d) {
    return WorkDay.endOf(d);
  }

  static YearMonth parseMonth(String month) {
    try {
      return YearMonth.parse(month.trim());
    } catch (DateTimeParseException | NullPointerException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must be yyyy-MM");
    }
  }

  static DayOfWeek parseWeeklyOff(String weeklyOff) {
    if (isBlank(weeklyOff)) return null;
    try {
      return DayOfWeek.valueOf(weeklyOff.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown weeklyOff: " + weeklyOff);
    }
  }

  private static LocalDateTime parseLocalDateTime(String value) {
    String v = value.trim().replace(' ', 'T');
    // Drop any zone suffix the sender's driver adds; the factory wall-clock time is what we keep.
    int zone = Math.max(v.indexOf('+', 10), v.indexOf('Z', 10));
    if (zone > 0) v = v.substring(0, zone);
    try {
      return LocalDateTime.parse(v).truncatedTo(ChronoUnit.SECONDS);
    } catch (DateTimeParseException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad punchTime: " + value);
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
