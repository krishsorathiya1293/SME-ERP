package com.erp.service;

import com.erp.mapper.EntityMapper;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractSpecificationServiceV1<E, R, S>
    extends AbstractCrudServiceV1<E, R, S> {

  private final JpaRepository<E, Long> repo;
  private final EntityMapper<E, R, S> mapperInstance;

  protected AbstractSpecificationServiceV1(
      JpaRepository<E, Long> repo, EntityMapper<E, R, S> mapper) {
    this.repo = repo;
    this.mapperInstance = mapper;
  }

  @Override
  protected final JpaRepository<E, Long> repository() {
    return repo;
  }

  @Override
  protected final EntityMapper<E, R, S> mapper() {
    return mapperInstance;
  }
}
