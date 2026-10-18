package com.erp.formsmanagement.domain.repository.attendance;

import com.erp.formsmanagement.domain.entity.attendance.StaffUpadEntity;
import com.erp.repository.CoreRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffUpadRepository extends CoreRepository<StaffUpadEntity, Long> {
  List<StaffUpadEntity> findByEmpCodeOrderByEntryDateDescIdDesc(String empCode);

  List<StaffUpadEntity> findByEmpCodeIn(Collection<String> empCodes);

  List<StaffUpadEntity> findTop10ByOrderByEntryDateDescIdDesc();
}
