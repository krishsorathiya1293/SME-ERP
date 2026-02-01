package com.erp.mastermanagement.controller;

import com.erp.api.mastermanagement.ItemMasterManagementApi;
import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewItem;
import com.erp.api.mastermanagement.model.PaginatedResultItem;
import com.erp.mastermanagement.service.ItemService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ItemController implements ItemMasterManagementApi {

  private final ItemService itemService;

  @Override
  public ResponseEntity<Item> createItem(NewItem newItem) {
    return ResponseEntity.ok(itemService.save(newItem));
  }

  @Override
  public ResponseEntity<Void> deleteItem(Long id) {
    itemService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<PaginatedResultItem> getAllItems(
      Optional<String> filterByStatus,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return ResponseEntity.ok(
        itemService.getAll(filterByStatus, search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<Item> getItemById(Long id) {
    return ResponseEntity.ok(itemService.getById(id));
  }

  @Override
  public ResponseEntity<Item> updateItem(Long id, NewItem newItem) {
    return ResponseEntity.ok(itemService.update(id, newItem));
  }
}
