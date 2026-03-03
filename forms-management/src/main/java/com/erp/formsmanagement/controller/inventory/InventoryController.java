package com.erp.formsmanagement.controller.inventory;

import com.erp.api.itemmanagement.InventoryItemManagementApi;
import com.erp.api.itemmanagement.model.Inventory;
import com.erp.api.itemmanagement.model.NewInventory;
import com.erp.api.itemmanagement.model.PaginatedResultInventory;
import com.erp.controller.AbstractCrudControllerV2;
import com.erp.formsmanagement.domain.entity.inventory.filter.InventoryFilter;
import com.erp.formsmanagement.service.inventory.InventoryService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController
    extends AbstractCrudControllerV2<Long, NewInventory, Inventory, InventoryFilter, PaginatedResultInventory>
    implements InventoryItemManagementApi {

  public InventoryController(InventoryService s) {
    super(s, s);
  }

  @Override
  public ResponseEntity<Inventory> createInventory(Long itemId, NewInventory newInventory) {
    return crud().createOne(itemId, newInventory);
  }

  @Override
  public ResponseEntity<Void> deleteInventory(Long itemId, Long id) {
    return crud().delete(itemId, id);
  }

  @Override
  public ResponseEntity<PaginatedResultInventory> getAllInventory(
      Long itemId,
      Optional<String> itemName,
      Optional<String> sizeInInch,
      Optional<String> sizeInMm,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    InventoryFilter filter =
        new InventoryFilter(itemName.orElse(null), sizeInInch.orElse(null), sizeInMm.orElse(null));
    return page().getAll(
        itemId, GetAllQuery.of(Optional.of(filter), search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<Inventory> getInventoryById(Long itemId, Long id) {
    return crud().getById(itemId, id);
  }

  @Override
  public ResponseEntity<Inventory> updateInventory(Long itemId, Long id, NewInventory newInventory) {
    return crud().update(itemId, id, newInventory);
  }
}
