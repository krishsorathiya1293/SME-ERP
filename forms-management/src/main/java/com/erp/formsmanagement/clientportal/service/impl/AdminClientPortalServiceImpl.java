package com.erp.formsmanagement.clientportal.service.impl;

import com.erp.api.clientportalmanagement.model.ClientAccount;
import com.erp.api.clientportalmanagement.model.ClientCredentials;
import com.erp.api.clientportalmanagement.model.OrderRequest;
import com.erp.api.clientportalmanagement.model.OrderRequestStatus;
import com.erp.api.clientportalmanagement.model.PaginatedResultClientAccount;
import com.erp.api.clientportalmanagement.model.PaginatedResultOrderRequest;
import com.erp.api.clientportalmanagement.model.PartyType;
import com.erp.api.clientportalmanagement.model.UpdateOrderRequestStatusRequest;
import com.erp.api.ordermanagement.model.NewOrder;
import com.erp.api.ordermanagement.model.NewOrderItem;
import com.erp.api.ordermanagement.model.Order;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestEntity;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestItemEntity;
import com.erp.formsmanagement.clientportal.domain.entity.ClientOrderRequestStatus;
import com.erp.formsmanagement.clientportal.domain.repository.ClientOrderRequestRepository;
import com.erp.formsmanagement.clientportal.mapper.ClientOrderRequestMapper;
import com.erp.formsmanagement.clientportal.service.AdminClientPortalService;
import com.erp.formsmanagement.domain.repository.client.ClientInventoryRepository;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.entity.order.OrderEntity;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.service.order.OrderService;
import com.erp.usermanagement.model.entity.UserEntity;
import com.erp.usermanagement.repository.UserRepository;
import com.erp.usermanagement.service.UserService;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import com.erp.wrappers.CreateOne;
import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminClientPortalServiceImpl implements AdminClientPortalService {

  private static final String[] EXPORT_HEADERS = {
    "Party Name", "GST", "Contact No", "Email", "Party Type", "Username", "Password", "Status"
  };

  private final PartyRepository partyRepository;
  private final UserRepository userRepository;
  private final UserService userService;
  private final ClientOrderRequestRepository clientOrderRequestRepository;
  private final ClientOrderRequestMapper clientOrderRequestMapper;
  private final ClientInventoryRepository clientInventoryRepository;
  private final OrderService orderService;

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultClientAccount getAllClientAccounts(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> search,
      Optional<String> sortBy,
      Optional<String> sortDirection) {
    Pageable pageable = PaginationUtils.getPageRequest(page, size, sortDirection, sortBy);
    Page<PartyEntity> parties =
        partyRepository.findAll(partyRepository.filter(null, search.orElse(null)), pageable);

    List<ClientAccount> data = parties.getContent().stream().map(this::toClientAccount).toList();

    return new PaginatedResultClientAccount()
        .data(data)
        .empty(data.isEmpty())
        .last(parties.isLast())
        .first(parties.isFirst())
        .totalPages(parties.getTotalPages())
        .totalElements((int) parties.getTotalElements())
        .size(data.size());
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] exportClientCredentials(boolean onlyPending) {
    List<PartyEntity> parties = partyRepository.findAll();

    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Client Credentials");

      Row header = sheet.createRow(0);
      for (int i = 0; i < EXPORT_HEADERS.length; i++) {
        header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
      }

      int rowIndex = 1;
      for (PartyEntity party : parties) {
        UserEntity user = userRepository.findByPartyId(party.getId()).orElse(null);
        if (user == null) {
          continue;
        }
        boolean pending = user.getInitialPassword() != null;
        if (onlyPending && !pending) {
          continue;
        }

        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(nullToEmpty(party.getName()));
        row.createCell(1).setCellValue(nullToEmpty(party.getGst()));
        row.createCell(2).setCellValue(nullToEmpty(party.getContactNo()));
        row.createCell(3).setCellValue(nullToEmpty(party.getEmail()));
        row.createCell(4)
            .setCellValue(party.getPartyType() == null ? "" : party.getPartyType().name());
        row.createCell(5).setCellValue(nullToEmpty(user.getUsername()));
        row.createCell(6).setCellValue(pending ? user.getInitialPassword() : "");
        row.createCell(7).setCellValue(pending ? "Pending" : "Changed");
      }

      for (int i = 0; i < EXPORT_HEADERS.length; i++) {
        sheet.autoSizeColumn(i);
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      workbook.write(out);
      return out.toByteArray();
    } catch (java.io.IOException e) {
      throw new UncheckedIOException("Failed to generate client credentials export", e);
    }
  }

  @Override
  @Transactional
  public ClientCredentials resetClientCredentials(Long partyId) {
    UserEntity user = userService.resetClientCredentials(partyId);
    return new ClientCredentials().username(user.getUsername()).password(user.getInitialPassword());
  }

  @Override
  @Transactional(readOnly = true)
  public PaginatedResultOrderRequest getAllOrderRequests(
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortBy,
      Optional<String> sortDirection,
      Optional<Long> partyId,
      Optional<OrderRequestStatus> status,
      Optional<String> search) {
    Pageable pageable = PaginationUtils.getPageRequest(page, size, sortDirection, sortBy);

    Optional<ClientOrderRequestStatus> statusEntity =
        status.map(s -> ClientOrderRequestStatus.valueOf(s.name()));

    Page<ClientOrderRequestEntity> requests =
        clientOrderRequestRepository.findAll(
            clientOrderRequestRepository.filter(partyId, statusEntity, search), pageable);

    return PageMapper.toResult(
        requests,
        this::toOrderRequest,
        PaginatedResultOrderRequest::new,
        PaginatedResultOrderRequest::setData);
  }

  @Override
  @Transactional
  public OrderRequest updateOrderRequestStatus(Long id, UpdateOrderRequestStatusRequest request) {
    ClientOrderRequestEntity entity =
        clientOrderRequestRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Order request not found with id: " + id));

    ClientOrderRequestStatus oldStatus = entity.getStatus();
    ClientOrderRequestStatus newStatus = ClientOrderRequestStatus.valueOf(request.getStatus().name());
    entity.setStatus(newStatus);
    ClientOrderRequestEntity saved = clientOrderRequestRepository.save(entity);

    if (newStatus == ClientOrderRequestStatus.APPROVED
        && oldStatus != ClientOrderRequestStatus.APPROVED) {
      createOrderFromRequest(saved);
    }

    return toOrderRequest(saved);
  }

  /**
   * When an order request is approved, create a corresponding order in Order Management so it
   * can be tracked/processed (job work, dispatch, etc.) like any other order. Only items that
   * reference a known catalog item size can be carried over, since orders require a valid
   * item size reference.
   */
  private void createOrderFromRequest(ClientOrderRequestEntity entity) {
    List<ClientOrderRequestItemEntity> items = entity.getItems();
    if (items == null || items.isEmpty()) {
      return;
    }

    Long partyId = entity.getParty().getId();
    List<NewOrderItem> orderItems =
        items.stream()
            .filter(item -> item.getItemSize() != null)
            .map(
                item -> {
                  NewOrderItem orderItem =
                      new NewOrderItem()
                          .itemSizeId(item.getItemSize().getId())
                          .plating(item.getPlating())
                          .qtyPc(item.getQtyPc())
                          .qtyKg(item.getQtyKg())
                          .pendingPc(item.getQtyPc());

                  // Packing (pcs/box, box/carton, pcs/carton) must reflect this CLIENT's negotiated
                  // packing from Client Management, not the item master's defaults — the same client
                  // often boxes/cartons a size differently from the master. Falls back to the master
                  // (via the display's own fallback) only when the client has no row for the size.
                  clientInventoryRepository
                      .findByParty_IdAndSize_Id(partyId, item.getItemSize().getId())
                      .ifPresent(
                          ci -> {
                            if (ci.getPcsPerBox() != null) {
                              orderItem.setPcPerBox(ci.getPcsPerBox().doubleValue());
                            }
                            if (ci.getBoxPerCarton() != null) {
                              orderItem.setBoxPerCartoon(ci.getBoxPerCarton().doubleValue());
                            }
                            if (ci.getPcsPerCarton() != null) {
                              orderItem.setPcPerCartoon(ci.getPcsPerCarton().doubleValue());
                            }
                            if (ci.getPcsPerBox() != null
                                && ci.getPcsPerBox() > 0
                                && item.getQtyPc() != null) {
                              orderItem.setStickerQty(
                                  Math.ceil(item.getQtyPc() / ci.getPcsPerBox()));
                            }
                          });
                  return orderItem;
                })
            .toList();

    if (orderItems.isEmpty()) {
      return;
    }

    NewOrder newOrder =
        new NewOrder()
            .orderDate(entity.getOrderDate() != null ? entity.getOrderDate() : LocalDate.now())
            .items(orderItems);

    var result = orderService.save(entity.getParty().getId(), newOrder);
    if (result instanceof CreateOne<Order> created && created.item().getId() != null) {
      OrderEntity orderRef = new OrderEntity();
      orderRef.setId(created.item().getId());
      entity.setOrder(orderRef);
      clientOrderRequestRepository.save(entity);
    }
  }

  private OrderRequest toOrderRequest(ClientOrderRequestEntity entity) {
    String username =
        entity.getParty() == null
            ? null
            : userRepository
                .findByPartyId(entity.getParty().getId())
                .map(UserEntity::getUsername)
                .orElse(null);
    return clientOrderRequestMapper.toDomain(entity, username);
  }

  private ClientAccount toClientAccount(PartyEntity party) {
    ClientAccount account =
        new ClientAccount()
            .partyId(party.getId())
            .partyName(party.getName())
            .gst(party.getGst())
            .contactNo(party.getContactNo())
            .email(party.getEmail())
            .partyType(party.getPartyType() == null ? null : PartyType.valueOf(party.getPartyType().name()))
            .createdAt(toOffsetDateTime(party));

    userRepository
        .findByPartyId(party.getId())
        .ifPresent(
            user ->
                account
                    .userId(user.getId())
                    .username(user.getUsername())
                    .credentialsPending(user.getInitialPassword() != null)
                    .initialPassword(user.getInitialPassword())
                    .enabled(user.getEnabled()));

    return account;
  }

  private OffsetDateTime toOffsetDateTime(PartyEntity party) {
    return party.getCreatedAt() == null ? null : party.getCreatedAt().atOffset(ZoneOffset.UTC);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
