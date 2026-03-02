package com.erp.formsmanagement.controller.client;

import com.erp.api.clientmanagement.ClientInventoryClientManagementApi;
import com.erp.api.clientmanagement.model.ClientInventory;
import com.erp.api.clientmanagement.model.NewClientInventory;
import com.erp.api.clientmanagement.model.PaginatedResultClientInventory;
import com.erp.controller.GenericCrudDelegateV2;
import com.erp.controller.GetAllDelegateV2;
import com.erp.formsmanagement.domain.entity.client.filter.ClientInventoryFilter;
import com.erp.formsmanagement.service.client.ClientInventoryService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ClientInventoryController implements ClientInventoryClientManagementApi {

  private final ClientInventoryService clientInventoryService;

  private GenericCrudDelegateV2<Long, NewClientInventory, ClientInventory, Long> crud() {
    return new GenericCrudDelegateV2<>(clientInventoryService);
  }

  private GetAllDelegateV2<Long, ClientInventoryFilter, PaginatedResultClientInventory> page() {
    return new GetAllDelegateV2<>(clientInventoryService);
  }

  @Override
  public ResponseEntity<ClientInventory> createClientInventory(
      Long clientId, NewClientInventory newClientInventory) {
    return crud().createOne(clientId, newClientInventory);
  }

  @Override
  public ResponseEntity<Void> deleteClientInventory(Long clientId, Long inventoryId) {
    return crud().delete(clientId, inventoryId);
  }

  @Override
  public ResponseEntity<PaginatedResultClientInventory> getInventoryByClient(
      Long clientId,
      Optional<Long> sizeId,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page().getAll(
        clientId,
        GetAllQuery.of(Optional.of(new ClientInventoryFilter(search.orElse(null), sizeId.orElse(null))), search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<ClientInventory> getClientInventoryById(Long clientId, Long inventoryId) {
    return crud().getById(clientId, inventoryId);
  }

  @Override
  public ResponseEntity<ClientInventory> updateClientInventory(
      Long clientId, Long inventoryId, NewClientInventory newClientInventory) {
    return crud().update(clientId, inventoryId, newClientInventory);
  }
}
