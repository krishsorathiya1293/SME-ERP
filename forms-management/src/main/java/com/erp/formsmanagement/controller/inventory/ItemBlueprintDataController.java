package com.erp.formsmanagement.controller.inventory;

import com.erp.api.itemmanagement.SizeItemManagementApi;
import com.erp.api.itemmanagement.model.NewSize;
import com.erp.api.itemmanagement.model.Size;
import com.erp.api.itemmanagement.model.UpdateSizeHighlight;
import com.erp.controller.AbstractCrudControllerV2;
import com.erp.formsmanagement.service.inventory.ItemBlueprintDataService;
import com.erp.util.GetAllQuery;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ItemBlueprintDataController
    extends AbstractCrudControllerV2<Long, NewSize, Size, Void, List<Size>>
    implements SizeItemManagementApi {

  private final ItemBlueprintDataService sizeService;

  public ItemBlueprintDataController(ItemBlueprintDataService s) {
    super(s, s);
    this.sizeService = s;
  }

  /**
   * Marks a stock-master row, or clears the mark. Keyed on the size alone — the grid colours a row
   * it already has in front of it, and has no reason to resend the row's dimensions to do so.
   */
  @PutMapping("/api/v1/sizes/{sizeId}/highlight")
  public ResponseEntity<Size> updateSizeHighlight(
      @PathVariable Long sizeId, @RequestBody UpdateSizeHighlight request) {
    return ResponseEntity.ok(sizeService.updateHighlight(sizeId, request));
  }

  @Override
  public ResponseEntity<Size> createSize(Long itemId, NewSize newSize) {
    return crud().createOne(itemId, newSize);
  }

  @Override
  public ResponseEntity<Size> updateSize(Long itemId, Long sizeId, NewSize newSize) {
    return crud().update(itemId, sizeId, newSize);
  }

  @Override
  public ResponseEntity<Void> deleteSize(Long itemId, Long sizeId) {
    return crud().delete(itemId, sizeId);
  }

  @Override
  public ResponseEntity<Size> getSizeById(Long itemId, Long sizeId) {
    return crud().getById(itemId, sizeId);
  }

  @Override
  public ResponseEntity<List<Size>> getSizesByItemId(Long itemId, Optional<String> search) {
    return page().getAll(itemId, GetAllQuery.of(search));
  }
}
