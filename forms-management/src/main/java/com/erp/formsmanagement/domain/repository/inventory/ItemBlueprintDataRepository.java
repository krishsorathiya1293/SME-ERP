package com.erp.formsmanagement.domain.repository.inventory;

import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.repository.CoreRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemBlueprintDataRepository extends CoreRepository<ItemBlueprintDataEntity, Long> {
  Optional<ItemBlueprintDataEntity> findByItem_IdAndSizeInInchAndSizeInMm(
      Long itemId, String sizeInInch, String sizeInMm);

  Optional<ItemBlueprintDataEntity> findFirstBySizeInInchAndSizeInMm(
      String sizeInInch, String sizeInMm);

  Optional<ItemBlueprintDataEntity> findFirstByItem_ItemNameAndSizeInInchAndSizeInMm(
      String itemName, String sizeInInch, String sizeInMm);

  /**
   * Matches a blueprint size by item name + inch + mm, treating a NULL and an empty-string size the
   * same. The blueprint import stores {@code sizeInMm = NULL} for items that have no mm size (e.g.
   * "Super Marble", "Crank"), but callers pass "" for a blank mm. A plain {@code = ''} comparison
   * never matches NULL in SQL, so those items were silently skipped — this COALESCEs both sides so
   * an mm-less size still resolves. Returns a list (ordered) so a shared inch/mm across items can be
   * de-duped by the caller without a NonUniqueResultException.
   */
  @Query(
      "SELECT b FROM ItemBlueprintDataEntity b "
          + "WHERE b.item.itemName = :itemName "
          + "AND COALESCE(b.sizeInInch, '') = COALESCE(:sizeInInch, '') "
          + "AND COALESCE(b.sizeInMm, '') = COALESCE(:sizeInMm, '') "
          + "ORDER BY b.id")
  List<ItemBlueprintDataEntity> findByItemNameAndSizesNullSafe(
      @Param("itemName") String itemName,
      @Param("sizeInInch") String sizeInInch,
      @Param("sizeInMm") String sizeInMm);
}
