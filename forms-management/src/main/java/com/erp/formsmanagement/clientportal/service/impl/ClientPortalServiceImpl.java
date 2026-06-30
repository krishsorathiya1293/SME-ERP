package com.erp.formsmanagement.clientportal.service.impl;

import com.erp.api.clientportalmanagement.model.CatalogItem;
import com.erp.api.clientportalmanagement.model.CatalogItemSize;
import com.erp.api.clientportalmanagement.model.ChangePasswordRequest;
import com.erp.api.clientportalmanagement.model.ClientOrder;
import com.erp.api.clientportalmanagement.model.ClientOrderItem;
import com.erp.api.clientportalmanagement.model.ClientProfile;
import com.erp.api.clientportalmanagement.model.NewOrderRequest;
import com.erp.api.clientportalmanagement.model.NewOrderRequestItem;
import com.erp.api.clientportalmanagement.model.OrderRequest;
import com.erp.api.clientportalmanagement.model.PaginatedResultClientOrder;
import com.erp.api.clientportalmanagement.model.PaginatedResultOrderRequest;
import com.erp.api.clientportalmanagement.model.PartyType;
import com.erp.api.clientportalmanagement.model.UpdateClientProfileRequest;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.clientportal.ClientPortalConstants;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestEntity;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestItemEntity;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestStatus;
import com.erp.formsmanagement.clientportal.domain.repository.ClientOrderRequestRepository;
import com.erp.formsmanagement.clientportal.mapper.ClientOrderRequestMapper;
import com.erp.formsmanagement.clientportal.service.ClientPortalService;
import com.erp.formsmanagement.clientportal.service.CurrentClientProvider;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.domain.repository.order.OrderRepository;
import com.erp.usermanagement.model.entity.UserEntity;
import com.erp.usermanagement.service.UserService;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientPortalServiceImpl implements ClientPortalService {

  private final CurrentClientProvider currentClientProvider;
  private final PartyRepository partyRepository;
  private final OrderRepository orderRepository;
  private final UserService userService;
  private final ItemBlueprintRepository itemBlueprintRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;
  private final ClientOrderRequestRepository clientOrderRequestRepository;
  private final ClientOrderRequestMapper clientOrderRequestMapper;

  @Override
  @Transactional(readOnly = true)
  public ClientProfile getMyProfile() {
    UserEntity user = currentClientProvider.getCurrentUser();
    PartyEntity party = getParty(user);
    return toClientProfile(user, party);
  }

  @Override
  @Transactional
  public ClientProfile updateMyProfile(UpdateClientProfileRequest request) {
    UserEntity user = currentClientProvider.getCurrentUser();
    PartyEntity party = getParty(user);

    if (request.getEmail() != null) {
      party.setEmail(request.getEmail());
    }
    if (request.getContactNo() != null) {
      party.setContactNo(request.getContactNo());
    }
    party = partyRepository.save(party);

    return toClientProfile(user, party);
  }

  @Override
  @Transactional
  public void changeMyPassword(ChangePasswordRequest request) {
    UserEntity user = currentClientProvider.getCurrentUser();
    userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultClientOrder getMyOrders(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortBy,
      Optional<String> sortDirection) {
    UserEntity user = currentClientProvider.getCurrentUser();
    Pageable pageable = PaginationUtils.getPageRequest(page, size, sortDirection, sortBy);
    Page<OrderEntity> orders = orderRepository.findByParty_Id(user.getPartyId(), pageable);

    var data = orders.getContent().stream().map(this::toClientOrder).toList();

    return new PaginatedResultClientOrder()
        .data(data)
        .empty(data.isEmpty())
        .last(orders.isLast())
        .first(orders.isFirst())
        .totalPages(orders.getTotalPages())
        .totalElements((int) orders.getTotalElements())
        .size(data.size());
  }

  @Override
  @Transactional(readOnly = true)
  public List<CatalogItem> getMyCatalog() {
    return itemBlueprintRepository.findAll().stream().map(this::toCatalogItem).toList();
  }

  @Override
  @Transactional
  public OrderRequest submitOrderRequest(NewOrderRequest request) {
    UserEntity user = currentClientProvider.getCurrentUser();
    PartyEntity party = getParty(user);

    ClientOrderRequestEntity entity = new ClientOrderRequestEntity();
    entity.setParty(party);
    entity.setStatus(ClientOrderRequestStatus.PENDING_APPROVAL);
    entity.setOrderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now());
    entity.setItems(
        request.getItems().stream().map(item -> toItemEntity(item, entity)).toList());

    ClientOrderRequestEntity saved = clientOrderRequestRepository.save(entity);
    return clientOrderRequestMapper.toDomain(saved, user.getUsername());
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultOrderRequest getMyOrderRequests(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortBy,
      Optional<String> sortDirection) {
    UserEntity user = currentClientProvider.getCurrentUser();
    Pageable pageable = PaginationUtils.getPageRequest(page, size, sortDirection, sortBy);
    Page<ClientOrderRequestEntity> requests =
        clientOrderRequestRepository.findByParty_Id(user.getPartyId(), pageable);

    return PageMapper.toResult(
        requests,
        entity -> clientOrderRequestMapper.toDomain(entity, user.getUsername()),
        PaginatedResultOrderRequest::new,
        PaginatedResultOrderRequest::setData);
  }

  private ClientOrderRequestItemEntity toItemEntity(
      NewOrderRequestItem dto, ClientOrderRequestEntity request) {
    ClientOrderRequestItemEntity item = new ClientOrderRequestItemEntity();
    item.setRequest(request);

    if (dto.getItemId() != null) {
      itemBlueprintRepository.findById(dto.getItemId()).ifPresent(item::setItem);
    }
    if (dto.getSizeId() != null) {
      itemBlueprintDataRepository.findById(dto.getSizeId()).ifPresent(item::setItemSize);
    }

    item.setItemName(dto.getItemName());
    item.setCategory(dto.getCategory());
    item.setSizeInInch(dto.getSizeInInch());
    item.setSizeInMm(dto.getSizeInMm());
    item.setPlating(dto.getPlating());
    item.setQtyPc(dto.getQtyPc());
    item.setQtyKg(dto.getQtyKg());
    item.setPendingPc(dto.getQtyPc());
    return item;
  }

  private CatalogItem toCatalogItem(com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity item) {
    var sizes =
        item.getSizes() == null
            ? List.<CatalogItemSize>of()
            : item.getSizes().stream()
                .map(
                    s -> {
                      var cs = new CatalogItemSize()
                            .id(s.getId())
                            .sizeInInch(s.getSizeInInch())
                            .sizeInMm(s.getSizeInMm());
                      if (s.getInventory() != null) {
                        cs.pcsPerBox(s.getInventory().getPcsPerBox());
                        cs.pcsPerCarton(s.getInventory().getPcsPerCarton());
                      }
                      return cs;
                    })
                .toList();

    return new CatalogItem()
        .id(item.getId())
        .itemName(item.getItemName())
        .category(item.getCategory() == null ? null : item.getCategory().getName())
        .sizes(sizes)
        .platings(ClientPortalConstants.FINISH_OPTIONS);
  }

  private PartyEntity getParty(UserEntity user) {
    return partyRepository
        .findById(user.getPartyId())
        .orElseThrow(
            () -> new EntityNotFoundException("Party not found with id: " + user.getPartyId()));
  }

  private ClientProfile toClientProfile(UserEntity user, PartyEntity party) {
    return new ClientProfile()
        .username(user.getUsername())
        .partyId(party.getId())
        .name(party.getName())
        .gst(party.getGst())
        .contactNo(party.getContactNo())
        .email(party.getEmail())
        .partyType(party.getPartyType() == null ? null : PartyType.valueOf(party.getPartyType().name()));
  }

  private ClientOrder toClientOrder(OrderEntity order) {
    var items =
        order.getOrderItems() == null
            ? java.util.List.<ClientOrderItem>of()
            : order.getOrderItems().stream().map(this::toClientOrderItem).toList();

    return new ClientOrder()
        .id(order.getId())
        .orderDate(order.getOrderDate())
        .items(items)
        .createdAt(toOffsetDateTime(order));
  }

  private ClientOrderItem toClientOrderItem(OrderItemEntity item) {
    return new ClientOrderItem()
        .id(item.getId())
        .itemName(
            item.getItemSize() == null || item.getItemSize().getItem() == null
                ? null
                : item.getItemSize().getItem().getItemName())
        .sizeInInch(item.getItemSize() == null ? null : item.getItemSize().getSizeInInch())
        .sizeInMm(item.getItemSize() == null ? null : item.getItemSize().getSizeInMm())
        .plating(item.getPlating())
        .qtyPc(item.getQtyPc())
        .qtyKg(item.getQtyKg())
        .pendingPc(item.getPendingPc())
        .dispatchedPc(item.getTotalDispatchedPc())
        .jobActionDone(item.getJobWork() != null);
  }

  private OffsetDateTime toOffsetDateTime(OrderEntity order) {
    return order.getCreatedAt() == null ? null : order.getCreatedAt().atOffset(ZoneOffset.UTC);
  }
}
