package com.erp.formsmanagement.service.inventory;

import com.erp.api.itemmanagement.model.Size;
import java.util.List;

public interface ItemBlueprintDataService {
  List<Size> getSizesByItemId(Long itemId, String search);
  Size getSizeById(Long itemId, Long sizeId);
  Size createSize(Long itemId, com.erp.api.itemmanagement.model.NewSize newSize);
  Size updateSize(Long itemId, Long sizeId, com.erp.api.itemmanagement.model.NewSize newSize);
  void deleteSize(Long itemId, Long sizeId);
}
