package com.erp.formsmanagement.clientportal.service.impl;

import com.erp.api.clientportalmanagement.model.ClientCredentials;
import com.erp.api.clientportalmanagement.model.Company;
import com.erp.api.clientportalmanagement.model.NewPartyGroup;
import com.erp.api.clientportalmanagement.model.PartyGroup;
import com.erp.api.clientportalmanagement.model.SetGroupPartiesRequest;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.clientportal.service.PartyGroupService;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.master.PartyGroupEntity;
import com.erp.formsmanagement.domain.repository.master.PartyGroupRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.usermanagement.model.entity.UserEntity;
import com.erp.usermanagement.repository.UserRepository;
import com.erp.usermanagement.service.UserService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartyGroupServiceImpl implements PartyGroupService {

  private final PartyGroupRepository partyGroupRepository;
  private final PartyRepository partyRepository;
  private final UserRepository userRepository;
  private final UserService userService;

  @Override
  @Transactional(readOnly = true)
  public List<PartyGroup> getAll() {
    return partyGroupRepository.findAll().stream().map(this::toDomain).toList();
  }

  @Override
  @Transactional
  public PartyGroup create(NewPartyGroup request) {
    PartyGroupEntity entity = new PartyGroupEntity();
    entity.setName(request.getName());
    entity.setEmail(request.getEmail());
    entity.setContactNo(request.getContactNo());

    PartyGroupEntity saved = partyGroupRepository.save(entity);
    userService.registerGroupUser(saved.getId(), saved.getName());
    return toDomain(saved);
  }

  @Override
  @Transactional
  public PartyGroup setParties(Long groupId, SetGroupPartiesRequest request) {
    PartyGroupEntity group = getGroup(groupId);
    Set<Long> desired = new HashSet<>(request.getPartyIds());

    // Detach parties that are no longer members.
    for (PartyEntity current : partyRepository.findByGroupId(groupId)) {
      if (!desired.contains(current.getId())) {
        current.setGroupId(null);
        partyRepository.save(current);
      }
    }

    // Attach the desired parties. Each party keeps its own login working; the group login is an
    // additional way to reach all member companies.
    for (Long partyId : desired) {
      PartyEntity party =
          partyRepository
              .findById(partyId)
              .orElseThrow(
                  () -> new EntityNotFoundException("Party not found with id: " + partyId));
      if (party.getGroupId() != null && !party.getGroupId().equals(groupId)) {
        throw new IllegalArgumentException(
            "Party " + partyId + " already belongs to another group");
      }
      party.setGroupId(groupId);
      partyRepository.save(party);
    }

    return toDomain(group);
  }

  @Override
  @Transactional
  public void assignPartyToGroup(Long partyId, Long groupId) {
    PartyEntity party =
        partyRepository
            .findById(partyId)
            .orElseThrow(() -> new EntityNotFoundException("Party not found with id: " + partyId));

    if (groupId != null) {
      getGroup(groupId); // validate the target group exists
    }
    // The party keeps its own login regardless; the group login is an additional shared access path.
    party.setGroupId(groupId);
    partyRepository.save(party);
  }

  @Override
  @Transactional
  public ClientCredentials resetCredentials(Long groupId) {
    getGroup(groupId);
    UserEntity user = userService.resetGroupCredentials(groupId);
    return new ClientCredentials().username(user.getUsername()).password(user.getInitialPassword());
  }

  private PartyGroupEntity getGroup(Long id) {
    return partyGroupRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Party group not found with id: " + id));
  }

  private PartyGroup toDomain(PartyGroupEntity entity) {
    PartyGroup dto =
        new PartyGroup()
            .id(entity.getId())
            .name(entity.getName())
            .email(entity.getEmail())
            .contactNo(entity.getContactNo())
            .createdAt(toOffsetDateTime(entity.getCreatedAt()))
            .parties(
                partyRepository.findByGroupId(entity.getId()).stream()
                    .map(p -> new Company().partyId(p.getId()).partyName(p.getName()))
                    .toList());

    userRepository
        .findByGroupId(entity.getId())
        .ifPresent(
            user ->
                dto.username(user.getUsername())
                    .credentialsPending(user.getInitialPassword() != null)
                    .initialPassword(user.getInitialPassword()));

    return dto;
  }

  private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime createdAt) {
    return createdAt == null ? null : createdAt.atOffset(ZoneOffset.UTC);
  }
}
