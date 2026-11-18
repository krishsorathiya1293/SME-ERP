package com.erp.formsmanagement.domain.repository.attendance;

import com.erp.formsmanagement.domain.entity.attendance.StaffBonusJamaEntity;
import com.erp.repository.CoreRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffBonusJamaRepository extends CoreRepository<StaffBonusJamaEntity, Long> {
  List<StaffBonusJamaEntity> findByEmpCodeOrderByEntryDateDescIdDesc(String empCode);

  List<StaffBonusJamaEntity> findByEmpCodeIn(Collection<String> empCodes);

  List<StaffBonusJamaEntity> findByEntryDateBetween(LocalDate from, LocalDate to);

  List<StaffBonusJamaEntity> findTop10ByOrderByEntryDateDescIdDesc();
}
