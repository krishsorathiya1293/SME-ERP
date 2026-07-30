package com.erp.formsmanagement.service.order;

import com.erp.api.ordermanagement.model.JobWork;
import com.erp.api.ordermanagement.model.NewJobWork;
import com.erp.api.ordermanagement.model.PaginatedResultJobWork;
import com.erp.api.ordermanagement.model.UpdateJobWorkStatus;
import com.erp.api.ordermanagement.model.UpdateJobWorkType;
import com.erp.formsmanagement.domain.entity.order.JobWorkType;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;
import com.erp.util.GetAllQuery;

public interface JobWorkService
    extends CoreServiceV2<Long, NewJobWork, JobWork, Long>,
        GetAllServiceV2<Long, Void, PaginatedResultJobWork> { 

  JobWork updateStatus(Long orderItemId, Long id, UpdateJobWorkStatus request);

  JobWork updateType(Long orderItemId, Long id, UpdateJobWorkType request);

  /** Create a job work that is NOT tied to any order item (Manual mode). */
  JobWork createManual(NewJobWork request);

  /** Update a job work that is NOT tied to any order item (Manual mode). */
  JobWork updateManual(Long id, NewJobWork request);

  /**
   * Global listing across every order item, for the dashboard / plating pages.
   * {@code type} filters by JobWorkType; pass null for all.
   */
  PaginatedResultJobWork getAllGlobal(JobWorkType type, GetAllQuery<Void> query);
}
