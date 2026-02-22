package com.erp.service;

import com.erp.wrappers.CreateResult;

public interface CoreServiceV1<R, S, I> {
  S getById(I id);

  CreateResult<S> save(R request);

  S update(I id, R request);

  void deleteById(I id);
}
