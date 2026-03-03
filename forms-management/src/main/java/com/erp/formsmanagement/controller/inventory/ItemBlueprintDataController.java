package com.erp.formsmanagement.controller.inventory;

import com.erp.api.itemmanagement.SizeItemManagementApi;
import com.erp.api.itemmanagement.model.NewSize;
import com.erp.api.itemmanagement.model.Size;
import com.erp.controller.AbstractCrudControllerV2;
import com.erp.formsmanagement.service.inventory.ItemBlueprintDataService;
import com.erp.util.GetAllQuery;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ItemBlueprintDataController
    extends AbstractCrudControllerV2<Long, NewSize, Size, Void, List<Size>>
    implements SizeItemManagementApi {

  public ItemBlueprintDataController(ItemBlueprintDataService s) {
    super(s, s);
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
