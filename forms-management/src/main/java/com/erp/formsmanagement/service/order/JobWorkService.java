package com.erp.formsmanagement.service.order;

import com.erp.api.ordermanagement.model.JobWork;
import com.erp.api.ordermanagement.model.NewJobWork;
import com.erp.api.ordermanagement.model.PaginatedResultJobWork;
import com.erp.api.ordermanagement.model.UpdateJobWorkStatus;
import com.erp.api.ordermanagement.model.UpdateJobWorkType;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;

public interface JobWorkService
    extends CoreServiceV2<Long, NewJobWork, JobWork, Long>,
        GetAllServiceV2<Long, Void, PaginatedResultJobWork> { 

  JobWork updateStatus(Long orderItemId, Long id, UpdateJobWorkStatus request);

  JobWork updateType(Long orderItemId, Long id, UpdateJobWorkType request);
}
