package com.erp.formsmanagement.domain.repository.attendance;

import com.erp.formsmanagement.domain.entity.attendance.AttendanceAdjustmentEntity;
import com.erp.repository.CoreRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceAdjustmentRepository extends CoreRepository<AttendanceAdjustmentEntity, Long> {
  Optional<AttendanceAdjustmentEntity> findByEmpCodeAndWorkDate(String empCode, LocalDate workDate);

  List<AttendanceAdjustmentEntity> findByEmpCodeInAndWorkDateBetween(
      Collection<String> empCodes, LocalDate from, LocalDate to);

  List<AttendanceAdjustmentEntity> findByWorkDateBetween(LocalDate from, LocalDate to);
}
