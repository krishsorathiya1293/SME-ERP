package com.erp.formsmanagement.domain.repository.attendance;

import com.erp.formsmanagement.domain.entity.attendance.StaffDepartmentEntity;
import com.erp.repository.CoreRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffDepartmentRepository extends CoreRepository<StaffDepartmentEntity, Long> {
  List<StaffDepartmentEntity> findAllByOrderByNameAsc();

  Optional<StaffDepartmentEntity> findByNameIgnoreCase(String name);
}
