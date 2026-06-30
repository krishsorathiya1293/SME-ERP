package com.erp.formsmanagement.service.client.impl;

import com.erp.api.clientmanagement.model.ClientInventory;
import com.erp.api.clientmanagement.model.NewClientInventory;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.client.ClientInventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.repository.client.ClientInventoryRepository;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.mapper.client.ClientInventoryMapper;
import com.erp.formsmanagement.service.client.ClientInventoryService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientInventoryServiceImpl implements ClientInventoryService {

  private final ClientInventoryRepository clientInventoryRepository;
  private final PartyRepository partyRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;
  private final ClientInventoryMapper clientInventoryMapper;

  public ClientInventoryServiceImpl(
      ClientInventoryRepository clientInventoryRepository,
      PartyRepository partyRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository,
      ClientInventoryMapper clientInventoryMapper) {
    this.clientInventoryRepository = clientInventoryRepository;
    this.partyRepository = partyRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
    this.clientInventoryMapper = clientInventoryMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientInventory> getAll(
      Long clientId, Optional<Long> sizeId, Optional<String> search) {
    if (!partyRepository.existsById(clientId)) {
      throw new EntityNotFoundException("Client (Party) not found with id: " + clientId);
    }

    Specification<ClientInventoryEntity> spec =
        Specification.where(clientInventoryRepository.filterByClientId(clientId))
            .and(clientInventoryRepository.filterBySearch(search));

    return clientInventoryRepository.findAll(spec).stream()
        .filter(this::differsFromBase)
        .map(clientInventoryMapper::toDomain)
        .toList();
  }

  /**
   * A client inventory row is only "customized" when at least one of its non-null <b>pricing</b>
   * values differs from the corresponding base inventory value. Packing fields (pcsPerBox,
   * boxPerCarton, pcsPerCarton, cartonWeight) are ignored here — the import copies them in for
   * every row, so they are not a reliable signal of a deliberate price customization. A null client
   * field means "inherit base" and is never a difference. Rows with no base inventory are always
   * considered custom.
   */
  private boolean differsFromBase(ClientInventoryEntity ci) {
    InventoryEntity base = ci.getSize() == null ? null : ci.getSize().getInventory();
    if (base == null) {
      return true;
    }
    return overrides(ci.getSssatinlacq(), base.getSs())
        || overrides(ci.getAntiq(), base.getAntiq())
        || overrides(ci.getSidegold(), base.getSidegold())
        || overrides(ci.getSartinlacq(), base.getSartinlacq())
        || overrides(ci.getZblack(), base.getZblack())
        || overrides(ci.getGrblack(), base.getGrblack())
        || overrides(ci.getMattss(), base.getMattss())
        || overrides(ci.getMattantiq(), base.getMattantiq())
        || overrides(ci.getPvdrose(), base.getPvdrose())
        || overrides(ci.getPvdgold(), base.getPvdgold())
        || overrides(ci.getPvdblack(), base.getPvdblack())
        || overrides(ci.getRosegold(), base.getRosegold())
        || overrides(ci.getClearlacq(), base.getClearlacq());
  }

  /** True when the client supplied a non-null value that differs from the base value. */
  private boolean overrides(Object clientValue, Object baseValue) {
    return clientValue != null && !Objects.equals(clientValue, baseValue);
  }

  @Override
  public ClientInventory getById(Long clientId, Long id) {
    return clientInventoryRepository
        .findByIdAndParty_Id(id, clientId)
        .map(clientInventoryMapper::toDomain)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Client inventory not found with id: " + id + " for client: " + clientId));
  }

  @Override
  public ClientInventory create(Long clientId, NewClientInventory request) {
    ClientInventoryEntity entity =
        clientInventoryRepository
            .findByParty_IdAndSize_Id(clientId, request.getSizeId())
            .orElseGet(ClientInventoryEntity::new);
    clientInventoryMapper.updateEntity(entity, request);
    linkPartyAndSize(entity, clientId, request);
    return clientInventoryMapper.toDomain(clientInventoryRepository.save(entity));
  }

  @Override
  public ClientInventory update(Long clientId, Long id, NewClientInventory request) {
    ClientInventoryEntity entity =
        clientInventoryRepository
            .findByParty_IdAndSize_Id(clientId, request.getSizeId())
            .orElseGet(ClientInventoryEntity::new);
    clientInventoryMapper.updateEntity(entity, request);
    linkPartyAndSize(entity, clientId, request);
    return clientInventoryMapper.toDomain(clientInventoryRepository.save(entity));
  }

  @Override
  public void deleteById(Long clientId, Long id) {
    ClientInventoryEntity entity =
        clientInventoryRepository
            .findByIdAndParty_Id(id, clientId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Client inventory not found with id: " + id + " for client: " + clientId));
    clientInventoryRepository.delete(entity);
  }

  private void linkPartyAndSize(
      ClientInventoryEntity entity, Long clientId, NewClientInventory request) {
    PartyEntity party =
        partyRepository
            .findById(clientId)
            .orElseThrow(
                () -> new EntityNotFoundException("Client (Party) not found with id: " + clientId));

    ItemBlueprintDataEntity size =
        itemBlueprintDataRepository
            .findById(request.getSizeId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException("Size not found with id: " + request.getSizeId()));

    entity.setParty(party);
    entity.setSize(size);
  }
}
