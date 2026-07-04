package com.erp.formsmanagement.domain.repository.master;

import com.erp.formsmanagement.domain.entity.master.PartyGroupEntity;
import com.erp.repository.CoreRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyGroupRepository extends CoreRepository<PartyGroupEntity, Long> {}
