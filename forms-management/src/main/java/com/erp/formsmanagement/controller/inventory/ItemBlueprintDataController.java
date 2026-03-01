package com.erp.formsmanagement.controller.inventory;

import com.erp.api.itemmanagement.SizeItemManagementApi;
import com.erp.api.itemmanagement.model.Size;
import com.erp.formsmanagement.service.inventory.ItemBlueprintDataService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ItemBlueprintDataController implements SizeItemManagementApi {

  private final ItemBlueprintDataService itemBlueprintDataService;

  @Override
  public ResponseEntity<Size> getSizeById(Long itemId, Long sizeId) {
    return ResponseEntity.ok(itemBlueprintDataService.getSizeById(itemId, sizeId));
  }

  @Override
  public ResponseEntity<List<Size>> getSizesByItemId(Long itemId, Optional<String> search) {
    return ResponseEntity.ok(itemBlueprintDataService.getSizesByItemId(itemId, search.orElse(null)));
  }

  @Override
  public ResponseEntity<Size> createSize(Long itemId, com.erp.api.itemmanagement.model.NewSize newSize) {
    return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(itemBlueprintDataService.createSize(itemId, newSize));
  }

  @Override
  public ResponseEntity<Size> updateSize(Long itemId, Long sizeId, com.erp.api.itemmanagement.model.NewSize newSize) {
    return ResponseEntity.ok(itemBlueprintDataService.updateSize(itemId, sizeId, newSize));
  }

  @Override
  public ResponseEntity<Void> deleteSize(Long itemId, Long sizeId) {
    itemBlueprintDataService.deleteSize(itemId, sizeId);
    return ResponseEntity.noContent().build();
  }
}
