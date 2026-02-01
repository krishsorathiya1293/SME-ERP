package com.erp.mastermanagement.repository;

import com.erp.mastermanagement.domain.SubCategoryEntity;
import com.erp.repository.CoreRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubCategoryRepository extends CoreRepository<SubCategoryEntity, Long> {}
