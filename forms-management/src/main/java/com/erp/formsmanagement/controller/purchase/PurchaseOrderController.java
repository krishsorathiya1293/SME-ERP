package com.erp.formsmanagement.controller.purchase;

import com.erp.api.purchasemanagement.PurchaseOrderPurchaseManagementApi;
import com.erp.api.purchasemanagement.model.NewPurchaseOrder;
import com.erp.api.purchasemanagement.model.PaginatedResultPurchaseOrder;
import com.erp.api.purchasemanagement.model.PurchaseOrder;
import com.erp.controller.AbstractCrudControllerV1;
import com.erp.formsmanagement.service.purchase.PurchaseOrderService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PurchaseOrderController
    extends AbstractCrudControllerV1<
        NewPurchaseOrder, PurchaseOrder, String, PaginatedResultPurchaseOrder>
    implements PurchaseOrderPurchaseManagementApi {

  public PurchaseOrderController(PurchaseOrderService service) {
    super(service, service);
  }

  @Override
  public ResponseEntity<PaginatedResultPurchaseOrder> getAllPurchaseOrders(
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortByFields,
      Optional<String> direction) {
    return page()
        .getAll(
            GetAllQuery.of(Optional.empty(), search, page, size, sortByFields, direction));
  }

  @Override
  public ResponseEntity<PurchaseOrder> createPurchaseOrder(NewPurchaseOrder newPurchaseOrder) {
    return crud().createOne(newPurchaseOrder);
  }

  @Override
  public ResponseEntity<Void> deletePurchaseOrder(Long id) {
    return crud().delete(id);
  }

  @Override
  public ResponseEntity<PurchaseOrder> getPurchaseOrderById(Long id) {
    return crud().getById(id);
  }

  @Override
  public ResponseEntity<PurchaseOrder> updatePurchaseOrder(
      Long id, NewPurchaseOrder newPurchaseOrder) {
    return crud().update(id, newPurchaseOrder);
  }
}
