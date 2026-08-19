package com.erp.formsmanagement.service.order;

import com.erp.api.ordermanagement.model.JobWork;
import com.erp.api.ordermanagement.model.NewJobWork;
import com.erp.api.ordermanagement.model.PaginatedResultJobWork;
import com.erp.api.ordermanagement.model.UpdateJobWorkBajaar;
import com.erp.api.ordermanagement.model.UpdateJobWorkStatus;
import com.erp.api.ordermanagement.model.UpdateJobWorkType;
import com.erp.formsmanagement.domain.entity.order.JobWorkReturnState;
import com.erp.formsmanagement.domain.entity.order.JobWorkType;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;
import com.erp.util.GetAllQuery;

public interface JobWorkService
    extends CoreServiceV2<Long, NewJobWork, JobWork, Long>,
        GetAllServiceV2<Long, Void, PaginatedResultJobWork> { 

  JobWork updateStatus(Long orderItemId, Long id, UpdateJobWorkStatus request);

  JobWork updateType(Long orderItemId, Long id, UpdateJobWorkType request);

  /**
   * Sets which market rate a job work is priced against. Only ROJNU carries an amount — for FIXED
   * the amount belongs to the {@code jobwork.fixed.bajaar} setting, so anything sent with it is
   * discarded rather than frozen onto the row.
   */
  JobWork updateBajaar(Long id, UpdateJobWorkBajaar request);

  /** Create a job work that is NOT tied to any order item (Manual mode). */
  JobWork createManual(NewJobWork request);

  /** Update a job work that is NOT tied to any order item (Manual mode). */
  JobWork updateManual(Long id, NewJobWork request);

  /**
   * Global listing across every order item, for the dashboard / plating pages.
   * {@code type} filters by JobWorkType; pass null for all.
   */
  PaginatedResultJobWork getAllGlobal(
      JobWorkType type, JobWorkReturnState returnState, GetAllQuery<Void> query);

  /**
   * Global stat-card counts (total / pending / partially returned / fully returned) over the whole
   * dataset, honouring the same {@code type} + {@code search} filters as {@link #getAllGlobal} so
   * the cards stay correct while the list itself is paginated. Pass null/blank for no filter.
   *
   * <p>The return-state filter is deliberately *not* applied here: the cards are how the user
   * picks a state, so they have to keep showing the counts for the other two.
   */
  JobWorkStats getGlobalStats(JobWorkType type, String search);
}
