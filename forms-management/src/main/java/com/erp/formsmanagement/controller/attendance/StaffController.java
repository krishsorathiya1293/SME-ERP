package com.erp.formsmanagement.controller.attendance;

import com.erp.api.attendancemanagement.StaffAttendanceManagementApi;
import com.erp.api.attendancemanagement.model.AttendanceAdjustmentInput;
import com.erp.api.attendancemanagement.model.PaymentHistoryRow;
import com.erp.api.attendancemanagement.model.SalaryPayment;
import com.erp.api.attendancemanagement.model.SalaryPaymentInput;
import com.erp.api.attendancemanagement.model.SalaryStatement;
import com.erp.api.attendancemanagement.model.StaffDashboard;
import com.erp.api.attendancemanagement.model.StaffDay;
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
import com.erp.formsmanagement.service.attendance.StaffService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaffController implements StaffAttendanceManagementApi {

  private final StaffService service;

  public StaffController(StaffService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<StaffDashboard> getStaffDashboard() {
    return ResponseEntity.ok(service.dashboard());
  }

  @Override
  public ResponseEntity<StaffSettings> getStaffSettings() {
    return ResponseEntity.ok(service.getSettings());
  }

  @Override
  public ResponseEntity<StaffSettings> updateStaffSettings(StaffSettings staffSettings) {
    return ResponseEntity.ok(service.updateSettings(staffSettings));
  }

  @Override
  public ResponseEntity<List<StaffDepartment>> getStaffDepartments() {
    return ResponseEntity.ok(service.listDepartments());
  }

  @Override
  public ResponseEntity<StaffDepartment> createStaffDepartment(StaffDepartmentInput input) {
    return ResponseEntity.ok(service.createDepartment(input));
  }

  @Override
  public ResponseEntity<StaffDepartment> updateStaffDepartment(Long id, StaffDepartmentInput input) {
    return ResponseEntity.ok(service.updateDepartment(id, input));
  }

  @Override
  public ResponseEntity<Void> deleteStaffDepartment(Long id) {
    service.deleteDepartment(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<StaffSummary>> getStaffEmployees(Optional<Boolean> includeInactive) {
    return ResponseEntity.ok(service.listEmployees(includeInactive.orElse(false)));
  }

  @Override
  public ResponseEntity<StaffProfile> createStaffEmployee(StaffProfileInput input) {
    return ResponseEntity.ok(service.createEmployee(input));
  }

  @Override
  public ResponseEntity<StaffProfile> getStaffEmployee(String empCode) {
    return ResponseEntity.ok(service.getProfile(empCode));
  }

  @Override
  public ResponseEntity<StaffProfile> updateStaffEmployee(String empCode, StaffProfileInput input) {
    return ResponseEntity.ok(service.updateEmployee(empCode, input));
  }

  @Override
  public ResponseEntity<Void> deleteStaffEmployee(String empCode) {
    service.deleteEmployee(empCode);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<StaffProfile> setStaffEmployeeActive(String empCode, Boolean active) {
    return ResponseEntity.ok(service.setActive(empCode, Boolean.TRUE.equals(active)));
  }

  @Override
  public ResponseEntity<Void> updateStaffPhotos(String empCode, StaffPhotosInput input) {
    service.updatePhotos(empCode, input);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<StaffImage> getStaffAadharPhoto(String empCode) {
    return ResponseEntity.ok(service.aadharPhoto(empCode));
  }

  @Override
  public ResponseEntity<List<StaffDay>> getStaffDays(String empCode, LocalDate from, LocalDate to) {
    return ResponseEntity.ok(service.days(empCode, from, to));
  }

  @Override
  public ResponseEntity<StaffDay> adjustStaffDay(
      String empCode, LocalDate date, AttendanceAdjustmentInput input) {
    return ResponseEntity.ok(service.adjustDay(empCode, date, input));
  }

  @Override
  public ResponseEntity<StaffDay> clearStaffDayAdjustment(String empCode, LocalDate date) {
    return ResponseEntity.ok(service.clearAdjustment(empCode, date));
  }

  @Override
  public ResponseEntity<SalaryStatement> getSalaryStatement(
      String empCode, LocalDate from, LocalDate to) {
    return ResponseEntity.ok(service.statement(empCode, from, to));
  }

  @Override
  public ResponseEntity<List<SalaryPayment>> getSalaryPayments(String empCode) {
    return ResponseEntity.ok(service.listPayments(empCode));
  }

  @Override
  public ResponseEntity<SalaryPayment> createSalaryPayment(String empCode, SalaryPaymentInput input) {
    return ResponseEntity.ok(service.createPayment(empCode, input));
  }

  @Override
  public ResponseEntity<Void> deleteSalaryPayment(Long id) {
    service.deletePayment(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<Upad>> getUpads(String empCode) {
    return ResponseEntity.ok(service.listUpads(empCode));
  }

  @Override
  public ResponseEntity<Upad> createUpad(String empCode, UpadInput input) {
    return ResponseEntity.ok(service.createUpad(empCode, input));
  }

  @Override
  public ResponseEntity<Void> deleteUpad(Long id) {
    service.deleteUpad(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<PaymentHistoryRow>> getPaymentHistory(
      String empCode, LocalDate from, LocalDate to) {
    return ResponseEntity.ok(service.paymentHistory(empCode, from, to));
  }
}
