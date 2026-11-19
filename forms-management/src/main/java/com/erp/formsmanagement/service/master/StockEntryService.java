package com.erp.formsmanagement.service.master;

import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewStockEntry;
import com.erp.api.mastermanagement.model.NewStockEntryType;
import com.erp.api.mastermanagement.model.StockEntry;
import com.erp.api.mastermanagement.model.StockEntryType;
import com.erp.api.mastermanagement.model.StockEntryUnit;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.ItemEntity;
import com.erp.formsmanagement.domain.entity.master.StockEntryEntity;
import com.erp.formsmanagement.domain.entity.master.StockStatus;
import com.erp.formsmanagement.domain.entity.order.ElementType;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.ItemRepository;
import com.erp.formsmanagement.domain.repository.master.StockEntryRepository;
import com.erp.formsmanagement.domain.repository.order.JobWorkRepository;
import com.erp.formsmanagement.mapper.master.ItemMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Stock of a size, kept by peti. The kg typed in comes off the client's weighing machine with the
 * petis on it, so petis x empty peti weight is taken off to get the net kg; pcs = net kg / weight
 * per pc, rounded down, as the stock dialog always worked it out.
 *
 * <p>By hand: ADD puts an entry on top of the stock ("10 peti there, 4 more came"), MINUS takes
 * one off it, SET replaces it (a recount). Every entry is kept with its date and the stock right
 * after it. An entry can also be removed outright — that leaves nothing behind, the stock simply
 * goes back to what it would be without it.
 *
 * <p>An entry is given in one of three units. PETI is the weighing-machine case above. KG is loose
 * weight with no petis. PCS is a count of pieces, turned into kg as pcs x weight per pc ("10 pcs at
 * 0.100 kg = 1 kg"). The stock itself is always kg, and the pieces shown are always kg / weight
 * per pc — so the kg of a PCS entry is kept exact rather than rounded to the gram, or the pieces
 * would not count back to what was typed.
 *
 * <p>By job work: a chitthi sent to the plater takes its net kg out of the same stock (JW_OUT) and
 * is refused when the size has less than that. Editing the chitthi moves the difference, deleting
 * it puts the kg back (JW_BACK). A size with no stock row at all is not tracked: nothing is checked
 * and nothing is taken.
 */
@Service
@Transactional
public class StockEntryService {

  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  /** Half a gram. Weights are kept to 3 decimals, so anything under this is rounding, not stock. */
  private static final double EPS = 0.0005;

  private static final String ADD = "ADD";
  private static final String MINUS = "MINUS";
  private static final String SET = "SET";
  private static final String JW_OUT = "JW_OUT";
  private static final String JW_BACK = "JW_BACK";

  private final ItemRepository items;
  private final StockEntryRepository entries;
  private final ItemBlueprintDataRepository sizes;
  private final ItemMapper itemMapper;
  private final JobWorkRepository jobWorks;

  public StockEntryService(
      ItemRepository items,
      StockEntryRepository entries,
      ItemBlueprintDataRepository sizes,
      ItemMapper itemMapper,
      JobWorkRepository jobWorks) {
    this.jobWorks = jobWorks;
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

  // ── By hand: add, minus, set ──────────────────────────────────────────────

  public Item create(NewStockEntry in) {
    ItemBlueprintDataEntity size =
        sizes
            .findById(in.getSizeId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Size not found"));
    ItemEntity item = locked(size.getId()).orElseGet(() -> newItem(size));

    boolean add = in.getType() == NewStockEntryType.ADD;
    boolean minus = in.getType() == NewStockEntryType.MINUS;
    StockEntryUnit unit = in.getUnit() == null ? StockEntryUnit.PETI : in.getUnit();
    boolean byPeti = unit == StockEntryUnit.PETI;
    boolean byPcs = unit == StockEntryUnit.PCS;

    // Only what the chosen unit uses is read; the rest of the request is ignored, not trusted.
    int peti = byPeti && in.getPetiCount() != null ? in.getPetiCount() : 0;
    double gross = byPcs || in.getGrossKg() == null ? 0 : in.getGrossKg();
    double count = byPcs && in.getPcs() != null ? in.getPcs() : 0;
    double emptyWt =
        in.getEmptyPetiWeight() != null
            ? in.getEmptyPetiWeight()
            : item.getEmptyPetiWeight() == null ? 0 : item.getEmptyPetiWeight();
    double wpc = in.getWeightPerPc() == null ? 0 : in.getWeightPerPc();
    if (peti < 0 || gross < 0 || emptyWt < 0 || wpc < 0 || count < 0) {
      throw bad("Values can't be negative");
    }

    // The kg this entry stands for.
    double net;
    if (byPcs) {
      if (count != Math.rint(count)) throw bad("Pcs must be a whole number");
      if (count > 0 && wpc <= 0) throw bad("Enter the weight per pc");
      // Left exact, not rounded to the gram: a piece weight is often a dozen weight / 12, and
      // 10 x 0.120833... rounded to 1.208 kg would count back as 9 pieces, not 10.
      net = count * wpc;
    } else if (byPeti) {
      net = round3(gross - peti * emptyWt);
      if (net < 0) {
        throw bad(
            "Empty peti weight ("
                + round3(peti * emptyWt)
                + " kg) is more than the "
                + round3(gross)
                + " kg typed in");
      }
    } else {
      net = round3(gross);
    }
    boolean nothing = byPcs ? count == 0 : byPeti ? peti == 0 && gross == 0 : gross == 0;
    String what = byPcs ? "pcs" : byPeti ? "petis and kg" : "kg";
    if (add && nothing) throw bad("Enter the " + what + " to add");
    if (minus && nothing) throw bad("Enter the " + what + " to take off");
    if (net > 0 && wpc <= 0) throw bad("Enter the weight per pc");

    int oldPeti = item.getPetiCount() == null ? 0 : item.getPetiCount();
    double oldKg = item.getItemKg() == null ? 0 : item.getItemKg();
    if (minus && item.getId() == null) throw bad("There is no stock for this size yet");
    if (minus && net > oldKg + EPS) {
      throw bad(
          "Only "
              + kg(oldKg)
              + " kg ("
              + whole(pcs(oldKg, wpc))
              + " pcs) in stock, cannot take off "
              + kg(net)
              + " kg"
              + (byPcs ? " (" + whole(count) + " pcs)" : ""));
    }

    // Petis are a count of crates on the shelf, not the stock itself, so a minus that names more
    // crates than are listed just empties the count rather than failing on it. Loose kg and pcs
    // do not come in crates, so they leave the count alone.
    int totalPeti =
        !byPeti ? oldPeti : add ? oldPeti + peti : minus ? Math.max(0, oldPeti - peti) : peti;
    double totalKg = tidy(add ? oldKg + net : minus ? oldKg - net : net);

    // A SET that names the stock already there only changed settings (low stock warning, weight
    // per pc). The kg on file is kept as it is: the form shows it to the gram, and writing that
    // rounded figure back would shave a piece off a stock that was entered in pieces.
    boolean sameStock =
        !add && !minus && totalPeti == oldPeti && Math.abs(totalKg - oldKg) <= EPS;
    if (sameStock) totalKg = oldKg;
    double totalPc = pcs(totalKg, wpc);
    boolean stockChanged =
        add || minus || !sameStock || !Objects.equals(item.getTotalPc(), totalPc);

    item.setPetiCount(totalPeti);
    item.setEmptyPetiWeight(emptyWt);
    item.setWeightPerPc(wpc);
    item.setItemKg(totalKg);
    item.setTotalPc(totalPc);
    if (in.getLowStockWarning() != null) item.setLowStockWarning(in.getLowStockWarning());
    if (item.getStockStatus() == null) item.setStockStatus(StockStatus.IN_STOCK);
    item = items.save(item);

    if (stockChanged) {
      // What a SET stands for is the stock it states; an ADD or MINUS, the amount it moves.
      double entryKg = add || minus ? net : totalKg;
      StockEntryEntity e = new StockEntryEntity();
      e.setItem(item);
      e.setEntryType(in.getType().name());
      e.setEntryUnit(unit.name());
      e.setEntryDate(in.getEntryDate() == null ? LocalDate.now(IST) : in.getEntryDate());
      e.setPetiCount(peti);
      e.setGrossKg(byPcs ? null : round3(gross));
      e.setEmptyPetiWeight(byPeti ? emptyWt : null);
      e.setNetKg(entryKg);
      e.setWeightPerPc(wpc);
      e.setPcs(byPcs ? count : pcs(entryKg, wpc));
      e.setTotalPetiAfter(totalPeti);
      e.setTotalKgAfter(totalKg);
      e.setTotalPcAfter(totalPc);
      e.setNote(trim(in.getNote()));
      entries.save(e);
    }
    return itemMapper.toDomain(item);
  }

  // ── Remove an entry outright ──────────────────────────────────────────────

  /**
   * Deletes one hand-made entry as if it had never been typed. Nothing records the removal.
   *
   * <p>The stock moves back by what the entry had changed, and so do the "stock after" figures of
   * the entries made on top of it. A later recount (SET) stops that: a recount states the stock
   * outright, so nothing from before it reaches today's figure — the entry just goes.
   */
  public Item remove(Long entryId) {
    StockEntryEntity entry =
        entries
            .findById(entryId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock entry not found"));
    String type = entry.getEntryType();
    if (JW_OUT.equals(type) || JW_BACK.equals(type)) {
      throw bad("This entry belongs to a job work. Edit or delete the job work instead.");
    }

    Long sizeId = entry.getItem().getSize() == null ? null : entry.getItem().getSize().getId();
    ItemEntity item = (sizeId == null ? Optional.<ItemEntity>empty() : locked(sizeId))
        .filter(i -> i.getId().equals(entry.getItem().getId()))
        .orElse(entry.getItem());

    List<StockEntryEntity> all = entries.findByItem_IdOrderByIdAsc(item.getId());
    int at = -1;
    for (int i = 0; i < all.size(); i++) {
      if (all.get(i).getId().equals(entryId)) {
        at = i;
        break;
      }
    }

    // What this entry changed the stock by.
    double dKg;
    int dPeti;
    int entryPeti = entry.getPetiCount() == null ? 0 : entry.getPetiCount();
    if (ADD.equals(type)) {
      dKg = entry.getNetKg();
      dPeti = entryPeti;
    } else if (MINUS.equals(type)) {
      dKg = -entry.getNetKg();
      dPeti = -entryPeti;
    } else {
      // A recount replaced whatever stood before it, which is the entry just ahead of it.
      StockEntryEntity before = at > 0 ? all.get(at - 1) : null;
      double baseKg = before == null ? 0 : before.getTotalKgAfter();
      int basePeti = before == null || before.getTotalPetiAfter() == null ? 0 : before.getTotalPetiAfter();
      int afterPeti = entry.getTotalPetiAfter() == null ? 0 : entry.getTotalPetiAfter();
      dKg = entry.getTotalKgAfter() - baseKg;
      dPeti = afterPeti - basePeti;
    }

    // Entries stacked on top of it, up to the next recount.
    List<StockEntryEntity> onTop = new ArrayList<>();
    boolean recountedSince = false;
    for (int i = at + 1; at >= 0 && i < all.size(); i++) {
      if (SET.equals(all.get(i).getEntryType())) {
        recountedSince = true;
        break;
      }
      onTop.add(all.get(i));
    }

    if (!recountedSince) {
      double have = item.getItemKg() == null ? 0 : item.getItemKg();
      if (have - dKg < -EPS) {
        throw bad(
            "Cannot remove this entry. It put "
                + kg(dKg)
                + " kg into stock and only "
                + kg(have)
                + " kg is left. Use Minus instead.");
      }
      move(item, -dKg, -dPeti);
    }
    for (StockEntryEntity later : onTop) {
      double laterKg = tidy(later.getTotalKgAfter() - dKg);
      int laterPeti = later.getTotalPetiAfter() == null ? 0 : later.getTotalPetiAfter();
      later.setTotalKgAfter(laterKg);
      later.setTotalPetiAfter(Math.max(0, laterPeti - dPeti));
      later.setTotalPcAfter(pcs(laterKg, weightPerPc(item, later.getWeightPerPc())));
    }
    entries.saveAll(onTop);
    entries.delete(entry);
    return itemMapper.toDomain(item);
  }

  // ── By job work ───────────────────────────────────────────────────────────

  /**
   * A chitthi being raised: its net kg leaves the size's stock, or the save is refused because the
   * size does not have that much. Call it once the net kg and the chitthi number are worked out.
   */
  public void jobWorkSent(JobWorkEntity jw) {
    jw.setStockKg(null);
    jw.setStockPeti(null);
    if (jw.getSize() == null) return;
    ItemEntity item = locked(jw.getSize().getId()).orElse(null);
    if (item == null) return; // no stock row for this size: not tracked

    double send = sentKg(jw);
    double have = item.getItemKg() == null ? 0 : item.getItemKg();
    if (send > have + EPS) {
      throw bad(
          "Not enough stock. Only "
              + kg(have)
              + " kg ("
              + whole(item.getTotalPc())
              + " pcs) in stock, cannot send "
              + kg(send)
              + " kg to job work.");
    }
    int petis = Math.min(sentPetis(jw), item.getPetiCount() == null ? 0 : item.getPetiCount());
    if (send > 0 || petis > 0) {
      move(item, -send, -petis);
      record(
          item,
          JW_OUT,
          send,
          petis,
          jw.getPetiWeightKg(),
          "Sent to job work " + label(jw) + party(jw),
          jw.getJobDate());
    }
    jw.setStockKg(send);
    jw.setStockPeti(petis);
  }

  /**
   * A chitthi that was edited: the stock moves by the difference between what it held and what it
   * holds now. {@code previousSizeId} is the size it was on before the edit.
   */
  public void jobWorkEdited(JobWorkEntity jw, Long previousSizeId) {
    if (jw.getStockKg() == null) return; // it never drew from stock, so an edit does not start to

    Long sizeId = jw.getSize() == null ? null : jw.getSize().getId();
    if (!Objects.equals(previousSizeId, sizeId)) {
      putBack(jw, previousSizeId, "Job work " + label(jw) + " moved to another size");
      jobWorkSent(jw);
      return;
    }

    ItemEntity item = sizeId == null ? null : locked(sizeId).orElse(null);
    if (item == null) {
      // The size's stock row has gone since; there is nothing left to adjust against.
      jw.setStockKg(null);
      jw.setStockPeti(null);
      return;
    }

    double held = jw.getStockKg();
    int heldPetis = jw.getStockPeti() == null ? 0 : jw.getStockPeti();
    double now = sentKg(jw);
    double more = round3(now - held);
    int morePetis = sentPetis(jw) - heldPetis;
    if (Math.abs(more) < EPS && morePetis == 0) return;

    double have = item.getItemKg() == null ? 0 : item.getItemKg();
    if (more > have + EPS) {
      throw bad(
          "Not enough stock. Only "
              + kg(have)
              + " kg more in stock, cannot raise this job work from "
              + kg(held)
              + " kg to "
              + kg(now)
              + " kg.");
    }
    // Never take more crates than the shelf lists.
    morePetis = Math.min(morePetis, item.getPetiCount() == null ? 0 : item.getPetiCount());

    move(item, -more, -morePetis);
    boolean out = more > 0 || (Math.abs(more) < EPS && morePetis > 0);
    record(
        item,
        out ? JW_OUT : JW_BACK,
        Math.abs(more),
        Math.abs(morePetis),
        jw.getPetiWeightKg(),
        "Job work " + label(jw) + " edited",
        null);
    jw.setStockKg(now);
    jw.setStockPeti(heldPetis + morePetis);
  }

  /** A chitthi about to be deleted: whatever it took out of stock goes back in. */
  public void jobWorkDeleted(JobWorkEntity jw) {
    Long sizeId = jw.getSize() == null ? null : jw.getSize().getId();
    putBack(jw, sizeId, "Job work " + label(jw) + " deleted");
  }

  /**
   * An order line about to be deleted. The database takes the line's chitthis down with it (FK
   * cascade), so the stock they hold is put back first.
   *
   * <p>Works from plain values, not {@code JobWorkEntity}: a loaded chitthi would still point at
   * the line once it is removed, and Hibernate refuses to flush an entity that references a
   * removed one.
   */
  public void orderLineDeleted(Long orderItemId) {
    if (orderItemId == null) return;
    List<JobWorkRepository.StockHold> holds = jobWorks.findStockHeldByOrderItemId(orderItemId);
    if (holds.isEmpty()) return;
    for (JobWorkRepository.StockHold hold : holds) {
      String label =
          hold.getJobWorkLabel() != null && !hold.getJobWorkLabel().isBlank()
              ? hold.getJobWorkLabel()
              : hold.getChitthiNo() != null && !hold.getChitthiNo().isBlank()
                  ? hold.getChitthiNo()
                  : "JW-" + hold.getId();
      giveBack(
          hold.getSizeId(),
          hold.getStockKg() == null ? 0 : hold.getStockKg(),
          hold.getStockPeti() == null ? 0 : hold.getStockPeti(),
          hold.getPetiWeightKg(),
          "Job work " + label + " deleted with its order line");
    }
    // In the same transaction as the put-back, so a chitthi that somehow outlives the line can
    // never hand the same kg back a second time.
    jobWorks.clearStockHeldByOrderItemId(orderItemId);
  }

  private void putBack(JobWorkEntity jw, Long sizeId, String note) {
    double held = jw.getStockKg() == null ? 0 : jw.getStockKg();
    int heldPetis = jw.getStockPeti() == null ? 0 : jw.getStockPeti();
    jw.setStockKg(null);
    jw.setStockPeti(null);
    giveBack(sizeId, held, heldPetis, jw.getPetiWeightKg(), note);
  }

  private void giveBack(Long sizeId, double kg, int petis, Double petiWeightKg, String note) {
    if (sizeId == null || (kg <= 0 && petis <= 0)) return;
    ItemEntity item = locked(sizeId).orElse(null);
    if (item == null) return;
    move(item, kg, petis);
    record(item, JW_BACK, kg, petis, petiWeightKg, note, null);
  }

  /** Changes the stock row by a signed kg and peti amount; pcs always follow from the kg. */
  private void move(ItemEntity item, double dKg, int dPeti) {
    double totalKg = tidy((item.getItemKg() == null ? 0 : item.getItemKg()) + dKg);
    int totalPeti = Math.max(0, (item.getPetiCount() == null ? 0 : item.getPetiCount()) + dPeti);
    item.setItemKg(totalKg);
    item.setPetiCount(totalPeti);
    double wpc = weightPerPc(item, null);
    if (wpc > 0) item.setTotalPc(pcs(totalKg, wpc));
    else if (totalKg <= 0) item.setTotalPc(0d);
    items.save(item);
  }

  /** Writes a job work's movement into the size's history; magnitudes are kept positive. */
  private void record(
      ItemEntity item,
      String type,
      double netKg,
      int petis,
      Double emptyPetiWeight,
      String note,
      LocalDate date) {
    double wpc = weightPerPc(item, null);
    StockEntryEntity e = new StockEntryEntity();
    e.setItem(item);
    e.setEntryType(type);
    e.setEntryUnit(StockEntryUnit.KG.name());
    e.setEntryDate(date == null ? LocalDate.now(IST) : date);
    e.setPetiCount(petis);
    e.setEmptyPetiWeight(emptyPetiWeight);
    e.setNetKg(round3(netKg));
    e.setWeightPerPc(wpc > 0 ? wpc : null);
    e.setPcs(pcs(netKg, wpc));
    e.setTotalPetiAfter(item.getPetiCount());
    e.setTotalKgAfter(item.getItemKg() == null ? 0 : item.getItemKg());
    e.setTotalPcAfter(item.getTotalPc() == null ? 0 : item.getTotalPc());
    e.setNote(note.length() > 255 ? note.substring(0, 255) : note);
    entries.save(e);
  }

  private Optional<ItemEntity> locked(Long sizeId) {
    return items.lockBySizeId(sizeId).stream().findFirst();
  }

  /** Net kg the chitthi sends out — what the job work itself calls its Qty Kg. */
  private static double sentKg(JobWorkEntity jw) {
    return jw.getQtyKg() == null ? 0 : Math.max(0, round3(jw.getQtyKg()));
  }

  /** Crates that leave with it. Drums are the plater's, not shelf petis, so they do not count. */
  private static int sentPetis(JobWorkEntity jw) {
    if (jw.getElementType() == ElementType.DRUM || jw.getElementCount() == null) return 0;
    return (int) Math.max(0, Math.round(jw.getElementCount()));
  }

  /** The stock's own weight per pc, else the one given, else the size's 1-pc weight. */
  private static double weightPerPc(ItemEntity item, Double fallback) {
    if (item.getWeightPerPc() != null && item.getWeightPerPc() > 0) return item.getWeightPerPc();
    if (fallback != null && fallback > 0) return fallback;
    Double ofSize = item.getSize() == null ? null : item.getSize().getPcsWeight();
    return ofSize != null && ofSize > 0 ? ofSize : 0;
  }

  private static String label(JobWorkEntity jw) {
    if (jw.getJobWorkLabel() != null && !jw.getJobWorkLabel().isBlank()) return jw.getJobWorkLabel();
    if (jw.getChitthiNo() != null && !jw.getChitthiNo().isBlank()) return jw.getChitthiNo();
    return jw.getId() == null ? "(new)" : "JW-" + jw.getId();
  }

  private static String party(JobWorkEntity jw) {
    String name = jw.getParty() == null ? null : jw.getParty().getName();
    return name == null || name.isBlank() ? "" : " (" + name.trim() + ")";
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
    return kg > 0 && wpc > 0 ? Math.floor(kg / wpc + 1e-7) : 0;
  }

  /**
   * A stock weight as it is kept: exact, but with float noise snapped back to the gram
   * (49.900000000000006 is 49.9) and never below zero. Weighed figures therefore stay at three
   * decimals, while a weight that came from a piece count keeps the fraction it needs.
   *
   * <p>A crumb under a gram can be left behind — weighed kg taken out of a stock that was entered
   * in pieces. It is kept on purpose: putting that same kg back (a job work deleted) must land on
   * the pieces there were before, and it would not if the crumb had been thrown away.
   */
  static double tidy(double v) {
    if (v < 1e-9) return 0;
    double gram = round3(v);
    return Math.abs(v - gram) < 1e-9 ? gram : v;
  }

  static double round3(double v) {
    return Math.round(v * 1000d) / 1000d;
  }

  /** A weight for a message: 3 decimals at most, no trailing zeros ("50", "12.5"). */
  private static String kg(double v) {
    BigDecimal d = BigDecimal.valueOf(round3(v)).stripTrailingZeros();
    return d.scale() < 0 ? d.setScale(0).toPlainString() : d.toPlainString();
  }

  private static String whole(Double v) {
    return String.valueOf(v == null ? 0 : Math.round(v));
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
        .type(StockEntryType.valueOf(e.getEntryType()))
        // Rows from before units existed were all weighed by the peti.
        .unit(
            e.getEntryUnit() == null
                ? StockEntryUnit.PETI
                : StockEntryUnit.valueOf(e.getEntryUnit()))
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
