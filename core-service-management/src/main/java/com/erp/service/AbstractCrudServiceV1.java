package com.erp.service;

import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.mapper.EntityMapper;
import com.erp.wrappers.CreateOne;
import com.erp.wrappers.CreateResult;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractCrudServiceV1<E, R, S> implements CoreServiceV1<R, S, Long> {

    protected abstract JpaRepository<E, Long> repository();

    protected abstract EntityMapper<E, R, S> mapper();

    protected void afterCreate(E entity, R request) {
    }

    protected void afterUpdate(E entity, R request) {
    }

    @Override
    public S getById(Long id) {
        return repository()
                .findById(id)
                .map(mapper()::toDomain)
                .orElseThrow(() -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
    }

    @Override
    public CreateResult<S> save(R request) {
        E entity = mapper().toEntity(request);
        afterCreate(entity, request);
        return new CreateOne<>(mapper().toDomain(repository().save(entity)));
    }

    @Override
    public S update(Long id, R request) {
        E entity = repository()
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
        mapper().updateEntity(entity, request);
        afterUpdate(entity, request);
        return mapper().toDomain(repository().save(entity));
    }

    @Override
    public void deleteById(Long id) {
        repository().deleteById(id);
    }
}
