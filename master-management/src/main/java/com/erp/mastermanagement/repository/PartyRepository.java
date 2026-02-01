package com.erp.mastermanagement.repository;

import com.erp.mastermanagement.domain.PartyEntity;
import com.erp.repository.CoreRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyRepository extends CoreRepository<PartyEntity, Long> {}
