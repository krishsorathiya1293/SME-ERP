package com.erp.formsmanagement.controller.inventory;

import com.erp.api.itemmanagement.ItemItemManagementApi;
import com.erp.api.itemmanagement.model.ImportResult;
import com.erp.api.itemmanagement.model.Item;
import com.erp.api.itemmanagement.model.NewItem;
import com.erp.controller.AbstractCrudControllerV1;
import com.erp.formsmanagement.service.inventory.ItemBlueprintImportService;
import com.erp.formsmanagement.service.inventory.ItemBlueprintService;
import com.erp.util.GetAllQuery;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ItemBlueprintController
    extends AbstractCrudControllerV1<NewItem, Item, Void, List<Item>>
    implements ItemItemManagementApi {

  private final ItemBlueprintImportService itemBlueprintImportService;

  public ItemBlueprintController(
      ItemBlueprintService s, ItemBlueprintImportService itemBlueprintImportService) {
    super(s, s);
    this.itemBlueprintImportService = itemBlueprintImportService;
  }

  @Override
  public ResponseEntity<Item> createItem(NewItem newItem) {
    return crud().createOne(newItem);
  }

  @Override
  public ResponseEntity<Item> updateItem(Long id, NewItem newItem) {
    return crud().update(id, newItem);
  }

  @Override
  public ResponseEntity<Void> deleteItem(Long id) {
    return crud().delete(id);
  }

  @Override
  public ResponseEntity<Item> getItemById(Long id) {
    return crud().getById(id);
  }

  @Override
  public ResponseEntity<List<Item>> getAllItems(Optional<String> search) {
    return page().getAll(GetAllQuery.of(search));
  }

  @Override
  public ResponseEntity<ImportResult> importItemBlueprints(MultipartFile file) {
    return ResponseEntity.ok(itemBlueprintImportService.importStockMaster(file));
  }
}
