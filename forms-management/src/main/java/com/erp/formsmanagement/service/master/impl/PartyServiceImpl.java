package com.erp.formsmanagement.service.master.impl;

import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.mapper.master.PartyMapper;
import com.erp.formsmanagement.service.master.PartyService;
import com.erp.mapper.EntityMapper;
import com.erp.service.AbstractCrudServiceV1;
import com.erp.util.GetAllQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartyServiceImpl
    extends AbstractCrudServiceV1<PartyEntity, NewParty, Party>
    implements PartyService {

  private final PartyRepository partyRepository;
  private final PartyMapper partyMapper;

  @Override
  protected JpaRepository<PartyEntity, Long> repository() {
    return partyRepository;
  }

  @Override
  protected EntityMapper<PartyEntity, NewParty, Party> mapper() {
    return partyMapper;
  }

  @Override
  public List<Party> getAll(GetAllQuery<String> query) {
    return partyRepository
        .findAll(partyRepository.filter(query.filter().orElse(null), query.search().orElse(null)))
        .stream()
        .map(partyMapper::toDomain)
        .toList();
  }
}
