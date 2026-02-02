package com.erp.mastermanagement.service.impl;

import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.mastermanagement.domain.PartyEntity;
import com.erp.mastermanagement.mapper.PartyMapper;
import com.erp.mastermanagement.repository.PartyRepository;
import com.erp.mastermanagement.service.PartyService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartyServiceImpl implements PartyService {
  private final PartyRepository partyRepository;
  private final PartyMapper partyMapper;

  @Override
  public List<Party> getAll(Optional<String> partyType, Optional<String> search) {

    return partyRepository
        .findAll(partyRepository.filter(partyType.orElse(null), search.orElse(null)))
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
  public Party save(NewParty request) {
    return partyMapper.toDomain(partyRepository.save(partyMapper.toEntity(request)));
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
