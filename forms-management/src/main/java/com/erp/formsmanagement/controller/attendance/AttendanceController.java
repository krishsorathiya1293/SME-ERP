package com.erp.formsmanagement.controller.attendance;

import com.erp.api.attendancemanagement.AttendanceAttendanceManagementApi;
import com.erp.api.attendancemanagement.model.AttendanceEmployee;
import com.erp.api.attendancemanagement.model.AttendancePunch;
import com.erp.api.attendancemanagement.model.DailyAttendance;
import com.erp.api.attendancemanagement.model.MonthlyAttendance;
import com.erp.api.attendancemanagement.model.SyncStatus;
import com.erp.formsmanagement.service.attendance.AttendanceExcelExporter;
import com.erp.formsmanagement.service.attendance.AttendanceService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttendanceController implements AttendanceAttendanceManagementApi {

  private final AttendanceService service;
  private final AttendanceExcelExporter excelExporter;

  public AttendanceController(AttendanceService service, AttendanceExcelExporter excelExporter) {
    this.service = service;
    this.excelExporter = excelExporter;
  }

  @Override
  public ResponseEntity<List<DailyAttendance>> getDailyAttendance(
      LocalDate date, Optional<String> weeklyOff) {
    return ResponseEntity.ok(service.daily(date, weeklyOff.orElse(null)));
  }

  @Override
  public ResponseEntity<MonthlyAttendance> getMonthlyAttendance(
      String month, Optional<String> weeklyOff) {
    return ResponseEntity.ok(service.monthly(month, weeklyOff.orElse(null)));
  }

  @Override
  public ResponseEntity<Resource> exportMonthlyAttendance(
      String month, Optional<String> weeklyOff) {
    MonthlyAttendance register = service.monthly(month, weeklyOff.orElse(null));
    byte[] workbook = excelExporter.export(register);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("attendance-" + register.getMonth() + ".xlsx")
                .build()
                .toString())
        .body(new ByteArrayResource(workbook));
  }

  @Override
  public ResponseEntity<List<AttendancePunch>> getAttendancePunches(
      LocalDate from, LocalDate to, Optional<String> empCode) {
    return ResponseEntity.ok(service.punches(from, to, empCode.orElse(null)));
  }

  @Override
  public ResponseEntity<List<AttendanceEmployee>> getAttendanceEmployees() {
    return ResponseEntity.ok(service.employees());
  }

  @Override
  public ResponseEntity<SyncStatus> getAttendanceHealth() {
    return ResponseEntity.ok(service.status());
  }
}
