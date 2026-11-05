package com.erp.formsmanagement.service.master;

import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewStockEntry;
import com.erp.api.mastermanagement.model.NewStockEntryType;
import com.erp.api.mastermanagement.model.StockEntry;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.ItemEntity;
import com.erp.formsmanagement.domain.entity.master.StockEntryEntity;
import com.erp.formsmanagement.domain.entity.master.StockStatus;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.ItemRepository;
import com.erp.formsmanagement.domain.repository.master.StockEntryRepository;
import com.erp.formsmanagement.mapper.master.ItemMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Stock of a size, kept by peti. The kg typed in comes off the client's weighing machine with the
 * petis on it, so petis x empty peti weight is taken off to get the net kg; pcs = net kg / weight
 * per pc, rounded down, as the stock dialog always worked it out.
 *
 * <p>ADD puts an entry on top of the stock ("10 peti there, 4 more came"); SET replaces it (a
 * recount). Every entry is kept with its date and the stock right after it.
 */
@Service
@Transactional
public class StockEntryService {

  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final ItemRepository items;
  private final StockEntryRepository entries;
  private final ItemBlueprintDataRepository sizes;
  private final ItemMapper itemMapper;

  public StockEntryService(
      ItemRepository items,
      StockEntryRepository entries,
      ItemBlueprintDataRepository sizes,
      ItemMapper itemMapper) {
    this.items = items;
    this.entries = entries;
    this.sizes = sizes;
    this.itemMapper = itemMapper;
  }

  @Transactional(readOnly = true)
  public List<StockEntry> list(Long sizeId) {
    return items
        .findFirstBySize_IdOrderByIdAsc(sizeId)
        .map(i -> entries.findByItem_IdOrderByEntryDateDescIdDesc(i.getId()))
        .orElse(List.of())
        .stream()
        .map(StockEntryService::toDomain)
        .toList();
  }

  public Item create(NewStockEntry in) {
    ItemBlueprintDataEntity size =
        sizes
            .findById(in.getSizeId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Size not found"));
    ItemEntity item =
        items.findFirstBySize_IdOrderByIdAsc(size.getId()).orElseGet(() -> newItem(size));

    boolean add = in.getType() == NewStockEntryType.ADD;
    int peti = in.getPetiCount() == null ? 0 : in.getPetiCount();
    double gross = in.getGrossKg() == null ? 0 : in.getGrossKg();
    double emptyWt =
        in.getEmptyPetiWeight() != null
            ? in.getEmptyPetiWeight()
            : item.getEmptyPetiWeight() == null ? 0 : item.getEmptyPetiWeight();
    double wpc = in.getWeightPerPc() == null ? 0 : in.getWeightPerPc();
    if (peti < 0 || gross < 0 || emptyWt < 0 || wpc < 0) throw bad("Values can't be negative");

    double net = round3(gross - peti * emptyWt);
    if (net < 0) {
      throw bad(
          "Empty peti weight ("
              + round3(peti * emptyWt)
              + " kg) is more than the "
              + round3(gross)
              + " kg typed in");
    }
    if (add && peti == 0 && gross == 0) throw bad("Enter the petis and kg to add");
    if (net > 0 && wpc <= 0) throw bad("Enter the weight per pc");

    int oldPeti = item.getPetiCount() == null ? 0 : item.getPetiCount();
    double oldKg = item.getItemKg() == null ? 0 : item.getItemKg();
    int totalPeti = add ? oldPeti + peti : peti;
    double totalKg = add ? round3(oldKg + net) : net;
    double totalPc = pcs(totalKg, wpc);

    // A SET that leaves the stock as it was only changed settings (low stock warning, etc.).
    boolean stockChanged =
        add
            || totalPeti != oldPeti
            || totalKg != round3(oldKg)
            || !Objects.equals(item.getTotalPc(), totalPc);

    item.setPetiCount(totalPeti);
    item.setEmptyPetiWeight(emptyWt);
    item.setWeightPerPc(wpc);
    item.setItemKg(totalKg);
    item.setTotalPc(totalPc);
    if (in.getLowStockWarning() != null) item.setLowStockWarning(in.getLowStockWarning());
    if (item.getStockStatus() == null) item.setStockStatus(StockStatus.IN_STOCK);
    item = items.save(item);

    if (stockChanged) {
      StockEntryEntity e = new StockEntryEntity();
      e.setItem(item);
      e.setEntryType(in.getType().name());
      e.setEntryDate(in.getEntryDate() == null ? LocalDate.now(IST) : in.getEntryDate());
      e.setPetiCount(peti);
      e.setGrossKg(round3(gross));
      e.setEmptyPetiWeight(emptyWt);
      e.setNetKg(net);
      e.setWeightPerPc(wpc);
      e.setPcs(pcs(net, wpc));
      e.setTotalPetiAfter(totalPeti);
      e.setTotalKgAfter(totalKg);
      e.setTotalPcAfter(totalPc);
      e.setNote(trim(in.getNote()));
      entries.save(e);
    }
    return itemMapper.toDomain(item);
  }

  private static ItemEntity newItem(ItemBlueprintDataEntity size) {
    ItemEntity i = new ItemEntity();
    i.setSize(size);
    i.setItemKg(0d);
    i.setTotalPc(0d);
    i.setPetiCount(0);
    i.setLowStockWarning(0d);
    i.setStockStatus(StockStatus.IN_STOCK);
    return i;
  }

  /** Whole pieces in {@code kg}: kg / weight per pc, rounded down (with slack for float error). */
  static double pcs(double kg, double wpc) {
    return kg > 0 && wpc > 0 ? Math.floor(kg / wpc + 1e-9) : 0;
  }

  static double round3(double v) {
    return Math.round(v * 1000d) / 1000d;
  }

  private static String trim(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  private static ResponseStatusException bad(String msg) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
  }

  private static StockEntry toDomain(StockEntryEntity e) {
    return new StockEntry()
        .id(e.getId())
        .type(NewStockEntryType.valueOf(e.getEntryType()))
        .entryDate(e.getEntryDate())
        .petiCount(e.getPetiCount())
        .grossKg(e.getGrossKg())
        .emptyPetiWeight(e.getEmptyPetiWeight())
        .netKg(e.getNetKg())
        .weightPerPc(e.getWeightPerPc())
        .pcs(e.getPcs())
        .totalPetiAfter(e.getTotalPetiAfter())
        .totalKgAfter(e.getTotalKgAfter())
        .totalPcAfter(e.getTotalPcAfter())
        .note(e.getNote())
        .createdAt(
            e.getCreatedAt() == null ? null : e.getCreatedAt().atZone(IST).toOffsetDateTime());
  }
}
