package com.erp.formsmanagement.domain.repository.attendance;

import com.erp.formsmanagement.domain.entity.attendance.AttendancePunchEntity;
import com.erp.repository.CoreRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendancePunchRepository extends CoreRepository<AttendancePunchEntity, Long> {

  /** Inserts one punch unless (emp_code, punch_time, terminal_sn) is already there. 1 = new. */
  @Modifying
  @Query(
      value =
          """
          INSERT INTO attendance_punch
              (emp_code, punch_time, punch_state, verify_type, terminal_sn, terminal_alias,
               source_id, created_at, last_updated_at)
          VALUES (:empCode, :punchTime, :punchState, :verifyType, :terminalSn, :terminalAlias,
                  :sourceId, :now, :now)
          ON CONFLICT ON CONSTRAINT uq_attendance_punch DO NOTHING
          """,
      nativeQuery = true)
  int insertIgnoringDuplicate(
      @Param("empCode") String empCode,
      @Param("punchTime") LocalDateTime punchTime,
      @Param("punchState") String punchState,
      @Param("verifyType") Integer verifyType,
      @Param("terminalSn") String terminalSn,
      @Param("terminalAlias") String terminalAlias,
      @Param("sourceId") Long sourceId,
      @Param("now") LocalDateTime now);

  List<AttendancePunchEntity> findByPunchTimeBetweenOrderByPunchTimeAsc(
      LocalDateTime from, LocalDateTime to);

  List<AttendancePunchEntity> findByEmpCodeAndPunchTimeBetweenOrderByPunchTimeAsc(
      String empCode, LocalDateTime from, LocalDateTime to);

  /** Who punched at least once in the window — the "active" employees expected to turn up. */
  @Query(
      "select distinct p.empCode from AttendancePunchEntity p"
          + " where p.punchTime between :from and :to")
  List<String> findEmpCodesWithPunchesBetween(
      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  /** [empCode, first-ever punchTime] — a new joiner isn't absent before they joined. */
  @Query(
      "select p.empCode, min(p.punchTime) from AttendancePunchEntity p"
          + " where p.empCode in :codes group by p.empCode")
  List<Object[]> findFirstPunchFor(@Param("codes") Collection<String> codes);

  /** [empCode, latest punchTime] per employee. */
  @Query("select p.empCode, max(p.punchTime) from AttendancePunchEntity p group by p.empCode")
  List<Object[]> findLatestPunchPerEmployee();

  @Query("select max(p.punchTime) from AttendancePunchEntity p")
  LocalDateTime findLatestPunchTime();

  @Query("select max(p.createdAt) from AttendancePunchEntity p")
  LocalDateTime findLatestCreatedAt();
}
