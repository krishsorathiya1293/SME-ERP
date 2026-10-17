package com.erp.formsmanagement.controller.attendance;

import com.erp.api.attendancemanagement.AttendanceSyncAttendanceManagementApi;
import com.erp.api.attendancemanagement.model.EmployeeSyncBatch;
import com.erp.api.attendancemanagement.model.PunchSyncBatch;
import com.erp.api.attendancemanagement.model.SyncResult;
import com.erp.api.attendancemanagement.model.SyncStatus;
import com.erp.formsmanagement.service.attendance.AttendanceAgentHeartbeat;
import com.erp.formsmanagement.service.attendance.AttendanceService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Called by the Ishita HRMS desktop app on the factory PC. These paths are in
 * security.public-access (it syncs even when nobody is signed in); the shared ATTENDANCE_SYNC_KEY
 * in X-Sync-Key is the only gate.
 */
@RestController
public class AttendanceSyncController implements AttendanceSyncAttendanceManagementApi {

  private final AttendanceService service;
  private final AttendanceAgentHeartbeat heartbeat;
  private final byte[] syncKey;

  public AttendanceSyncController(
      AttendanceService service,
      AttendanceAgentHeartbeat heartbeat,
      @Value("${attendance.sync-key:}") String syncKey) {
    this.service = service;
    this.heartbeat = heartbeat;
    this.syncKey = syncKey.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public ResponseEntity<SyncResult> syncAttendancePunches(
      String xSyncKey, PunchSyncBatch punchSyncBatch) {
    checkKey(xSyncKey);
    return ResponseEntity.ok(service.syncPunches(punchSyncBatch.getPunches()));
  }

  @Override
  public ResponseEntity<SyncResult> syncAttendanceEmployees(
      String xSyncKey, EmployeeSyncBatch employeeSyncBatch) {
    checkKey(xSyncKey);
    return ResponseEntity.ok(service.syncEmployees(employeeSyncBatch.getEmployees()));
  }

  @Override
  public ResponseEntity<SyncStatus> getAttendanceSyncStatus(String xSyncKey) {
    checkKey(xSyncKey);
    return ResponseEntity.ok(service.status());
  }

  private void checkKey(String provided) {
    // An unset key disables sync entirely rather than accepting anything.
    if (syncKey.length == 0
        || provided == null
        || !MessageDigest.isEqual(syncKey, provided.getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid sync key");
    }
    heartbeat.touch();
  }
}
