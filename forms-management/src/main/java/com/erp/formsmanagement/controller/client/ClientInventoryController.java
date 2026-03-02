package com.erp.formsmanagement.controller.client;

import com.erp.api.clientmanagement.ClientInventoryClientManagementApi;
import com.erp.api.clientmanagement.model.ClientInventory;
import com.erp.api.clientmanagement.model.NewClientInventory;
import com.erp.api.clientmanagement.model.PaginatedResultClientInventory;
import com.erp.controller.AbstractCrudControllerV2;
import com.erp.formsmanagement.domain.entity.client.filter.ClientInventoryFilter;
import com.erp.formsmanagement.service.client.ClientInventoryService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientInventoryController
    extends AbstractCrudControllerV2<
        Long,
        NewClientInventory,
        ClientInventory,
        ClientInventoryFilter,
        PaginatedResultClientInventory>
    implements ClientInventoryClientManagementApi {

  public ClientInventoryController(ClientInventoryService s) {
    super(s, s);
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
      Long sizeId,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page()
        .getAll(
            clientId,
            GetAllQuery.of(
                Optional.of(new ClientInventoryFilter(search.orElse(null), sizeId)),
                search,
                page,
                size,
                sortByFields,
                direction));
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
