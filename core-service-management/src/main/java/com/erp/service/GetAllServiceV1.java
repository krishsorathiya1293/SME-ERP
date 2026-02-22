package com.erp.service;

import com.erp.util.GetAllQuery;

public interface GetAllServiceV1<F, S> {
  S getAll(GetAllQuery<F> query);
}
