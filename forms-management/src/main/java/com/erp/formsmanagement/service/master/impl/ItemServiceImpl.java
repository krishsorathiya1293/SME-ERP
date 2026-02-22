package com.erp.formsmanagement.service.master.impl;

import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewItem;
import com.erp.api.mastermanagement.model.PaginatedResultItem;
import com.erp.constant.Constant;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.master.ItemEntity;
import com.erp.formsmanagement.domain.repository.master.ItemRepository;
import com.erp.formsmanagement.mapper.master.CategoryMapper;
import com.erp.formsmanagement.mapper.master.ItemMapper;
import com.erp.formsmanagement.mapper.master.SubCategoryMapper;
import com.erp.formsmanagement.service.master.CategoryService;
import com.erp.formsmanagement.service.master.ItemService;
import com.erp.formsmanagement.service.master.SubCategoryService;
import com.erp.util.PaginationUtils;
import com.erp.wrappers.CreateOne;
import com.erp.wrappers.CreateResult;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
  private final ItemRepository itemRepository;
  private final CategoryService categoryService;
  private final SubCategoryService subCategoryService;
  private final ItemMapper itemMapper;
  private final CategoryMapper categoryMapper;
  private final SubCategoryMapper subCategoryMapper;

  @Override
  public PaginatedResultItem getAll(
      Optional<String> filterByStatus,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortBy,
      Optional<String> direction) {

    Specification<ItemEntity> spec =
        Specification.where(itemRepository.filterByStatus(filterByStatus))
            .and(itemRepository.filterBySearch(search));
    Page<ItemEntity> results =
        itemRepository.findAll(spec, PaginationUtils.getPageRequest(page, size, direction, sortBy));
    List<Item> content = results.getContent().stream().map(itemMapper::toDomain).toList();
    return new PaginatedResultItem()
        .data(content)
        .empty(content.isEmpty())
        .first(results.isFirst())
        .last(results.isLast())
        .totalPages(results.getTotalPages())
        .size(content.size())
        .totalElements((int) results.getTotalElements());
  }

  @Override
  public Item getById(Long id) {
    return itemRepository
        .findById(id)
        .map(itemMapper::toDomain)
        .orElseThrow(
            () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
  }

  @Override
  public CreateResult<Item> save(NewItem request) {
    var entity = itemMapper.toEntity(request);
    entity.setCategory(categoryMapper.toEntity(categoryService.getById(request.getCategoryId())));
    entity.setSubCategory(
        subCategoryMapper.toEntity(
            subCategoryService.getById(request.getCategoryId(), request.getSubCategoryId())));
    return new CreateOne<>(itemMapper.toDomain(itemRepository.save(entity)));
  }

  @Override
  public Item update(Long id, NewItem request) {
    var entity =
        itemRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format(Constant.ENTITY_NOT_FOUND, id)));
    itemMapper.updateEntity(entity, request);
    entity.setCategory(categoryMapper.toEntity(categoryService.getById(request.getCategoryId())));
    entity.setSubCategory(
        subCategoryMapper.toEntity(
            subCategoryService.getById(request.getCategoryId(), request.getSubCategoryId())));

    return itemMapper.toDomain(itemRepository.save(entity));
  }

  @Override
  public void deleteById(Long id) {
    itemRepository.deleteById(id);
  }
}
