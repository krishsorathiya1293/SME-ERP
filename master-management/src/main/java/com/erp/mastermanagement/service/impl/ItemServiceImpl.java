package com.erp.mastermanagement.service.impl;

import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewItem;
import com.erp.api.mastermanagement.model.PaginatedResultItem;
import com.erp.exception.EntityNotFoundException;
import com.erp.mastermanagement.domain.ItemEntity;
import com.erp.mastermanagement.mapper.ItemMapper;
import com.erp.mastermanagement.repository.CategoryRepository;
import com.erp.mastermanagement.repository.ItemRepository;
import com.erp.mastermanagement.repository.SubCategoryRepository;
import com.erp.mastermanagement.service.ItemService;
import com.erp.util.PaginationUtils;
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
  private final CategoryRepository categoryRepository;
  private final SubCategoryRepository subCategoryRepository;
  private final ItemMapper itemMapper;

  @Override
  public PaginatedResultItem getAll(
      Optional<String> filterByStatus,
      Optional<String> search,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sortBy,
      Optional<String> direction) {

    // 1. Build Specification (same pattern as Order)
    Specification<ItemEntity> spec =
        Specification.where(itemRepository.filterByStatus(filterByStatus))
            .and(itemRepository.filterBySearch(search));

    // 2. Fetch paginated result
    Page<ItemEntity> results =
        itemRepository.findAll(spec, PaginationUtils.getPageRequest(page, size, direction, sortBy));

    // 3. Map content
    List<Item> content = results.getContent().stream().map(itemMapper::toDomain).toList();

    // 4. Build response (same structure as Order)
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
        .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + id));
  }

  @Override
  public Item save(NewItem request) {
    var category =
        categoryRepository
            .findById(request.getCategoryId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Category not found with id: " + request.getCategoryId()));
    var subCategory =
        subCategoryRepository
            .findById(request.getSubCategoryId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "SubCategory not found with id: " + request.getSubCategoryId()));

    var entity = itemMapper.toEntity(request);
    entity.setCategory(category);
    entity.setSubCategory(subCategory);

    return itemMapper.toDomain(itemRepository.save(entity));
  }

  @Override
  public Item update(Long id, NewItem request) {
    var entity =
        itemRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + id));

    var category =
        categoryRepository
            .findById(request.getCategoryId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Category not found with id: " + request.getCategoryId()));
    var subCategory =
        subCategoryRepository
            .findById(request.getSubCategoryId())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "SubCategory not found with id: " + request.getSubCategoryId()));

    itemMapper.updateEntity(entity, request);
    entity.setCategory(category);
    entity.setSubCategory(subCategory);

    return itemMapper.toDomain(itemRepository.save(entity));
  }

  @Override
  public void deleteById(Long id) {
    itemRepository.deleteById(id);
  }
}
