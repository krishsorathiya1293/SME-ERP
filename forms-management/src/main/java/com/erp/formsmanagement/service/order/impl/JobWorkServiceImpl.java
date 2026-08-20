package com.erp.formsmanagement.service.order.impl;

import com.erp.api.ordermanagement.model.JobWork;
import com.erp.api.ordermanagement.model.NewJobWork;
import com.erp.api.ordermanagement.model.PaginatedResultJobWork;
import com.erp.api.ordermanagement.model.UpdateJobWorkStatus;
import com.erp.api.ordermanagement.model.UpdateJobWorkType;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.clientportal.service.ClientOrderFulfillmentService;
import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.order.ElementType;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkOrderItemEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkReturnState;
import com.erp.formsmanagement.domain.entity.order.JobWorkStatus;
import com.erp.formsmanagement.domain.entity.order.JobWorkType;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.domain.repository.order.JobWorkRepository;
import com.erp.formsmanagement.domain.repository.order.OrderItemRepository;
import com.erp.formsmanagement.mapper.order.JobWorkMapper;
import com.erp.formsmanagement.service.master.TranslationService;
import com.erp.formsmanagement.service.order.JobWorkService;
import com.erp.formsmanagement.service.order.JobWorkStats;
import com.erp.formsmanagement.util.JobWorkNumber;
import com.erp.service.AbstractSpecificationServiceV2;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class JobWorkServiceImpl
    extends AbstractSpecificationServiceV2<JobWorkEntity, NewJobWork, JobWork, Long>
    implements JobWorkService {

  private final JobWorkRepository jobWorkRepository;
  private final OrderItemRepository orderItemRepository;
  private final PartyRepository partyRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;
  private final TranslationService translationService;
  private final ClientOrderFulfillmentService clientOrderFulfillmentService;

  public JobWorkServiceImpl(
      JobWorkRepository jobWorkRepository,
      OrderItemRepository orderItemRepository,
      PartyRepository partyRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository,
      TranslationService translationService,
      ClientOrderFulfillmentService clientOrderFulfillmentService,
      JobWorkMapper jobWorkMapper) {
    super(jobWorkRepository, jobWorkMapper);
    this.jobWorkRepository = jobWorkRepository;
    this.orderItemRepository = orderItemRepository;
    this.partyRepository = partyRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
    this.translationService = translationService;
    this.clientOrderFulfillmentService = clientOrderFulfillmentService;
  }

  /**
   * Registers the job work's party (by id) and finish in the translation dictionary so the editor
   * lists them and the print can render saved Hindi/Gujarati. Rows start blank — values are entered
   * by the user, never fetched. Best effort — a hiccup here must never block saving the job work.
   */
  private void ensureTranslations(JobWorkEntity entity) {
    try {
      if (entity.getParty() != null) {
        translationService.ensureParty(entity.getParty());
      }
      translationService.ensureFinish(entity.getFinish());
    } catch (Exception e) {
      log.warn("Failed to seed translations for job work {}", entity.getId(), e);
    }
  }

  @Override
  protected void afterCreate(JobWorkEntity entity, Long orderItemId, NewJobWork request) {
    linkRelations(entity, orderItemId, request);
    assignJobWorkNo(entity);
    ensureTranslations(entity);
    if (entity.getStatus() == null) {
      entity.setStatus(JobWorkStatus.PENDING);
    }
    applyMergedOrderItems(entity, request);
    // The order items have just gone out for plating — move their client requests to "In Plating".
    syncCoveredOrderItems(entity);
  }

  /**
   * Pulling a job work back off an order item puts that item back where it was before it was sent
   * out, so the client's request drops back to "Approved".
   */
  @Override
  @Transactional
  public void deleteById(Long orderItemId, Long id) {
    JobWorkEntity jobWork = jobWorkRepository.findById(id).orElse(null);

    // Every line the chitthi covered goes back where it was, not just the primary one.
    Map<Long, OrderItemEntity> covered = new LinkedHashMap<>();
    if (jobWork != null) {
      if (jobWork.getOrderItem() != null) {
        covered.put(jobWork.getOrderItem().getId(), jobWork.getOrderItem());
      }
      if (jobWork.getMergedOrderItems() != null) {
        jobWork.getMergedOrderItems().stream()
            .map(JobWorkOrderItemEntity::getOrderItem)
            .filter(Objects::nonNull)
            .forEach(item -> covered.putIfAbsent(item.getId(), item));
      }
    }
    // The inverse sides are already loaded in this persistence context and would still hold the
    // row we are about to remove; drop it so the stage split is derived from the real state.
    covered
        .values()
        .forEach(
            item -> {
              item.getJobWorks().removeIf(jw -> id.equals(jw.getId()));
              item.getJobWorkAllocations()
                  .removeIf(a -> a.getJobWork() != null && id.equals(a.getJobWork().getId()));
            });

    jobWorkRepository.deleteById(id);
    jobWorkRepository.flush();
    covered.values().forEach(clientOrderFulfillmentService::syncByOrderItem);
  }

  @Override
  protected void afterUpdate(JobWorkEntity entity, Long orderItemId, NewJobWork request) {
    linkRelations(entity, orderItemId, request);
    // jobWorkNo is never changed on update — keep the originally assigned number
    applyMergedOrderItems(entity, request);
    syncCoveredOrderItems(entity);
  }

  /**
   * Records which order lines this one chitthi covers, and how its weight divides between them.
   *
   * <p>A merge exists because the shop physically sends one batch: 200 Kg for one order and 100 Kg
   * for another, same party, item, size and finish, go to the plater together and come back
   * together. The chitthi is therefore one job work of 300 Kg — but the client portal still reports
   * per order line, so the 300 has to be attributable. It is split in proportion to what each line
   * ordered, which is the only split the shop floor can actually justify: nobody weighs the two
   * orders separately once they are in the same drum.
   *
   * <p>Passing one id (or none) leaves the job work exactly as it was before merging existed — no
   * allocation rows, {@code orderItem} alone describes it.
   */
  private void applyMergedOrderItems(JobWorkEntity entity, NewJobWork request) {
    List<Long> requested =
        request.getMergedOrderItemIds() == null ? List.of() : request.getMergedOrderItemIds();

    // Always include the primary line, and keep the caller's order without duplicates.
    Map<Long, OrderItemEntity> covered = new LinkedHashMap<>();
    if (entity.getOrderItem() != null) {
      covered.put(entity.getOrderItem().getId(), entity.getOrderItem());
    }
    for (Long id : requested) {
      if (id == null || covered.containsKey(id)) continue;
      covered.put(
          id,
          orderItemRepository
              .findById(id)
              .orElseThrow(
                  () ->
                      new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id))));
    }

    entity.getMergedOrderItems().clear();
    if (covered.size() < 2) {
      // Not a merge. Leaving the table empty keeps ordinary job works on exactly the old path.
      return;
    }

    List<OrderItemEntity> items = List.copyOf(covered.values());
    double[] weights = items.stream().mapToDouble(this::orderedKg).toArray();
    double totalWeight = 0d;
    for (double w : weights) totalWeight += w;

    double totalKg = entity.getQtyKg() != null ? entity.getQtyKg() : 0d;
    double totalPc = entity.getQtyPc() != null ? entity.getQtyPc() : 0d;

    List<JobWorkOrderItemEntity> allocations = new ArrayList<>();
    double assignedKg = 0d;
    double assignedPc = 0d;
    for (int i = 0; i < items.size(); i++) {
      // An order line with no usable quantity still gets a row (it is genuinely on this chitthi),
      // it just gets an even share when nothing better is known.
      double share =
          totalWeight > 0 ? weights[i] / totalWeight : 1d / items.size();

      JobWorkOrderItemEntity allocation = new JobWorkOrderItemEntity();
      allocation.setJobWork(entity);
      allocation.setOrderItem(items.get(i));
      boolean last = i == items.size() - 1;
      // The last line absorbs the rounding, so the parts always add back up to the whole.
      allocation.setQtyKg(last ? round3(totalKg - assignedKg) : round3(totalKg * share));
      allocation.setQtyPc(last ? round3(totalPc - assignedPc) : round3(totalPc * share));
      assignedKg += allocation.getQtyKg();
      assignedPc += allocation.getQtyPc();
      allocations.add(allocation);
    }
    entity.getMergedOrderItems().addAll(allocations);
  }

  /** The Kg an order line stands for, falling back to pieces through the size's 1-pc weight. */
  private double orderedKg(OrderItemEntity item) {
    if (item.getQtyKg() != null && item.getQtyKg() > 0) return item.getQtyKg();
    Double pcsWeight = item.getItemSize() != null ? item.getItemSize().getPcsWeight() : null;
    if (item.getQtyPc() != null && pcsWeight != null && pcsWeight > 0) {
      return item.getQtyPc() * pcsWeight;
    }
    return 0d;
  }

  /** Every order line this chitthi touches — the primary, plus the rest of a merge. */
  private void syncCoveredOrderItems(JobWorkEntity entity) {
    Map<Long, OrderItemEntity> covered = new LinkedHashMap<>();
    if (entity.getOrderItem() != null) {
      covered.put(entity.getOrderItem().getId(), entity.getOrderItem());
    }
    if (entity.getMergedOrderItems() != null) {
      entity.getMergedOrderItems().stream()
          .map(JobWorkOrderItemEntity::getOrderItem)
          .filter(Objects::nonNull)
          .forEach(item -> covered.putIfAbsent(item.getId(), item));
    }
    covered.values().forEach(clientOrderFulfillmentService::syncByOrderItem);
  }

  /**
   * Generates the party-wise job work number scoped to the current calendar month. Reads the
   * current MAX for this party this month and increments by 1, so each party has its own sequence
   * (AZ-1, AZ-2, ZP-1, …), the sequence resets when a new month begins, and deleting the latest
   * frees its number for reuse. No-op when the party isn't set yet.
   */
  private void assignJobWorkNo(JobWorkEntity entity) {
    if (entity.getParty() == null || entity.getParty().getId() == null) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    YearMonth yearMonth = YearMonth.from(now);

    LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
    LocalDateTime endDate = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

    Integer max =
        jobWorkRepository.findMaxJobWorkNoForPartyAndMonth(
            entity.getParty().getId(), startDate, endDate);

    entity.setJobWorkNo((max == null ? 0 : max) + 1);
    entity.setJobWorkLabel(
        JobWorkNumber.label(entity.getParty().getName(), entity.getJobWorkNo()));
  }

  private void linkRelations(JobWorkEntity entity, Long orderItemId, NewJobWork request) {
    OrderItemEntity orderItem =
        orderItemRepository
            .findById(orderItemId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, orderItemId)));
    entity.setOrderItem(orderItem);

    PartyEntity party =
        partyRepository
            .findById(request.getPartyId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getPartyId())));
    entity.setParty(party);

    ItemBlueprintDataEntity size =
        itemBlueprintDataRepository
            .findById(request.getSizeId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getSizeId())));
    entity.setSize(size);

    computeCalculatedFields(entity, request);
  }

  /**
   * Follows the client's Out-Side/In-Side Job-Work excel. The piece count comes from the order
   * ({@code qtyPc}); the shop enters the Peti count + tare weight per Peti and the rate. Everything
   * else is derived from those plus the item's own master rates (pcsWeight / pcsPerBox /
   * boxPerCarton):
   *
   * <ul>
   *   <li>Net Kg = Gross Kg − Peti count &times; 1-Peti tare
   *   <li>Total Pcs = Net Kg / 1-pc weight (1-pc weight fetched from item, editable per job work)
   *   <li>Sticker Qty = Total Pcs / pcsPerBox (one sticker per box)
   *   <li>Total Carton = Sticker Qty / boxPerCarton
   *   <li>Total Rate = Net Kg &times; Rate/Kg
   * </ul>
   *
   * @param requestPcsWeight the (possibly user-edited) 1-pc weight from the payload; falls back to
   *     the item's own {@code pcsWeight} when not supplied.
   */
  private void computeCalculatedFields(JobWorkEntity entity, NewJobWork request) {
    double grossKg = entity.getGrossKg() != null ? entity.getGrossKg() : 0.0;
    double elementCount = entity.getElementCount() != null ? entity.getElementCount() : 0.0;
    double petiWeightKg = entity.getPetiWeightKg() != null ? entity.getPetiWeightKg() : 0.0;

    // Net Kg = gross weighed − empty Peti/Drum tare
    double netKg = Math.max(0.0, round3(grossKg - (elementCount * petiWeightKg)));
    entity.setQtyKg(netKg);

    ItemBlueprintDataEntity size = entity.getSize();
    Double pcsWeight =
        request.getPcsWeight() != null ? request.getPcsWeight() : (size != null ? size.getPcsWeight() : null);
    Double totalPcs = (pcsWeight != null && pcsWeight > 0) ? netKg / pcsWeight : null;
    entity.setQtyPc(totalPcs);

    // Packing rates precedence: order item (already client-resolved at order creation) >
    // request (manual mode, client-resolved on the client) > size's item-master inventory.
    OrderItemEntity orderItem = entity.getOrderItem();
    InventoryEntity inventory = size != null ? size.getInventory() : null;
    Double pcsPerBox =
        firstPositive(
            orderItem != null ? orderItem.getPcPerBox() : null,
            request.getPcsPerBox(),
            inventory != null && inventory.getPcsPerBox() != null
                ? inventory.getPcsPerBox().doubleValue()
                : null);
    Double boxPerCarton =
        firstPositive(
            orderItem != null ? orderItem.getBoxPerCartoon() : null,
            request.getBoxPerCarton(),
            inventory != null && inventory.getBoxPerCarton() != null
                ? inventory.getBoxPerCarton().doubleValue()
                : null);

    Double stickerQty =
        (totalPcs != null && pcsPerBox != null && pcsPerBox > 0) ? totalPcs / pcsPerBox : null;
    entity.setStickerQty(stickerQty);

    Double totalCarton =
        (stickerQty != null && boxPerCarton != null && boxPerCarton > 0)
            ? stickerQty / boxPerCarton
            : null;
    entity.setTotalCarton(totalCarton);

    Double ratePerKg = entity.getRatePerKg();
    entity.setTotalRate(ratePerKg != null ? round3(netKg * ratePerKg) : null);
  }

  private static Double firstPositive(Double... values) {
    for (Double v : values) {
      if (v != null && v > 0) return v;
    }
    return null;
  }

  private static double round3(double value) {
    return Math.round(value * 1000.0) / 1000.0;
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultJobWork getAll(Long orderItemId, GetAllQuery<Void> query) {
    Specification<JobWorkEntity> spec =
        Specification.where(jobWorkRepository.filterByOrderItemId(orderItemId))
            .and(jobWorkRepository.filterBySearch(query.search()));

    Page<JobWorkEntity> results =
        jobWorkRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    return PageMapper.toResult(
        results,
        mapper()::toDomain,
        PaginatedResultJobWork::new,
        PaginatedResultJobWork::setData);
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultJobWork getAllGlobal(
      JobWorkType type, JobWorkReturnState returnState, GetAllQuery<Void> query) {
    Specification<JobWorkEntity> spec =
        Specification.where(jobWorkRepository.filterBySearch(query.search()))
            .and(jobWorkRepository.filterByType(type))
            .and(jobWorkRepository.filterByReturnState(returnState));

    Page<JobWorkEntity> results = jobWorkRepository.findAll(spec, globalPageRequest(query));

    return PageMapper.toResult(
        results,
        mapper()::toDomain,
        PaginatedResultJobWork::new,
        PaginatedResultJobWork::setData);
  }

  /**
   * The chitthi list reads by item.
   *
   * <p>A supervisor working the list is holding one item at a time — every chitthi for it, whatever
   * day it was raised — so the item is what the page is ordered by, and newest-first only breaks
   * ties within an item. Case is ignored so \Grip\ and \grip\ do not sort into two runs of the
   * same item. An explicit {@code sortBy} from the caller still wins.
   */
  private Pageable globalPageRequest(GetAllQuery<Void> query) {
    if (query.sortBy().isPresent()) {
      return PaginationUtils.getPageRequest(
          query.page(), query.size(), query.direction(), query.sortBy());
    }
    return PageRequest.of(
        query.page().orElse(0),
        query.size().orElse(10),
        Sort.by(Sort.Order.asc("size.item.itemName").ignoreCase(), Sort.Order.desc("createdAt")));
  }

  @Override
  @Transactional(readOnly = true)
  public JobWorkStats getGlobalStats(JobWorkType type, String search) {
    Specification<JobWorkEntity> base =
        Specification.where(jobWorkRepository.filterBySearch(Optional.ofNullable(search)))
            .and(jobWorkRepository.filterByType(type));

    long total = jobWorkRepository.count(base);
    long pending =
        jobWorkRepository.count(
            base.and(jobWorkRepository.filterByReturnState(JobWorkReturnState.PENDING)));
    long partiallyReturned =
        jobWorkRepository.count(
            base.and(
                jobWorkRepository.filterByReturnState(JobWorkReturnState.PARTIALLY_RETURNED)));
    long fullyReturned =
        jobWorkRepository.count(
            base.and(jobWorkRepository.filterByReturnState(JobWorkReturnState.FULLY_RETURNED)));

    return new JobWorkStats(total, pending, partiallyReturned, fullyReturned);
  }

  @Override
  @Transactional
  public JobWork updateStatus(Long orderItemId, Long id, UpdateJobWorkStatus request) {
    JobWorkEntity entity =
        jobWorkRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
    entity.setStatus(JobWorkStatus.valueOf(request.getStatus().name()));
    clientOrderFulfillmentService.syncByOrderItem(entity.getOrderItem());
    return mapper().toDomain(entity);
  }

  @Override
  @Transactional
  public JobWork updateType(Long orderItemId, Long id, UpdateJobWorkType request) {
    JobWorkEntity entity =
        jobWorkRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
    entity.setJobWorkType(JobWorkType.valueOf(request.getJobWorkType().name()));
    return mapper().toDomain(entity);
  }

  /**
   * Manual job work: not tied to any order item. The user picks the party + item/size from the
   * masters and enters the weighing/rate. All derived fields are computed the same way as an
   * order-based job work.
   */
  @Override
  @Transactional
  public JobWork createManual(NewJobWork request) {
    JobWorkEntity entity = new JobWorkEntity();
    applyManualFields(entity, request);
    entity.setJobWorkType(
        request.getJobWorkType() != null
            ? JobWorkType.valueOf(request.getJobWorkType().name())
            : JobWorkType.MANUAL);
    assignJobWorkNo(entity);
    JobWorkEntity saved = jobWorkRepository.save(entity);
    ensureTranslations(saved);
    return mapper().toDomain(saved);
  }

  @Override
  @Transactional
  public JobWork updateManual(Long id, NewJobWork request) {
    JobWorkEntity entity =
        jobWorkRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
    applyManualFields(entity, request);
    return mapper().toDomain(jobWorkRepository.save(entity));
  }

  /** Populates the fields a Manual job work carries — no order item is ever linked. */
  private void applyManualFields(JobWorkEntity entity, NewJobWork request) {
    PartyEntity party =
        partyRepository
            .findById(request.getPartyId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getPartyId())));
    entity.setParty(party);

    ItemBlueprintDataEntity size =
        itemBlueprintDataRepository
            .findById(request.getSizeId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, request.getSizeId())));
    entity.setSize(size);

    entity.setJobDate(request.getJobDate());
    entity.setFinish(request.getFinish());
    entity.setElementCount(request.getElementCount());
    entity.setElementType(
        request.getElementType() != null
            ? ElementType.valueOf(request.getElementType().name())
            : null);
    entity.setPetiWeightKg(request.getPetiWeightKg());
    entity.setGrossKg(request.getGrossKg());
    entity.setRatePerKg(request.getRatePerKg());
    if (request.getStatus() != null) {
      entity.setStatus(JobWorkStatus.valueOf(request.getStatus().name()));
    } else if (entity.getStatus() == null) {
      entity.setStatus(JobWorkStatus.PENDING);
    }
    entity.setChitthiNo(request.getChitthiNo());
    entity.setChitthiDate(request.getChitthiDate());
    entity.setOrderTime(request.getOrderTime());

    computeCalculatedFields(entity, request);
  }
}
