package com.erp.formsmanagement.domain.repository.attendance;

import com.erp.formsmanagement.domain.entity.attendance.AttendanceEmployeeEntity;
import com.erp.repository.CoreRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceEmployeeRepository
    extends CoreRepository<AttendanceEmployeeEntity, Long> {
  List<AttendanceEmployeeEntity> findByEmpCodeIn(Collection<String> empCodes);
}
