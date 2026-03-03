package com.erp.formsmanagement.service.client.impl;

import com.erp.api.clientmanagement.model.ClientInventory;
import com.erp.api.clientmanagement.model.NewClientInventory;
import com.erp.api.clientmanagement.model.PaginatedResultClientInventory;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.client.ClientInventoryEntity;
import com.erp.formsmanagement.domain.entity.client.filter.ClientInventoryFilter;
import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.repository.client.ClientInventoryRepository;
import com.erp.formsmanagement.domain.repository.inventory.InventoryRepository;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.mapper.client.ClientInventoryMapper;
import com.erp.formsmanagement.service.client.ClientInventoryService;
import com.erp.service.AbstractSpecificationServiceV2;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientInventoryServiceImpl
    extends AbstractSpecificationServiceV2<
        ClientInventoryEntity, NewClientInventory, ClientInventory, Long>
    implements ClientInventoryService {

  private final ClientInventoryRepository clientInventoryRepository;
  private final InventoryRepository inventoryRepository;
  private final PartyRepository partyRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;

  public ClientInventoryServiceImpl(
      ClientInventoryRepository clientInventoryRepository,
      InventoryRepository inventoryRepository,
      PartyRepository partyRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository,
      ClientInventoryMapper clientInventoryMapper) {
    super(clientInventoryRepository, clientInventoryMapper);
    this.clientInventoryRepository = clientInventoryRepository;
    this.inventoryRepository = inventoryRepository;
    this.partyRepository = partyRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
  }

  @Override
  protected void afterCreate(
      ClientInventoryEntity entity, Long clientId, NewClientInventory request) {
    linkPartyAndSize(entity, clientId, request);
  }

  @Override
  protected void afterUpdate(
      ClientInventoryEntity entity, Long clientId, NewClientInventory request) {
    linkPartyAndSize(entity, clientId, request);
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

  @Override
  public ClientInventory getById(Long clientId, Long id) {
    return clientInventoryRepository
        .findByIdAndParty_Id(id, clientId)
        .map(mapper()::toDomain)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Client inventory not found with id: " + id + " for client: " + clientId));
  }

  @Override
  public ClientInventory update(Long clientId, Long id, NewClientInventory request) {
    ClientInventoryEntity entity =
        clientInventoryRepository
            .findByParty_IdAndSize_Id(clientId, request.getSizeId())
            .orElseGet(ClientInventoryEntity::new);
    mapper().updateEntity(entity, request);
    afterUpdate(entity, clientId, request);
    return mapper().toDomain(clientInventoryRepository.save(entity));
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

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultClientInventory getAll(
      Long clientId, GetAllQuery<ClientInventoryFilter> query) {
    // 1. Load the client (party) entity for response mapping
    PartyEntity client =
        partyRepository
            .findById(clientId)
            .orElseThrow(
                () -> new EntityNotFoundException("Client (Party) not found with id: " + clientId));

    Optional<Long> sizeId = query.filter().map(ClientInventoryFilter::sizeId);

    Specification<InventoryEntity> spec =
        Specification.where(inventoryRepository.filterBySearch(query.search()))
            .and(inventoryRepository.filterBySizeId(sizeId));

    Page<InventoryEntity> inventoryPage =
        inventoryRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    Map<Long, ClientInventoryEntity> overridesBySizeId =
        clientInventoryRepository.findByParty_Id(clientId).stream()
            .collect(Collectors.toMap(e -> e.getSize().getId(), e -> e));

    ClientInventoryMapper clientInventoryMapper = (ClientInventoryMapper) mapper();

    return PageMapper.toResult(
        inventoryPage,
        inv -> {
          Long invSizeId = inv.getSize().getId();
          ClientInventoryEntity override = overridesBySizeId.get(invSizeId);
          return override != null
              ? clientInventoryMapper.toDomain(override)
              : clientInventoryMapper.fromBaseInventory(inv, client);
        },
        PaginatedResultClientInventory::new,
        PaginatedResultClientInventory::setData);
  }
}
