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
import com.erp.mapper.EntityMapper;
import com.erp.service.AbstractCrudServiceV2;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientInventoryServiceImpl
    extends AbstractCrudServiceV2<ClientInventoryEntity, NewClientInventory, ClientInventory, Long>
    implements ClientInventoryService {

  private final ClientInventoryRepository clientInventoryRepository;
  private final InventoryRepository inventoryRepository;
  private final PartyRepository partyRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;
  private final ClientInventoryMapper clientInventoryMapper;

  @Override
  protected JpaRepository<ClientInventoryEntity, Long> repository() {
    return clientInventoryRepository;
  }

  @Override
  protected EntityMapper<ClientInventoryEntity, NewClientInventory, ClientInventory> mapper() {
    return clientInventoryMapper;
  }

  @Override
  protected void afterCreate(ClientInventoryEntity entity, Long clientId, NewClientInventory request) {
    linkPartyAndSize(entity, clientId, request);
  }

  @Override
  protected void afterUpdate(ClientInventoryEntity entity, Long clientId, NewClientInventory request) {
    linkPartyAndSize(entity, clientId, request);
  }

  private void linkPartyAndSize(ClientInventoryEntity entity, Long clientId, NewClientInventory request) {
    PartyEntity party = partyRepository
        .findById(clientId)
        .orElseThrow(() -> new EntityNotFoundException("Client (Party) not found with id: " + clientId));

    ItemBlueprintDataEntity size = itemBlueprintDataRepository
        .findById(request.getSizeId())
        .orElseThrow(() -> new EntityNotFoundException("Size not found with id: " + request.getSizeId()));

    entity.setParty(party);
    entity.setSize(size);
  }

  @Override
  public ClientInventory getById(Long clientId, Long id) {
    return clientInventoryRepository
        .findByIdAndParty_Id(id, clientId)
        .map(clientInventoryMapper::toDomain)
        .orElseThrow(() -> new EntityNotFoundException(
            "Client inventory not found with id: " + id + " for client: " + clientId));
  }

  /**
   * Upsert by clientId + sizeId (natural key).
   * If a client override already exists for this size → update it.
   * If not → create a new override (first-time override of a base inventory row).
   */
  @Override
  public ClientInventory update(Long clientId, Long id, NewClientInventory request) {
    ClientInventoryEntity entity = clientInventoryRepository
        .findByParty_IdAndSize_Id(clientId, request.getSizeId())
        .orElseGet(ClientInventoryEntity::new);
    clientInventoryMapper.updateEntity(entity, request);
    afterUpdate(entity, clientId, request);
    return clientInventoryMapper.toDomain(clientInventoryRepository.save(entity));
  }

  @Override
  public void deleteById(Long clientId, Long id) {
    ClientInventoryEntity entity = clientInventoryRepository
        .findByIdAndParty_Id(id, clientId)
        .orElseThrow(() -> new EntityNotFoundException(
            "Client inventory not found with id: " + id + " for client: " + clientId));
    clientInventoryRepository.delete(entity);
  }

  /**
   * Returns all base inventory rows (paginated), overlaying any client-specific overrides.
   * - If the client has a price override for a given size → shows client data (isOverridden=true)
   * - Otherwise → shows base inventory data (isOverridden=false)
   */
  @Override
  @Transactional(readOnly = true)
  public PaginatedResultClientInventory getAll(Long clientId, GetAllQuery<ClientInventoryFilter> query) {
    // 1. Load the client (party) entity for response mapping
    PartyEntity client = partyRepository
        .findById(clientId)
        .orElseThrow(() -> new EntityNotFoundException("Client (Party) not found with id: " + clientId));

    // 2. Paginate base inventory, applying optional search filter
    Specification<InventoryEntity> spec = Specification.where(
        inventoryRepository.filterBySearch(query.search()));

    Page<InventoryEntity> inventoryPage = inventoryRepository.findAll(
        spec,
        PaginationUtils.getPageRequest(query.page(), query.size(), query.direction(), query.sortBy()));

    // 3. Load all client overrides for this client, keyed by size_id
    Map<Long, ClientInventoryEntity> overridesBySizeId = clientInventoryRepository
        .findByParty_Id(clientId)
        .stream()
        .collect(Collectors.toMap(
            (ClientInventoryEntity e) -> e.getSize().getId(),
            (ClientInventoryEntity e) -> e));

    // 4. Merge: for each base inventory row, use override if present, else base data
    return PageMapper.toResult(
        inventoryPage,
        inv -> {
          Long sizeId = inv.getSize().getId();
          ClientInventoryEntity override = overridesBySizeId.get(sizeId);
          return override != null
              ? clientInventoryMapper.toDomain(override)
              : clientInventoryMapper.fromBaseInventory(inv, client);
        },
        PaginatedResultClientInventory::new,
        PaginatedResultClientInventory::setData);
  }
}
