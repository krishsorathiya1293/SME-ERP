package com.erp.formsmanagement.service.master.impl;

import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.mapper.master.PartyMapper;
import com.erp.formsmanagement.service.master.PartyService;
import com.erp.util.GetAllQuery;
import com.erp.wrappers.CreateOne;
import com.erp.wrappers.CreateResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartyServiceImpl implements PartyService {
  private final PartyRepository partyRepository;
  private final PartyMapper partyMapper;

  @Override
  public List<Party> getAll(GetAllQuery<String> query) {
    return partyRepository
        .findAll(partyRepository.filter(query.filter().orElse(null), query.search().orElse(null)))
        .stream()
        .map(partyMapper::toDomain)
        .toList();
  }

  @Override
  public Party getById(Long id) {
    return partyRepository
        .findById(id)
        .map(partyMapper::toDomain)
        .orElseThrow(
            () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
  }

  @Override
  public CreateResult<Party> save(NewParty request) {
    return new CreateOne<>(
        partyMapper.toDomain(partyRepository.save(partyMapper.toEntity(request))));
  }

  @Override
  public Party update(Long id, NewParty request) {
    PartyEntity entity =
        partyRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
    partyMapper.updateEntity(entity, request);
    return partyMapper.toDomain(partyRepository.save(entity));
  }

  @Override
  public void deleteById(Long id) {
    partyRepository.deleteById(id);
  }
}
