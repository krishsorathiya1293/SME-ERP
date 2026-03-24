package com.erp.formsmanagement.controller.sales;

import com.erp.api.salesmanagement.SalesOrderSalesManagementApi;
import com.erp.api.salesmanagement.model.NewSalesOrder;
import com.erp.api.salesmanagement.model.PaginatedResultSalesOrder;
import com.erp.api.salesmanagement.model.SalesOrder;
import com.erp.controller.AbstractCrudControllerV1;
import com.erp.formsmanagement.service.sales.SalesOrderService;
import com.erp.util.GetAllQuery;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SalesOrderController
    extends AbstractCrudControllerV1<
        NewSalesOrder, SalesOrder, String, PaginatedResultSalesOrder>
    implements SalesOrderSalesManagementApi {

  public SalesOrderController(SalesOrderService service) {
    super(service, service);
  }

  @Override
  public ResponseEntity<PaginatedResultSalesOrder> getAllSalesOrders(
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
  public ResponseEntity<SalesOrder> createSalesOrder(NewSalesOrder newSalesOrder) {
    return crud().createOne(newSalesOrder);
  }

  @Override
  public ResponseEntity<Void> deleteSalesOrder(Long id) {
    return crud().delete(id);
  }

  @Override
  public ResponseEntity<SalesOrder> getSalesOrderById(Long id) {
    return crud().getById(id);
  }

  @Override
  public ResponseEntity<SalesOrder> updateSalesOrder(Long id, NewSalesOrder newSalesOrder) {
    return crud().update(id, newSalesOrder);
  }
}
