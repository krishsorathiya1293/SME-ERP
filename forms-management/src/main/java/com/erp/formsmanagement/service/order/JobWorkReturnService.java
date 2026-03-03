package com.erp.formsmanagement.service.order;

import com.erp.api.ordermanagement.model.JobWorkReturn;
import com.erp.api.ordermanagement.model.NewJobWorkReturn;
import com.erp.api.ordermanagement.model.PaginatedResultJobWorkReturn;
import com.erp.service.CoreServiceV2;
import com.erp.service.GetAllServiceV2;
import com.erp.util.GetAllQuery;

public interface JobWorkReturnService
    extends CoreServiceV2<Long, NewJobWorkReturn, JobWorkReturn, Long>,
        GetAllServiceV2<Long, Void, PaginatedResultJobWorkReturn> {

  @Override
  PaginatedResultJobWorkReturn getAll(Long jobWorkId, GetAllQuery<Void> query);
}
