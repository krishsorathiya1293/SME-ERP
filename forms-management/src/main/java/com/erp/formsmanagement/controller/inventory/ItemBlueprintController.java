package com.erp.formsmanagement.controller.inventory;

import com.erp.api.itemmanagement.ItemItemManagementApi;
import com.erp.api.itemmanagement.model.Item;
import com.erp.formsmanagement.service.inventory.ItemBlueprintService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ItemBlueprintController implements ItemItemManagementApi {

  private final ItemBlueprintService itemBlueprintService;

  @Override
  public ResponseEntity<List<Item>> getAllItems(Optional<String> search) {
    return ResponseEntity.ok(itemBlueprintService.getAllItems(search.orElse(null)));
  }

  @Override
  public ResponseEntity<Item> getItemById(Long id) {
    return ResponseEntity.ok(itemBlueprintService.getItemById(id));
  }

  @Override
  public ResponseEntity<Item> createItem(com.erp.api.itemmanagement.model.NewItem newItem) {
    return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(itemBlueprintService.createItem(newItem));
  }

  @Override
  public ResponseEntity<Item> updateItem(Long id, com.erp.api.itemmanagement.model.NewItem newItem) {
    return ResponseEntity.ok(itemBlueprintService.updateItem(id, newItem));
  }

  @Override
  public ResponseEntity<Void> deleteItem(Long id) {
    itemBlueprintService.deleteItem(id);
    return ResponseEntity.noContent().build();
  }
}
