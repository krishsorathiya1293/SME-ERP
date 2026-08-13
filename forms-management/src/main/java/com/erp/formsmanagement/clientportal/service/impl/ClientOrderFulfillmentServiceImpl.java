package com.erp.formsmanagement.clientportal.service.impl;

import com.erp.api.clientportalmanagement.model.OrderItemStage;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestEntity;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestStatus;
import com.erp.formsmanagement.clientportal.domain.repository.ClientOrderRequestRepository;
import com.erp.formsmanagement.clientportal.service.ClientOrderFulfillmentService;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkStatus;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.formsmanagement.domain.repository.order.JobWorkReturnRepository;
import com.erp.formsmanagement.domain.repository.order.OrderRepository;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private final ClientOrderRequestRepository clientOrderRequestRepository;
  private final OrderRepository orderRepository;
  private final JobWorkReturnRepository jobWorkReturnRepository;

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

  /**
   * An order shows the furthest stage any of its lines has reached: the moment one line goes out
   * for plating the order has left "Approved", and the moment one comes back there is something
   * ready to dispatch. Real orders run a dozen lines that move at different speeds, so rolling up
   * the *least* advanced line would pin an order in "Approved" until the last line was sent —
   * which is the whole problem this replaces. Which lines are where is on the per-line detail.
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
   * Works out where a single line stands:
   *
   * <ul>
   *   <li>DISPATCHED — every piece ordered has gone out
   *   <li>READY_TO_DISPATCH — the job worker has returned something; on a partial return it is the
   *       returned Kg that is ready to go
   *   <li>IN_PLATING — sent out for job work, nothing back yet
   *   <li>APPROVED — not sent anywhere yet
   * </ul>
   */
  private ItemFulfillment describe(OrderItemEntity item) {
    double qtyPc = item.getQtyPc() == null ? 0d : item.getQtyPc();
    double dispatchedPc = dispatchedPc(item, qtyPc);

    JobWorkEntity jobWork = item.getJobWork();
    Double sentKg = jobWork == null ? null : jobWork.getQtyKg();
    double returnedKg =
        jobWork == null
            ? 0d
            : nullToZero(jobWorkReturnRepository.sumReturnKgByJobWorkId(jobWork.getId()));
    double ghatiKg =
        jobWork == null || jobWork.getJobWorkReturns() == null
            ? 0d
            : jobWork.getJobWorkReturns().stream()
                .mapToDouble(r -> r.getGhati() == null ? 0d : r.getGhati())
                .sum();

    // A return that was recorded in this very transaction isn't in the DB yet, but the job work's
    // status has already been flipped to COMPLETE for it — so trust either signal.
    boolean anyReturned =
        jobWork != null && (returnedKg > 0 || jobWork.getStatus() == JobWorkStatus.COMPLETE);

    OrderItemStage stage;
    if (qtyPc > 0 && dispatchedPc >= qtyPc) {
      stage = OrderItemStage.DISPATCHED;
    } else if (jobWork == null) {
      stage = OrderItemStage.APPROVED;
    } else if (anyReturned) {
      stage = OrderItemStage.READY_TO_DISPATCH;
    } else {
      stage = OrderItemStage.IN_PLATING;
    }

    Double remainingKg =
        sentKg == null ? null : Math.max(0d, sentKg - returnedKg - ghatiKg);

    return new ItemFulfillment(stage, sentKg, returnedKg, remainingKg, dispatchedPc);
  }

  /**
   * {@code totalDispatchedPc} is an @Formula, so on an entity loaded before a dispatch was saved in
   * this transaction it still reads the old total. {@code pendingPc} is written explicitly by the
   * dispatch service, so take whichever of the two is further along.
   */
  private double dispatchedPc(OrderItemEntity item, double qtyPc) {
    double dispatched = nullToZero(item.getTotalDispatchedPc());
    if (item.getPendingPc() != null) {
      dispatched = Math.max(dispatched, qtyPc - item.getPendingPc());
    }
    return Math.max(0d, dispatched);
  }

  private ClientOrderRequestStatus toRequestStatus(OrderItemStage stage) {
    return switch (stage) {
      case DISPATCHED -> ClientOrderRequestStatus.DISPATCHED;
      case READY_TO_DISPATCH -> ClientOrderRequestStatus.READY_TO_DISPATCH;
      case IN_PLATING -> ClientOrderRequestStatus.IN_PLATING;
      case APPROVED -> ClientOrderRequestStatus.APPROVED;
    };
  }

  private static double nullToZero(Double value) {
    return value == null ? 0d : value;
  }
}
