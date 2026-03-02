package com.erp.formsmanagement.service.master.impl;

import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.mapper.master.PartyMapper;
import com.erp.formsmanagement.service.master.PartyService;
import com.erp.service.AbstractSpecificationServiceV1;
import com.erp.util.GetAllQuery;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PartyServiceImpl
    extends AbstractSpecificationServiceV1<PartyEntity, NewParty, Party>
    implements PartyService {

  private final PartyRepository partyRepository;

  public PartyServiceImpl(PartyRepository partyRepository, PartyMapper partyMapper) {
    super(partyRepository, partyMapper);
    this.partyRepository = partyRepository;
  }

  @Override
  public List<Party> getAll(GetAllQuery<String> query) {
    return partyRepository
        .findAll(partyRepository.filter(query.filter().orElse(null), query.search().orElse(null)))
        .stream()
        .map(e -> mapper().toDomain(e))
        .toList();
  }
}
