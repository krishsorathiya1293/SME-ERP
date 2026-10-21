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
import com.erp.formsmanagement.domain.entity.order.MergeMoveKind;
import com.erp.formsmanagement.domain.entity.order.OrderDispatchEntity;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemMergeMoveEntity;
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

    // When this line has been merged into another order, the work happens on the merged line and
    // nothing at all is recorded here. What is reported is this line's part of that work, so a
    // client who ordered 100 Kg of a merged 300 never sees the whole 300 against their order.
    OrderItemEntity worked = item.getMergedIntoItem() != null ? item.getMergedIntoItem() : item;
    Work work =
        worked != item && worked.getMergeMoves() != null && !worked.getMergeMoves().isEmpty()
            ? attributedWork(item, worked)
            : sharedWork(item, worked, orderedPc);
    double sentKg = work.sentKg();
    double returnedKg = work.returnedKg();
    double ghatiKg = work.ghatiKg();
    double dispatchedPc = work.dispatchedPc();
    Double dispatchedKg = toKg(dispatchedPc, pcsWeight);

    // What's still with the plater is pure Kg arithmetic, so it always works. The other three need
    // a Kg basis for the order line (its own qtyKg, or pieces converted through the size's 1-pc
    // weight); without one they are genuinely unknown, and null says so rather than showing a 0
    // that reads as "nothing left".
    double inPlatingKg = clampToZero(sentKg - returnedKg - ghatiKg);
    // Written long-hand rather than as a conditional expression on purpose. Mixing a Double
    // branch with a primitive double branch makes the WHOLE expression a double, so Java unboxes
    // the Double one -- and unboxing null threw NullPointerException on any line whose size has no
    // 1-pc weight the moment anything was dispatched against it. An if/else has no such promotion.
    Double readyToDispatchKg;
    if (dispatchedKg != null) {
      readyToDispatchKg = clampToZero(returnedKg - dispatchedKg);
    } else if (dispatchedPc > 0) {
      // No Kg basis and something has gone out: what is left is genuinely unknown, and null says
      // so rather than showing a 0 that reads as "nothing left".
      readyToDispatchKg = null;
    } else {
      readyToDispatchKg = clampToZero(returnedKg);
    }

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

  /** What has happened to one line: Kg sent, returned and burnt off at the plater, pcs dispatched. */
  private record Work(double sentKg, double returnedKg, double ghatiKg, double dispatchedPc) {}

  /**
   * The line's work when nothing was recorded against it before it was merged — an ordinary line,
   * or one merged while still untouched. All the merged line's work is shared out by what each
   * line ordered.
   */
  private Work sharedWork(OrderItemEntity item, OrderItemEntity worked, double orderedPc) {
    // `mergedShare` is 1.0 and `worked` is the line itself when unmerged.
    double mergedShare = mergedShareOf(item, worked);

    // A chitthi can cover several order lines (see JobWorkOrderItemEntity), so the worked line's
    // figures are its *share* of each one, not the whole. `shareOf` is 1.0 for an ordinary job
    // work, so unmerged lines are unaffected.
    List<JobWorkEntity> jobWorks = jobWorksTouching(worked);
    double sentKg =
        jobWorks.stream()
                .mapToDouble(jw -> nullToZero(jw.getQtyKg()) * shareOf(jw, worked))
                .sum()
            * mergedShare;

    double returnedKg =
        jobWorks.stream()
                .mapToDouble(
                    jw -> sumReturns(jw, JobWorkReturnEntity::getReturnKg) * shareOf(jw, worked))
                .sum()
            * mergedShare;
    double ghatiKg =
        jobWorks.stream()
                .mapToDouble(
                    jw -> sumReturns(jw, JobWorkReturnEntity::getGhati) * shareOf(jw, worked))
                .sum()
            * mergedShare;

    double dispatchedPc =
        worked == item
            ? dispatchedPc(item, orderedPc)
            : clampToZero(dispatchedPc(worked, nullToZero(worked.getQtyPc())) * mergedShare);
    return new Work(sentKg, returnedKg, ghatiKg, dispatchedPc);
  }

  /**
   * The line's work when it was merged with chitthis or dispatches already behind it.
   *
   * <p>Those records moved onto the merged line, and {@link OrderItemMergeMoveEntity} remembers
   * where each came from. A record made before the merge is credited in full to the order it was
   * made for — the client whose 168 pcs had already gone out still sees 168 gone, and nobody else
   * does. Only what was recorded after the merge is shared, and it is shared by what each line
   * still had to go rather than by what it ordered, so a line that was mostly done is not credited
   * twice over.
   */
  private Work attributedWork(OrderItemEntity item, OrderItemEntity merged) {
    List<OrderItemEntity> sources = merged.getMergedSourceItems();
    List<OrderItemMergeMoveEntity> moves = merged.getMergeMoves();
    List<JobWorkEntity> jobWorks = jobWorksTouching(merged);

    // Per chitthi: the part of it each source owned before the merge, and the part recorded since.
    Map<Long, Map<Long, Double>> originByJobWork = new HashMap<>();
    Map<Long, Double> postByJobWork = new HashMap<>();
    Map<Long, Double> sentBefore = new HashMap<>();
    for (JobWorkEntity jobWork : jobWorks) {
      Map<Long, Double> origin = originOf(jobWork, merged, moves);
      double owned = origin.values().stream().mapToDouble(Double::doubleValue).sum();
      originByJobWork.put(jobWork.getId(), origin);
      postByJobWork.put(jobWork.getId(), Math.max(0d, shareOf(jobWork, merged) - owned));
      origin.forEach(
          (sourceId, fraction) ->
              sentBefore.merge(sourceId, nullToZero(jobWork.getQtyKg()) * fraction, Double::sum));
    }

    Map<Long, Double> stillToSend = new HashMap<>();
    for (OrderItemEntity source : sources) {
      double orderedKg = nullToZero(orderedKgOf(source));
      stillToSend.put(
          source.getId(), Math.max(0d, orderedKg - sentBefore.getOrDefault(source.getId(), 0d)));
    }
    double sendWeight = weightOf(item, merged, stillToSend);

    double sentKg = 0d;
    double returnedKg = 0d;
    double ghatiKg = 0d;
    for (JobWorkEntity jobWork : jobWorks) {
      double fraction =
          originByJobWork.get(jobWork.getId()).getOrDefault(item.getId(), 0d)
              + postByJobWork.get(jobWork.getId()) * sendWeight;
      sentKg += nullToZero(jobWork.getQtyKg()) * fraction;
      returnedKg += sumReturns(jobWork, JobWorkReturnEntity::getReturnKg) * fraction;
      ghatiKg += sumReturns(jobWork, JobWorkReturnEntity::getGhati) * fraction;
    }

    // Dispatches the same way: each moved one belongs to the line it was made for.
    Map<Long, Long> dispatchSource = new HashMap<>();
    moves.stream()
        .filter(move -> move.getKind() == MergeMoveKind.DISPATCH)
        .forEach(move -> dispatchSource.put(move.getRecordId(), move.getSourceItem().getId()));
    Map<Long, Double> dispatchedBefore = new HashMap<>();
    if (merged.getDispatches() != null) {
      for (OrderDispatchEntity dispatch : merged.getDispatches()) {
        Long sourceId = dispatchSource.get(dispatch.getId());
        if (sourceId != null) {
          dispatchedBefore.merge(sourceId, nullToZero(dispatch.getDispatchPcs()), Double::sum);
        }
      }
    }
    double totalDispatched = dispatchedPc(merged, nullToZero(merged.getQtyPc()));
    double before = dispatchedBefore.values().stream().mapToDouble(Double::doubleValue).sum();
    double after = Math.max(0d, totalDispatched - before);

    Map<Long, Double> stillToGo = new HashMap<>();
    for (OrderItemEntity source : sources) {
      stillToGo.put(
          source.getId(),
          Math.max(
              0d,
              nullToZero(source.getQtyPc()) - dispatchedBefore.getOrDefault(source.getId(), 0d)));
    }
    double dispatchedPc =
        dispatchedBefore.getOrDefault(item.getId(), 0d)
            + after * weightOf(item, merged, stillToGo);

    return new Work(sentKg, returnedKg, ghatiKg, clampToZero(dispatchedPc));
  }

  /**
   * For one chitthi on a merged line, the fraction of it each source line owned before the merge.
   * Empty for a chitthi raised after the merge.
   */
  private Map<Long, Double> originOf(
      JobWorkEntity jobWork, OrderItemEntity merged, List<OrderItemMergeMoveEntity> moves) {
    Map<Long, Double> origin = new HashMap<>();
    List<JobWorkOrderItemEntity> allocations = jobWork.getMergedOrderItems();
    if (allocations != null && !allocations.isEmpty()) {
      // Split across several lines: each source owned the share it had on this chitthi.
      double total = nullToZero(jobWork.getQtyKg());
      if (total <= 0) {
        return origin;
      }
      for (JobWorkOrderItemEntity allocation : allocations) {
        if (allocation.getOrderItem() == null
            || !merged.getId().equals(allocation.getOrderItem().getId())) {
          continue;
        }
        moves.stream()
            .filter(
                move ->
                    move.getKind() == MergeMoveKind.ALLOCATION
                        && allocation.getId().equals(move.getRecordId()))
            .forEach(
                move ->
                    origin.merge(
                        move.getSourceItem().getId(),
                        Math.min(1d, nullToZero(move.getQtyKg()) / total),
                        Double::sum));
      }
      return origin;
    }
    // A plain chitthi belongs wholly to the line it was raised on.
    moves.stream()
        .filter(
            move ->
                move.getKind() == MergeMoveKind.JOB_WORK
                    && jobWork.getId().equals(move.getRecordId()))
        .findFirst()
        .ifPresent(move -> origin.put(move.getSourceItem().getId(), 1d));
    return origin;
  }

  /**
   * This line's part of what was recorded after the merge: what it still had outstanding, over
   * what every line still had. When nothing was outstanding anywhere, fall back to what it ordered.
   */
  private double weightOf(
      OrderItemEntity item, OrderItemEntity merged, Map<Long, Double> outstanding) {
    double total = outstanding.values().stream().mapToDouble(Double::doubleValue).sum();
    if (total > EPSILON) {
      return outstanding.getOrDefault(item.getId(), 0d) / total;
    }
    return mergedShareOf(item, merged);
  }

  private Double orderedKgOf(OrderItemEntity item) {
    if (item.getQtyKg() != null && item.getQtyKg() > 0) {
      return item.getQtyKg();
    }
    return toKg(nullToZero(item.getQtyPc()), pcsWeight(item));
  }

  /**
   * The single coarse stage for the admin badge. Quantity-wise a line is usually in several at
   * once; this reports the furthest thing that has happened to it.
   *
   * <p>DISPATCHED lands on the first piece out rather than the last, matching {@code
   * OrderMapper.toStatus} — one threshold, so the badge here and the status on the order sheet can
   * never disagree about the same line. This is a label only: the stage QUANTITIES beside it are
   * untouched and still split the line honestly across approved / in plating / ready / dispatched,
   * so a part-shipped line reports the real numbers even while it reads as dispatched.
   */
  private OrderItemStage stageOf(
      double orderedPc, double dispatchedPc, double sentKg, double returnedKg) {
    if (dispatchedPc > EPSILON) {
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
   * How much of a merged line belongs to one of the lines folded into it.
   *
   * <p>Ordered against ordered: a 100 Kg line inside a merged 300 owns a third of whatever the
   * merged line has done. Kg is preferred and pieces are the fallback, since an order placed only
   * in pieces still has to divide. Returns 1.0 when the line is not merged at all, and falls back
   * to an even split when neither basis is available -- an unknown share is better shared out than
   * silently dropped.
   */
  private double mergedShareOf(OrderItemEntity item, OrderItemEntity merged) {
    if (merged == item) {
      return 1d;
    }

    double mineKg = nullToZero(item.getQtyKg());
    double totalKg = nullToZero(merged.getQtyKg());
    if (mineKg > 0 && totalKg > 0) {
      return Math.min(1d, mineKg / totalKg);
    }

    double minePc = nullToZero(item.getQtyPc());
    double totalPc = nullToZero(merged.getQtyPc());
    if (minePc > 0 && totalPc > 0) {
      return Math.min(1d, minePc / totalPc);
    }

    List<OrderItemEntity> siblings = merged.getMergedSourceItems();
    int count = siblings == null || siblings.isEmpty() ? 1 : siblings.size();
    return 1d / count;
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
