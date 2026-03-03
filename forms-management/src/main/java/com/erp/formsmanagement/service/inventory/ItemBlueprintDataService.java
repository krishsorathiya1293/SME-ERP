package com.erp.formsmanagement.service.inventory;

import com.erp.api.itemmanagement.model.NewSize;
import com.erp.api.itemmanagement.model.Size;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;
import java.util.List;

public interface ItemBlueprintDataService
    extends CoreServiceV2<Long, NewSize, Size, Long>, GetAllServiceV2<Long, Void, List<Size>> {}
