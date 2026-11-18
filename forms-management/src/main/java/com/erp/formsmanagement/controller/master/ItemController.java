package com.erp.formsmanagement.controller.master;

import com.erp.api.mastermanagement.ItemMasterManagementApi;
import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewItem;
import com.erp.api.mastermanagement.model.NewStockEntry;
import com.erp.api.mastermanagement.model.PaginatedResultItem;
import com.erp.api.mastermanagement.model.StockEntry;
import com.erp.controller.AbstractCrudControllerV1;
import com.erp.formsmanagement.service.master.ItemService;
import com.erp.formsmanagement.service.master.StockEntryService;
import com.erp.util.GetAllQuery;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ItemController
    extends AbstractCrudControllerV1<NewItem, Item, String, PaginatedResultItem>
    implements ItemMasterManagementApi {

  private final StockEntryService stockEntries;

  public ItemController(ItemService s, StockEntryService stockEntries) {
    super(s, s);
    this.stockEntries = stockEntries;
  }

  @Override
  public ResponseEntity<List<StockEntry>> getStockEntries(Long sizeId) {
    return ResponseEntity.ok(stockEntries.list(sizeId));
  }

  @Override
  public ResponseEntity<Item> createStockEntry(NewStockEntry newStockEntry) {
    return ResponseEntity.status(HttpStatus.CREATED).body(stockEntries.create(newStockEntry));
  }

  @Override
  public ResponseEntity<Item> createItem(NewItem newItem) {
    return crud().createOne(newItem);
  }

  @Override
  public ResponseEntity<Void> deleteItem(Long id) {
    return crud().delete(id);
  }

  @Override
  public ResponseEntity<PaginatedResultItem> getAllItems(
      Optional<String> filterByStatus,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page().getAll(GetAllQuery.of(filterByStatus, search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<Item> getItemById(Long id) {
    return crud().getById(id);
  }

  @Override
  public ResponseEntity<Item> updateItem(Long id, NewItem newItem) {
    return crud().update(id, newItem);
  }
}
