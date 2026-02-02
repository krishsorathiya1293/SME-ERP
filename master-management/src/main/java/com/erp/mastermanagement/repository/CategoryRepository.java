package com.erp.mastermanagement.repository;

import com.erp.mastermanagement.domain.CategoryEntity;
import com.erp.repository.CoreRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends CoreRepository<CategoryEntity, Long> {
  @Query(
      """
        select distinct c
        from CategoryEntity c
        left join c.subCategories sc
        where lower(c.name) like lower(concat('%', :search, '%'))
           or lower(sc.name) like lower(concat('%', :search, '%'))
    """)
  List<CategoryEntity> searchByCategoryOrSubCategory(@Param("search") String search);
}
