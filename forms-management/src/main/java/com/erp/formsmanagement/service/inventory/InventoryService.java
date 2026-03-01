package com.erp.formsmanagement.service.inventory;

import com.erp.api.itemmanagement.model.Inventory;
import com.erp.api.itemmanagement.model.NewInventory;
import com.erp.api.itemmanagement.model.PaginatedResultInventory;
import com.erp.formsmanagement.domain.entity.inventory.filter.InventoryFilter;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;

public interface InventoryService
    extends CoreServiceV2<Long, NewInventory, Inventory, Long>,
        GetAllServiceV2<Long, InventoryFilter, PaginatedResultInventory> {}
