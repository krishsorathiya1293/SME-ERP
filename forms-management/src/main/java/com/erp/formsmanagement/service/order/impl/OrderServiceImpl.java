package com.erp.formsmanagement.service.order.impl;

import com.erp.api.ordermanagement.model.NewOrder;
import com.erp.api.ordermanagement.model.Order;
import com.erp.api.ordermanagement.model.OrderParty;
import com.erp.api.ordermanagement.model.PaginatedPartyOrdersResponse;
import com.erp.api.ordermanagement.model.PaginatedResultOrder;
import com.erp.api.ordermanagement.model.PartyOrdersResponse;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.domain.repository.order.OrderRepository;
import com.erp.formsmanagement.mapper.order.OrderMapper;
import com.erp.formsmanagement.service.order.OrderService;
import com.erp.mapper.EntityMapper;
import com.erp.service.AbstractCrudServiceV2;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl extends AbstractCrudServiceV2<OrderEntity, NewOrder, Order, Long>
    implements OrderService {

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;
  private final PartyRepository partyRepository;

  @Override
  protected JpaRepository<OrderEntity, Long> repository() {
    return orderRepository;
  }

  @Override
  protected EntityMapper<OrderEntity, NewOrder, Order> mapper() {
    return orderMapper;
  }

  @Override
  protected void afterCreate(OrderEntity entity, Long partyId, NewOrder request) {
    entity.setParty(
        partyRepository
            .findById(partyId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format(Constant.ENTITY_NOT_FOUND, partyId))));
  }

  @Override
  protected void afterUpdate(OrderEntity entity, Long partyId, NewOrder request) {
    afterCreate(entity, partyId, request);
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultOrder getAll(Long partyId, GetAllQuery<String> query) {
    var party =
        partyRepository
            .findById(partyId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, partyId)));
    Specification<OrderEntity> spec =
        (root, q, cb) -> cb.equal(root.get("party").get("id"), partyId);
    Page<OrderEntity> page =
        orderRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));
    return PageMapper.toResult(
        page,
        orderMapper::toDomain,
        PaginatedResultOrder::new,
        (response, orders) -> {
          PartyOrdersResponse wrapper = new PartyOrdersResponse();
          wrapper.setParty(new OrderParty().id(party.getId()).name(party.getName()));
          wrapper.setOrders(orders);
          response.setData(wrapper);
        });
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedPartyOrdersResponse getAll(GetAllQuery<String> query) {

    Page<PartyEntity> partyPage =
        partyRepository.findAll(
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));

    List<Long> partyIds = partyPage.getContent().stream().map(PartyEntity::getId).toList();

    Map<Long, List<Order>> ordersByParty =
        orderRepository.findByParty_IdIn(partyIds).stream()
            .collect(
                Collectors.groupingBy(
                    oe -> oe.getParty().getId(),
                    Collectors.mapping(orderMapper::toDomain, Collectors.toList())));
    return PageMapper.toResult(
        partyPage,
        p -> p,
        PaginatedPartyOrdersResponse::new,
        (response, ignored) -> {
          List<PartyOrdersResponse> data = new ArrayList<>();

          for (PartyEntity party : partyPage.getContent()) {

            PartyOrdersResponse por = new PartyOrdersResponse();

            por.setParty(new OrderParty().id(party.getId()).name(party.getName()));

            por.setOrders(ordersByParty.getOrDefault(party.getId(), new ArrayList<>()));

            data.add(por);
          }

          response.setData(data);
        });
  }
}
