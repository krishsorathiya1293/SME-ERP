package com.erp.formsmanagement.service.master.impl;

import com.erp.api.mastermanagement.model.Item;
import com.erp.api.mastermanagement.model.NewItem;
import com.erp.api.mastermanagement.model.PaginatedResultItem;
import com.erp.formsmanagement.domain.entity.master.ItemEntity;
import com.erp.formsmanagement.domain.repository.master.ItemRepository;
import com.erp.formsmanagement.mapper.master.CategoryMapper;
import com.erp.formsmanagement.mapper.master.ItemMapper;
import com.erp.formsmanagement.mapper.master.SubCategoryMapper;
import com.erp.formsmanagement.service.master.CategoryService;
import com.erp.formsmanagement.service.master.ItemService;
import com.erp.formsmanagement.service.master.SubCategoryService;
import com.erp.service.AbstractSpecificationServiceV1;
import com.erp.util.GetAllQuery;
import com.erp.util.PageMapper;
import com.erp.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ItemServiceImpl
    extends AbstractSpecificationServiceV1<ItemEntity, NewItem, Item>
    implements ItemService {

  private final ItemRepository itemRepository;
  private final CategoryService categoryService;
  private final SubCategoryService subCategoryService;
  private final CategoryMapper categoryMapper;
  private final SubCategoryMapper subCategoryMapper;

  public ItemServiceImpl(
      ItemRepository itemRepository,
      ItemMapper itemMapper,
      CategoryService categoryService,
      SubCategoryService subCategoryService,
      CategoryMapper categoryMapper,
      SubCategoryMapper subCategoryMapper) {
    super(itemRepository, itemMapper);
    this.itemRepository = itemRepository;
    this.categoryService = categoryService;
    this.subCategoryService = subCategoryService;
    this.categoryMapper = categoryMapper;
    this.subCategoryMapper = subCategoryMapper;
  }

  @Override
  protected void afterCreate(ItemEntity entity, NewItem request) {
    entity.setCategory(categoryMapper.toEntity(categoryService.getById(request.getCategoryId())));
    entity.setSubCategory(
        subCategoryMapper.toEntity(
            subCategoryService.getById(request.getCategoryId(), request.getSubCategoryId())));
  }

  @Override
  protected void afterUpdate(ItemEntity entity, NewItem request) {
    afterCreate(entity, request);
  }

  @Override
  public PaginatedResultItem getAll(GetAllQuery<String> query) {
    Specification<ItemEntity> spec =
        Specification.where(itemRepository.filterByStatus(query.filter()))
            .and(itemRepository.filterBySearch(query.search()));
    Page<ItemEntity> results =
        itemRepository.findAll(
            spec,
            PaginationUtils.getPageRequest(
                query.page(), query.size(), query.direction(), query.sortBy()));
    return PageMapper.toResult(
        results, mapper()::toDomain, PaginatedResultItem::new, PaginatedResultItem::setData);
  }
}
