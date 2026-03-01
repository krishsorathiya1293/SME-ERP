package com.erp.formsmanagement.service.inventory;

import com.erp.api.itemmanagement.model.Item;
import java.util.List;

public interface ItemBlueprintService {
  List<Item> getAllItems(String search);
  Item getItemById(Long id);
  Item createItem(com.erp.api.itemmanagement.model.NewItem newItem);
  Item updateItem(Long id, com.erp.api.itemmanagement.model.NewItem newItem);
  void deleteItem(Long id);
}
