package com.erp.service;

import com.erp.wrappers.CreateResult;

public interface CoreServiceV2<P, R, S, I> {
  S getById(P entityId, I id);

  CreateResult<S> save(P entityId, R request);

  S update(P entityId, I id, R request);

  void deleteById(P entityId, I id);
}
