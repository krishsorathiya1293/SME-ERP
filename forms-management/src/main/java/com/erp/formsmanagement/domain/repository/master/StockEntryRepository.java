package com.erp.formsmanagement.domain.repository.master;

import com.erp.formsmanagement.domain.entity.master.StockEntryEntity;
import com.erp.repository.CoreRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface StockEntryRepository extends CoreRepository<StockEntryEntity, Long> {
  List<StockEntryEntity> findByItem_IdOrderByEntryDateDescIdDesc(Long itemId);
}
