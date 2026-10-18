package com.erp.formsmanagement.domain.repository.attendance;

import com.erp.formsmanagement.domain.entity.attendance.StaffSalaryPaymentEntity;
import com.erp.repository.CoreRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffSalaryPaymentRepository extends CoreRepository<StaffSalaryPaymentEntity, Long> {
  List<StaffSalaryPaymentEntity> findByEmpCodeOrderByPeriodFromDescIdDesc(String empCode);

  List<StaffSalaryPaymentEntity> findByEmpCodeIn(Collection<String> empCodes);

  List<StaffSalaryPaymentEntity> findTop10ByOrderByPaidOnDescIdDesc();
}
