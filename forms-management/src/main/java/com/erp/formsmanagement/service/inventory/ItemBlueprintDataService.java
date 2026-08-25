package com.erp.formsmanagement.service.inventory;

import com.erp.api.itemmanagement.model.NewSize;
import com.erp.api.itemmanagement.model.Size;
import com.erp.api.itemmanagement.model.UpdateSizeHighlight;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;
import java.util.List;

public interface ItemBlueprintDataService
    extends CoreServiceV2<Long, NewSize, Size, Long>, GetAllServiceV2<Long, Void, List<Size>> {

  /**
   * Sets or clears a stock-master row's highlight.
   *
   * <p>Keyed on the size alone, and separate from the ordinary size update: marking a row is done
   * from the grid by someone who has the row in front of them and no reason to resend its
   * dimensions to colour it. Colour and note move together — a coloured row nobody can explain is
   * worse than no colour at all — so clearing the colour clears the note with it.
   */
  Size updateHighlight(Long sizeId, UpdateSizeHighlight request);
}
