package com.erp.service;

import com.erp.util.GetAllQuery;

public interface GetAllServiceV2<P, F, S> {
  S getAll(P parentId, GetAllQuery<F> query);
}
