package com.erp.formsmanagement.clientportal.service.impl;

import com.erp.api.clientportalmanagement.model.OrderItemStage;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestEntity;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestStatus;
import com.erp.formsmanagement.clientportal.domain.repository.ClientOrderRequestRepository;
import com.erp.formsmanagement.clientportal.service.ClientOrderFulfillmentService;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkOrderItemEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkReturnEntity;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.formsmanagement.domain.repository.order.OrderRepository;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientOrderFulfillmentServiceImpl implements ClientOrderFulfillmentService {

  /**
   * Statuses the works may overwrite. A request still awaiting a decision, or already rejected, is
   * the admin's to move — nothing downstream may drag it back into the pipeline.
   */
  private static final Set<ClientOrderRequestStatus> DERIVED_STATUSES =
      EnumSet.of(
          ClientOrderRequestStatus.APPROVED,
          ClientOrderRequestStatus.IN_PLATING,
          ClientOrderRequestStatus.READY_TO_DISPATCH,
          ClientOrderRequestStatus.DISPATCHED,
          ClientOrderRequestStatus.IN_PROGRESS,
          ClientOrderRequestStatus.COMPLETED);

  /** Weights are compared and reported to 3 decimals, matching the job-work sheets. */
  private static final double EPSILON = 0.0005;

  private final ClientOrderRequestRepository clientOrderRequestRepository;
  private final OrderRepository orderRepository;

  @Override
  @Transactional
  public void syncByOrderItem(OrderItemEntity orderItem) {
    if (orderItem == null || orderItem.getOrder() == null) {
      return;
    }
    // Reading only the id off the lazy proxy — no need to load the whole order here.
    syncByOrderId(orderItem.getOrder().getId());
  }

  @Override
  @Transactional
  public void syncByOrderId(Long orderId) {
    if (orderId == null) {
      return;
    }

    List<ClientOrderRequestEntity> requests = clientOrderRequestRepository.findAllByOrder_Id(orderId);
    if (requests.isEmpty()) {
      return;
    }

    OrderEntity order = orderRepository.findById(orderId).orElse(null);
    if (order == null) {
      return;
    }

    ClientOrderRequestStatus derived = toRequestStatus(rollUp(order));
    requests.stream()
        .filter(request -> DERIVED_STATUSES.contains(request.getStatus()))
        .forEach(request -> request.setStatus(derived));
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, ItemFulfillment> describeByLine(OrderEntity order) {
    Map<String, ItemFulfillment> byLine = new HashMap<>();
    if (order == null || order.getOrderItems() == null) {
      return byLine;
    }
    for (OrderItemEntity item : order.getOrderItems()) {
      if (item.getItemSize() == null) {
        continue;
      }
      Long sizeId = item.getItemSize().getId();
      ItemFulfillment fulfillment = describe(item);
      byLine.put(ClientOrderFulfillmentService.lineKey(sizeId, item.getPlating()), fulfillment);

      // Size-only fallback for a line whose plating no longer matches. Where a size appears more
      // than once, keep the least advanced — same "what is it still waiting on?" rule as the roll-up.
      byLine.merge(
          ClientOrderFulfillmentService.lineKey(sizeId, null),
          fulfillment,
          (existing, candidate) ->
              candidate.stage().ordinal() < existing.stage().ordinal() ? candidate : existing);
    }
    return byLine;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<Long, ItemFulfillment> describeByOrderItemId(OrderEntity order) {
    Map<Long, ItemFulfillment> byId = new HashMap<>();
    if (order == null || order.getOrderItems() == null) {
      return byId;
    }
    for (OrderItemEntity item : order.getOrderItems()) {
      byId.put(item.getId(), describe(item));
    }
    return byId;
  }

  /**
   * An order shows the furthest stage any of its lines has reached, which is the coarse badge the
   * admin screen filters on. The client portal doesn't use this — it splits each line's quantity
   * across the stages instead, so a part-plated order appears under several tabs at once.
   *
   * <p>DISPATCHED is the one exception: while any line is still to go out, the order is not
   * dispatched. An order with no lines has nothing outstanding, so it reads as approved.
   */
  private OrderItemStage rollUp(OrderEntity order) {
    List<OrderItemEntity> items = order.getOrderItems();
    if (items == null || items.isEmpty()) {
      return OrderItemStage.APPROVED;
    }

    List<OrderItemStage> stages = items.stream().map(item -> describe(item).stage()).toList();

    if (stages.stream().allMatch(stage -> stage == OrderItemStage.DISPATCHED)) {
      return OrderItemStage.DISPATCHED;
    }
    // A line already out of the door got at least as far as ready, so it counts towards
    // READY_TO_DISPATCH rather than dragging the whole order into DISPATCHED.
    if (stages.stream()
        .anyMatch(
            stage ->
                stage == OrderItemStage.READY_TO_DISPATCH || stage == OrderItemStage.DISPATCHED)) {
      return OrderItemStage.READY_TO_DISPATCH;
    }
    if (stages.stream().anyMatch(stage -> stage == OrderItemStage.IN_PLATING)) {
      return OrderItemStage.IN_PLATING;
    }
    return OrderItemStage.APPROVED;
  }

  /**
   * Splits one line's quantity across the stages of the works:
   *
   * <pre>
   *   approved        = ordered  - sent                   (never went to the plater)
   *   inPlating       = sent     - returned - ghati       (with the plater right now)
   *   readyToDispatch = returned - dispatched             (back, waiting to go out)
   *   dispatched      = dispatched                        (gone)
   * </pre>
   *
   * <p>Ghati is weight burnt off during the process: it leaves plating but never becomes
   * dispatchable, so it is subtracted rather than counted as returned.
   *
   * <p>The books mix units — the order is placed in pieces, the plater works in Kg, dispatch is
   * counted in pieces — so everything is normalised to Kg through the size's 1-pc weight and the
   * piece equivalent is reported alongside.
   */
  private ItemFulfillment describe(OrderItemEntity item) {
    Double pcsWeight = pcsWeight(item);

    double orderedPc = nullToZero(item.getQtyPc());
    Double orderedKg = item.getQtyKg() != null && item.getQtyKg() > 0
        ? item.getQtyKg()
        : toKg(orderedPc, pcsWeight);

    // A chitthi can cover several order lines (see JobWorkOrderItemEntity), so this line's figures
    // are its *share* of each one, not the whole. `shareOf` is 1.0 for an ordinary job work, so
    // unmerged lines are unaffected.
    List<JobWorkEntity> jobWorks = jobWorksTouching(item);
    double sentKg =
        jobWorks.stream().mapToDouble(jw -> nullToZero(jw.getQtyKg()) * shareOf(jw, item)).sum();

    double returnedKg =
        jobWorks.stream()
            .mapToDouble(jw -> sumReturns(jw, JobWorkReturnEntity::getReturnKg) * shareOf(jw, item))
            .sum();
    double ghatiKg =
        jobWorks.stream()
            .mapToDouble(jw -> sumReturns(jw, JobWorkReturnEntity::getGhati) * shareOf(jw, item))
            .sum();

    double dispatchedPc = dispatchedPc(item, orderedPc);
    Double dispatchedKg = toKg(dispatchedPc, pcsWeight);

    // What's still with the plater is pure Kg arithmetic, so it always works. The other three need
    // a Kg basis for the order line (its own qtyKg, or pieces converted through the size's 1-pc
    // weight); without one they are genuinely unknown, and null says so rather than showing a 0
    // that reads as "nothing left".
    double inPlatingKg = clampToZero(sentKg - returnedKg - ghatiKg);
    Double readyToDispatchKg =
        dispatchedKg == null
            ? (dispatchedPc > 0 ? null : clampToZero(returnedKg))
            : clampToZero(returnedKg - dispatchedKg);

    // Dispatch does not require plating — stock can go straight out. Anything dispatched beyond
    // what came back from the plater was therefore shipped without ever being sent, so it has to
    // come off "approved" as well; otherwise the same weight is reported both as still waiting to
    // be sent and as already gone.
    double shippedWithoutPlating =
        dispatchedKg == null ? 0d : clampToZero(dispatchedKg - returnedKg);
    Double approvedKg =
        orderedKg == null ? null : clampToZero(orderedKg - sentKg - shippedWithoutPlating);

    return new ItemFulfillment(
        stageOf(orderedPc, dispatchedPc, sentKg, returnedKg),
        round3(orderedKg),
        orderedPc,
        round3(approvedKg),
        toPc(approvedKg, pcsWeight),
        round3(inPlatingKg),
        toPc(inPlatingKg, pcsWeight),
        round3(readyToDispatchKg),
        toPc(readyToDispatchKg, pcsWeight),
        round3(dispatchedKg),
        dispatchedPc,
        round3(ghatiKg),
        round3(sentKg),
        round3(returnedKg),
        round3(inPlatingKg));
  }

  /**
   * The single coarse stage for the admin badge. Quantity-wise a line is usually in several at
   * once; this reports the furthest thing that has happened to it.
   */
  private OrderItemStage stageOf(
      double orderedPc, double dispatchedPc, double sentKg, double returnedKg) {
    if (orderedPc > 0 && dispatchedPc >= orderedPc - EPSILON) {
      return OrderItemStage.DISPATCHED;
    }
    if (returnedKg > EPSILON) {
      return OrderItemStage.READY_TO_DISPATCH;
    }
    if (sentKg > EPSILON) {
      return OrderItemStage.IN_PLATING;
    }
    return OrderItemStage.APPROVED;
  }

  /**
   * Every chitthi this line appears on: the ones it owns outright, plus the merged ones it was
   * folded into. Distinct by id, because a merged chitthi's primary line reaches it both ways.
   */
  private List<JobWorkEntity> jobWorksTouching(OrderItemEntity item) {
    Map<Long, JobWorkEntity> byId = new LinkedHashMap<>();
    if (item.getJobWorks() != null) {
      item.getJobWorks().forEach(jw -> byId.put(jw.getId(), jw));
    }
    if (item.getJobWorkAllocations() != null) {
      item.getJobWorkAllocations().stream()
          .map(JobWorkOrderItemEntity::getJobWork)
          .filter(Objects::nonNull)
          .forEach(jw -> byId.putIfAbsent(jw.getId(), jw));
    }
    return List.copyOf(byId.values());
  }

  /**
   * How much of {@code jobWork} belongs to {@code item}, as a fraction of the whole.
   *
   * <p>1.0 for an unmerged chitthi. For a merged one it is the line's allocated Kg over the
   * chitthi's total, which is also how returns and ghati are apportioned — the plater weighs the
   * batch as a whole and never tells us which line a given kilo came back from, so proportional is
   * the only defensible split.
   */
  private double shareOf(JobWorkEntity jobWork, OrderItemEntity item) {
    List<JobWorkOrderItemEntity> allocations = jobWork.getMergedOrderItems();
    if (allocations == null || allocations.isEmpty()) {
      // Not merged: it counts in full for its own line, and not at all for anyone else's.
      return jobWork.getOrderItem() != null && jobWork.getOrderItem().getId().equals(item.getId())
          ? 1d
          : 0d;
    }

    double total = jobWork.getQtyKg() != null ? jobWork.getQtyKg() : 0d;
    double mine =
        allocations.stream()
            .filter(a -> a.getOrderItem() != null && item.getId().equals(a.getOrderItem().getId()))
            .mapToDouble(a -> nullToZero(a.getQtyKg()))
            .sum();
    if (total <= 0) return 0d;
    return Math.min(1d, mine / total);
  }

  private double sumReturns(
      JobWorkEntity jobWork, java.util.function.Function<JobWorkReturnEntity, Double> field) {
    if (jobWork.getJobWorkReturns() == null) return 0d;
    return jobWork.getJobWorkReturns().stream()
        .mapToDouble(r -> nullToZero(field.apply(r)))
        .sum();
  }

  private Double pcsWeight(OrderItemEntity item) {
    ItemBlueprintDataEntity size = item.getItemSize();
    Double weight = size == null ? null : size.getPcsWeight();
    return weight != null && weight > 0 ? weight : null;
  }

  /** Null when the size has no 1-pc weight — better an absent figure than a made-up one. */
  private Double toKg(double pc, Double pcsWeight) {
    return pcsWeight == null ? null : round3(pc * pcsWeight);
  }

  private Double toPc(Double kg, Double pcsWeight) {
    return pcsWeight == null || kg == null ? null : round3(kg / pcsWeight);
  }

  /**
   * {@code totalDispatchedPc} is an @Formula, so on an entity loaded before a dispatch was saved in
   * this transaction it still reads the old total. {@code pendingPc} is written explicitly by the
   * dispatch service, so take whichever of the two is further along.
   */
  private double dispatchedPc(OrderItemEntity item, double orderedPc) {
    double dispatched = nullToZero(item.getTotalDispatchedPc());
    if (item.getPendingPc() != null) {
      dispatched = Math.max(dispatched, orderedPc - item.getPendingPc());
    }
    return clampToZero(dispatched);
  }

  private ClientOrderRequestStatus toRequestStatus(OrderItemStage stage) {
    return switch (stage) {
      case DISPATCHED -> ClientOrderRequestStatus.DISPATCHED;
      case READY_TO_DISPATCH -> ClientOrderRequestStatus.READY_TO_DISPATCH;
      case IN_PLATING -> ClientOrderRequestStatus.IN_PLATING;
      case APPROVED -> ClientOrderRequestStatus.APPROVED;
    };
  }

  private static double clampToZero(double value) {
    return value < EPSILON ? 0d : value;
  }

  private static Double round3(Double value) {
    return value == null ? null : Math.round(value * 1000.0) / 1000.0;
  }

  private static double nullToZero(Double value) {
    return value == null ? 0d : value;
  }
}
