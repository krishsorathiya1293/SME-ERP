package com.erp.formsmanagement.service.client.impl;

import com.erp.api.clientmanagement.model.ClientInventory;
import com.erp.api.clientmanagement.model.NewClientInventory;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.client.ClientInventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.repository.client.ClientInventoryRepository;
import com.erp.formsmanagement.domain.repository.inventory.InventoryRepository;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.mapper.client.ClientInventoryMapper;
import com.erp.formsmanagement.service.client.ClientInventoryService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientInventoryServiceImpl implements ClientInventoryService {

  private final ClientInventoryRepository clientInventoryRepository;
  private final InventoryRepository inventoryRepository;
  private final PartyRepository partyRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;
  private final ClientInventoryMapper clientInventoryMapper;

  public ClientInventoryServiceImpl(
      ClientInventoryRepository clientInventoryRepository,
      InventoryRepository inventoryRepository,
      PartyRepository partyRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository,
      ClientInventoryMapper clientInventoryMapper) {
    this.clientInventoryRepository = clientInventoryRepository;
    this.inventoryRepository = inventoryRepository;
    this.partyRepository = partyRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
    this.clientInventoryMapper = clientInventoryMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientInventory> getAll(
      Long clientId, Optional<Long> sizeId, Optional<String> search) {
    PartyEntity client =
        partyRepository
            .findById(clientId)
            .orElseThrow(
                () -> new EntityNotFoundException("Client (Party) not found with id: " + clientId));

    Specification<InventoryEntity> spec =
        Specification.where(inventoryRepository.filterBySearch(search))
            .and(inventoryRepository.filterBySizeId(sizeId));

    List<InventoryEntity> allInventory = inventoryRepository.findAll(spec);

    Map<Long, ClientInventoryEntity> overridesBySizeId =
        clientInventoryRepository.findByParty_Id(clientId).stream()
            .collect(Collectors.toMap(e -> e.getSize().getId(), e -> e));

    return allInventory.stream()
        .map(inv -> {
          Long invSizeId = inv.getSize().getId();
          ClientInventoryEntity override = overridesBySizeId.get(invSizeId);
          return override != null
              ? clientInventoryMapper.toDomain(override)
              : clientInventoryMapper.fromBaseInventory(inv, client);
        })
        .toList();
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
