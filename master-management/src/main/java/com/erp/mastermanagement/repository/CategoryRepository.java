package com.erp.mastermanagement.repository;

import com.erp.mastermanagement.domain.CategoryEntity;
import com.erp.repository.CoreRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends CoreRepository<CategoryEntity, Long> {}
