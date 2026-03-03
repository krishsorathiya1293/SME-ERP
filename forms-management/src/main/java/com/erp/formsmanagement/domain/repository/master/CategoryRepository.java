package com.erp.formsmanagement.domain.repository.master;

import com.erp.formsmanagement.domain.entity.master.CategoryEntity;
import com.erp.repository.CoreRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends CoreRepository<CategoryEntity, Long> {
  List<CategoryEntity> findByNameContainingIgnoreCase(String name);
}
