package com.erp.formsmanagement.domain.repository.master;

import com.erp.formsmanagement.domain.entity.master.TranslationEntity;
import com.erp.formsmanagement.domain.entity.master.TranslationType;
import com.erp.repository.CoreRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface TranslationRepository extends CoreRepository<TranslationEntity, Long> {
  List<TranslationEntity> findByTypeOrderBySourceTextAsc(TranslationType type);

  Optional<TranslationEntity> findByTypeAndSourceText(TranslationType type, String sourceText);

  Optional<TranslationEntity> findByPartyId(Long partyId);
}
