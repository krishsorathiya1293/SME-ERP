package com.erp.mapper;

public interface EntityMapper<E, R, S> {
    S toDomain(E entity);

    E toEntity(R request);

    void updateEntity(E entity, R request);
}
