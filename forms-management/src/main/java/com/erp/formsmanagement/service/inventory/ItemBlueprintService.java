package com.erp.formsmanagement.service.inventory;

import com.erp.api.itemmanagement.model.Item;
import com.erp.api.itemmanagement.model.NewItem;
import com.erp.service.CoreServiceV1;
import com.erp.service.GetAllServiceV1;
import java.util.List;

public interface ItemBlueprintService
    extends CoreServiceV1<NewItem, Item, Long>, GetAllServiceV1<Void, List<Item>> {}
