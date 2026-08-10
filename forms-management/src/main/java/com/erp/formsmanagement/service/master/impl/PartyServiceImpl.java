package com.erp.formsmanagement.service.master.impl;

import com.erp.api.mastermanagement.model.NewParty;
import com.erp.api.mastermanagement.model.Party;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.master.PartyGroupEntity;
import com.erp.formsmanagement.domain.repository.master.PartyGroupRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.mapper.master.PartyMapper;
import com.erp.formsmanagement.service.master.PartyService;
import com.erp.service.AbstractSpecificationServiceV1;
import com.erp.usermanagement.service.UserService;
import com.erp.util.GetAllQuery;
import com.erp.wrappers.CreateOne;
import com.erp.wrappers.CreateResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartyServiceImpl
    extends AbstractSpecificationServiceV1<PartyEntity, NewParty, Party>
    implements PartyService {

  private final PartyRepository partyRepository;
  private final PartyGroupRepository partyGroupRepository;
  private final UserService userService;

  public PartyServiceImpl(
      PartyRepository partyRepository,
      PartyGroupRepository partyGroupRepository,
      PartyMapper partyMapper,
      UserService userService) {
    super(partyRepository, partyMapper);
    this.partyRepository = partyRepository;
    this.partyGroupRepository = partyGroupRepository;
    this.userService = userService;
  }

  @Override
  @Transactional
  public CreateResult<Party> save(NewParty request) {
    PartyEntity savedEntity = partyRepository.save(mapper().toEntity(request));
    userService.registerClientUser(savedEntity.getId(), savedEntity.getName());
    return new CreateOne<>(mapper().toDomain(savedEntity));
  }

  /**
   * Deletes a party. Every party gets an auto-created CLIENT login on {@link #save}, and its
   * {@code users.party_id} foreign key has no cascade — so that login must be cleared first or the
   * delete fails even for a party with no orders. Transactional/order-carrying foreign keys (orders,
   * job_works, packing_invoice, sales_orders, gres_fillings) stay {@code ON DELETE RESTRICT}, so a
   * party still referenced by real records raises a foreign-key violation that the global handler
   * surfaces as a clean 409. Running in one transaction keeps it atomic — if the party delete is
   * blocked, the login removal rolls back too.
   */
  @Override
  @Transactional
  public void deleteById(Long id) {
    userService.deleteClientUserByPartyId(id);
    partyRepository.deleteById(id);
  }

  @Override
  public List<Party> getAll(GetAllQuery<String> query) {
    Map<Long, String> groupNames =
        partyGroupRepository.findAll().stream()
            .collect(Collectors.toMap(PartyGroupEntity::getId, PartyGroupEntity::getName));

    return partyRepository
        .findAll(partyRepository.filter(query.filter().orElse(null), query.search().orElse(null)))
        .stream()
        .map(
            e -> {
              Party dto = mapper().toDomain(e);
              if (e.getGroupId() != null) {
                dto.setGroupName(groupNames.get(e.getGroupId()));
              }
              return dto;
            })
        .toList();
  }
}
