package com.erp.formsmanagement.clientportal.service.impl;

import com.erp.api.clientportalmanagement.model.CatalogItem;
import com.erp.api.clientportalmanagement.model.CatalogItemSize;
import com.erp.api.clientportalmanagement.model.ChangePasswordRequest;
import com.erp.api.clientportalmanagement.model.ClientItemJobWork;
import com.erp.api.clientportalmanagement.model.ClientOrder;
import com.erp.api.clientportalmanagement.model.ClientOrderItem;
import com.erp.api.clientportalmanagement.model.ClientProfile;
import com.erp.api.clientportalmanagement.model.Company;
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
import com.erp.formsmanagement.clientportal.service.ClientOrderFulfillmentService;
import com.erp.formsmanagement.clientportal.service.ClientOrderFulfillmentService.ItemFulfillment;
import com.erp.formsmanagement.clientportal.service.ClientPortalService;
import com.erp.formsmanagement.clientportal.service.CurrentClientProvider;
import com.erp.formsmanagement.domain.entity.client.ClientInventoryEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkEntity;
import com.erp.formsmanagement.domain.entity.order.JobWorkReturnEntity;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.entity.order.OrderItemEntity;
import com.erp.formsmanagement.domain.repository.client.ClientInventoryRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
  private final ClientInventoryRepository clientInventoryRepository;
  private final ClientOrderRequestRepository clientOrderRequestRepository;
  private final ClientOrderRequestMapper clientOrderRequestMapper;
  private final ClientOrderFulfillmentService clientOrderFulfillmentService;

  @Override
  @Transactional(readOnly = true)
  public ClientProfile getMyProfile() {
    UserEntity user = currentClientProvider.getCurrentUser();
    PartyEntity party = getActiveParty();
    return toClientProfile(user, party);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Company> getMyCompanies() {
    return currentClientProvider.accessibleParties().stream()
        .map(p -> new Company().partyId(p.getId()).partyName(p.getName()))
        .toList();
  }

  @Override
  @Transactional
  public ClientProfile updateMyProfile(UpdateClientProfileRequest request) {
    UserEntity user = currentClientProvider.getCurrentUser();
    PartyEntity party = getActiveParty();

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
    Long partyId = currentClientProvider.getActivePartyId();
    Pageable pageable = PaginationUtils.getPageRequest(page, size, sortDirection, sortBy);
    Page<OrderEntity> orders = orderRepository.findByParty_Id(partyId, pageable);

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
    Long partyId = currentClientProvider.getActivePartyId();

    // A client_inventory row exists only for the specific sizes the admin has assigned/sells to this
    // client, and it carries that client's own packing (pcs/box, pcs/carton) from the client
    // management sheet. If the client has any such rows, restrict the catalog to exactly those sizes
    // and use their per-client packing. If none are configured, we sell everything, so fall back to
    // the full catalog with the stock-master packing.
    Map<Long, ClientInventoryEntity> clientPackingBySizeId =
        clientInventoryRepository.findByParty_Id(partyId).stream()
            .collect(Collectors.toMap(ci -> ci.getSize().getId(), ci -> ci, (a, b) -> a));

    if (clientPackingBySizeId.isEmpty()) {
      return itemBlueprintRepository.findAll().stream().map(item -> toCatalogItem(item, null)).toList();
    }

    return itemBlueprintRepository.findAll().stream()
        .map(item -> toCatalogItem(item, clientPackingBySizeId))
        .filter(catalogItem -> !catalogItem.getSizes().isEmpty())
        .toList();
  }

  @Override
  @Transactional
  public OrderRequest submitOrderRequest(NewOrderRequest request) {
    UserEntity user = currentClientProvider.getCurrentUser();
    PartyEntity party = getActivePartyForWrite();

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
    Long partyId = currentClientProvider.getActivePartyId();
    Pageable pageable = PaginationUtils.getPageRequest(page, size, sortDirection, sortBy);
    Page<ClientOrderRequestEntity> requests =
        clientOrderRequestRepository.findByParty_Id(partyId, pageable);

    return PageMapper.toResult(
        requests,
        entity ->
            clientOrderRequestMapper.toDomain(entity, user.getUsername(), fulfillmentOf(entity)),
        PaginatedResultOrderRequest::new,
        PaginatedResultOrderRequest::setData);
  }

  /**
   * Per-line progress from the order this request became. Looked up by id rather than through the
   * lazy association so a request whose order has since been deleted degrades to "no progress"
   * instead of blowing up on a dangling proxy.
   */
  private Map<String, ItemFulfillment> fulfillmentOf(ClientOrderRequestEntity entity) {
    if (entity.getOrder() == null) {
      return Map.of();
    }
    return orderRepository
        .findById(entity.getOrder().getId())
        .map(clientOrderFulfillmentService::describeByLine)
        .orElseGet(Map::of);
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

  private CatalogItem toCatalogItem(
      com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity item,
      Map<Long, ClientInventoryEntity> clientPackingBySizeId) {
    var sizes =
        item.getSizes() == null
            ? List.<CatalogItemSize>of()
            : item.getSizes().stream()
                .filter(s -> clientPackingBySizeId == null || clientPackingBySizeId.containsKey(s.getId()))
                .map(
                    s -> {
                      var cs = new CatalogItemSize()
                            .id(s.getId())
                            .sizeInInch(s.getSizeInInch())
                            .sizeInMm(s.getSizeInMm());
                      applyPacking(cs, s, clientPackingBySizeId);
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

  /**
   * Sets the box/carton packing for a catalog size. For a client-specific catalog the packing comes
   * from that client's client_inventory row (the client management sheet); a blank client cell falls
   * back to the stock-master inventory so the Box/Carton unit is not lost. For the full catalog
   * (clientPackingBySizeId == null) the stock-master packing is used.
   */
  private void applyPacking(
      CatalogItemSize cs,
      com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity size,
      Map<Long, ClientInventoryEntity> clientPackingBySizeId) {
    var base = size.getInventory();
    Integer basePcsPerBox = base == null ? null : base.getPcsPerBox();
    Integer basePcsPerCarton = base == null ? null : base.getPcsPerCarton();

    if (clientPackingBySizeId == null) {
      cs.pcsPerBox(basePcsPerBox);
      cs.pcsPerCarton(basePcsPerCarton);
      return;
    }

    ClientInventoryEntity ci = clientPackingBySizeId.get(size.getId());
    cs.pcsPerBox(ci != null && ci.getPcsPerBox() != null ? ci.getPcsPerBox() : basePcsPerBox);
    cs.pcsPerCarton(
        ci != null && ci.getPcsPerCarton() != null ? ci.getPcsPerCarton() : basePcsPerCarton);
  }

  private PartyEntity getActiveParty() {
    Long partyId = currentClientProvider.getActivePartyId();
    return partyRepository
        .findById(partyId)
        .orElseThrow(() -> new EntityNotFoundException("Party not found with id: " + partyId));
  }

  /**
   * Resolves the active party for a WRITE, refusing to guess for a group login (see
   * {@link CurrentClientProvider#requireActivePartyId()}). Prevents an order from being silently
   * created for the first company when the client's company selection didn't reach the server.
   */
  private PartyEntity getActivePartyForWrite() {
    Long partyId = currentClientProvider.requireActivePartyId();
    return partyRepository
        .findById(partyId)
        .orElseThrow(() -> new EntityNotFoundException("Party not found with id: " + partyId));
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
        .jobActionDone(item.getJobWork() != null)
        .jobWork(toClientItemJobWork(item.getJobWork()));
  }

  /**
   * Per-item job-work progress for the client portal. Kg returned so far is the sum over the job
   * work's returns; ghati (loss burnt off during the process) is tracked separately from the
   * returned weight, so both count against what is still lying with the job worker.
   */
  private ClientItemJobWork toClientItemJobWork(JobWorkEntity jobWork) {
    if (jobWork == null) {
      return null;
    }

    List<JobWorkReturnEntity> returns =
        jobWork.getJobWorkReturns() == null ? List.of() : jobWork.getJobWorkReturns();
    double sentKg = jobWork.getQtyKg() == null ? 0d : jobWork.getQtyKg();
    double returnedKg =
        returns.stream().mapToDouble(r -> r.getReturnKg() == null ? 0d : r.getReturnKg()).sum();
    double ghatiKg =
        returns.stream().mapToDouble(r -> r.getGhati() == null ? 0d : r.getGhati()).sum();

    return new ClientItemJobWork()
        .type(jobWork.getJobWorkType() == null ? null : jobWork.getJobWorkType().name())
        .typeLabel(jobWork.getJobWorkType() == null ? null : jobWork.getJobWorkType().getValue())
        .status(jobWork.getStatus() == null ? null : jobWork.getStatus().name())
        .finish(jobWork.getFinish())
        .jobWorkNo(jobWork.getJobWorkNo())
        .sentKg(sentKg)
        .returnedKg(returnedKg)
        .ghatiKg(ghatiKg)
        .remainingKg(Math.max(0d, sentKg - returnedKg - ghatiKg));
  }

  private OffsetDateTime toOffsetDateTime(OrderEntity order) {
    return order.getCreatedAt() == null ? null : order.getCreatedAt().atOffset(ZoneOffset.UTC);
  }
}
