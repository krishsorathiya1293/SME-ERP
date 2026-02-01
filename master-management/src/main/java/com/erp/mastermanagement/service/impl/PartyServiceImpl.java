package com.erp.mastermanagement.service.impl;

import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
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
    var specification = org.springframework.data.jpa.domain.Specification.<PartyEntity>where(null);

    if (partyType.isPresent()) {
      specification = specification.and(
          (root, query, cb) -> cb.equal(root.get("partyType").get("name"), partyType.get()));
    }

    if (search.isPresent()) {
      specification = specification.and(
          (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + search.get().toLowerCase() + "%"));
    }

    return partyRepository.findAll(specification).stream().map(partyMapper::toDomain).toList();
  }

  @Override
  public Party getById(Long id) {
    return partyRepository
        .findById(id)
        .map(partyMapper::toDomain)
        .orElseThrow(() -> new RuntimeException("Party not found with id: " + id));
  }

  @Override
  public Party save(NewParty request) {
    var entity = partyMapper.toEntity(request);
    return partyMapper.toDomain(partyRepository.save(entity));
  }

  @Override
  public Party update(Long id, NewParty request) {
    var entity = partyRepository
        .findById(id)
        .orElseThrow(() -> new RuntimeException("Party not found with id: " + id));
    partyMapper.updateEntity(entity, request);
    return partyMapper.toDomain(partyRepository.save(entity));
  }

  @Override
  public void deleteById(Long id) {
    partyRepository.deleteById(id);
  }
}
