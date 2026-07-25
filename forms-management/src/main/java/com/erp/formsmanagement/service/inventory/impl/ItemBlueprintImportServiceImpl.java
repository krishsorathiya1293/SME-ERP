package com.erp.formsmanagement.service.inventory.impl;

import com.erp.api.itemmanagement.model.ImportResult;
import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintEntity;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintRepository;
import com.erp.formsmanagement.service.inventory.ItemBlueprintImportService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ItemBlueprintImportServiceImpl implements ItemBlueprintImportService {

  private static final String SHEET_NAME = "All";

  private static final int COL_ITEM_NAME = 0;
  private static final int COL_SIZE_IN_INCH = 1;
  private static final int COL_SIZE_IN_MM = 2;
  private static final int COL_DOZEN_WEIGHT = 3;
  private static final int COL_PCS_WEIGHT = 4;
  private static final int COL_PCS_PER_BOX = 5;
  private static final int COL_BOX_PER_CARTON = 6;
  private static final int COL_PCS_PER_CARTON = 7;
  private static final int COL_CARTON_WEIGHT = 8;

  private final ItemBlueprintRepository itemBlueprintRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;

  public ItemBlueprintImportServiceImpl(
      ItemBlueprintRepository itemBlueprintRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository) {
    this.itemBlueprintRepository = itemBlueprintRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
  }

  private record StockRow(
      String itemName,
      String sizeInInch,
      String sizeInMm,
      Double dozenWeight,
      Double pcsWeight,
      Integer pcsPerBox,
      Integer boxPerCarton,
      Integer pcsPerCarton,
      Double cartonWeight) {}

  @Override
  public ImportResult importStockMaster(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Uploaded file is empty");
    }

    List<StockRow> rows = parse(file);
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("No item rows found in uploaded file");
    }

    Map<String, List<StockRow>> grouped = new LinkedHashMap<>();
    for (StockRow row : rows) {
      grouped.computeIfAbsent(row.itemName(), k -> new ArrayList<>()).add(row);
    }

    int itemsCreated = 0;
    int sizesCreated = 0;

    for (Map.Entry<String, List<StockRow>> entry : grouped.entrySet()) {
      String itemName = entry.getKey();

      // Find existing item by name (case-insensitive) or create new
      ItemBlueprintEntity item = itemBlueprintRepository
          .findByItemNameIgnoreCase(itemName)
          .orElse(null);

      if (item == null) {
        item = new ItemBlueprintEntity();
        item.setItemName(itemName);
        item = itemBlueprintRepository.save(item);
        itemsCreated++;
      }

      final ItemBlueprintEntity savedItem = item;

      for (StockRow row : entry.getValue()) {
        String sizeInInch = row.sizeInInch() != null ? row.sizeInInch() : "";
        String sizeInMm = row.sizeInMm() != null ? row.sizeInMm() : "";

        boolean sizeExists = itemBlueprintDataRepository
            .findByItem_IdAndSizeInInchAndSizeInMm(savedItem.getId(), sizeInInch, sizeInMm)
            .isPresent();

        if (!sizeExists) {
          ItemBlueprintDataEntity size = new ItemBlueprintDataEntity();
          size.setItem(savedItem);
          size.setSizeInInch(row.sizeInInch());
          size.setSizeInMm(row.sizeInMm());
          size.setDozenWeight(row.dozenWeight());
          size.setPcsWeight(row.pcsWeight());

          InventoryEntity inventory = new InventoryEntity();
          inventory.setPcsPerBox(row.pcsPerBox());
          inventory.setBoxPerCarton(row.boxPerCarton());
          inventory.setPcsPerCarton(row.pcsPerCarton());
          inventory.setCartonWeight(row.cartonWeight());
          inventory.setSize(size);
          size.setInventory(inventory);

          itemBlueprintDataRepository.save(size);
          sizesCreated++;
        }
      }
    }

    ImportResult result = new ImportResult();
    result.setItemsCreated(itemsCreated);
    result.setSizesCreated(sizesCreated);
    result.setMessage(
        "Import complete: " + itemsCreated + " new items and " + sizesCreated
            + " new sizes added. Existing items and sizes were not modified.");
    return result;
  }

  private List<StockRow> parse(MultipartFile file) {
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheet(SHEET_NAME);
      if (sheet == null) {
        sheet = workbook.getSheetAt(0);
      }

      List<StockRow> rows = new ArrayList<>();
      String currentItemName = null;
      String currentSizeInInch = null;

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }

        String itemName = readTextCell(row.getCell(COL_ITEM_NAME));
        if (itemName != null) {
          currentItemName = itemName;
          currentSizeInInch = null;
        }
        if (currentItemName == null) {
          continue;
        }

        String sizeInInch = readTextCell(row.getCell(COL_SIZE_IN_INCH));
        if (sizeInInch != null) {
          currentSizeInInch = sizeInInch;
        } else {
          sizeInInch = currentSizeInInch;
        }
        String sizeInMm = readTextCell(row.getCell(COL_SIZE_IN_MM));

        if (sizeInInch == null && sizeInMm == null) {
          continue;
        }

        rows.add(
            new StockRow(
                currentItemName,
                sizeInInch,
                sizeInMm,
                readNumericCell(row.getCell(COL_DOZEN_WEIGHT)),
                readNumericCell(row.getCell(COL_PCS_WEIGHT)),
                readIntegerCell(row.getCell(COL_PCS_PER_BOX)),
                readIntegerCell(row.getCell(COL_BOX_PER_CARTON)),
                readIntegerCell(row.getCell(COL_PCS_PER_CARTON)),
                readNumericCell(row.getCell(COL_CARTON_WEIGHT))));
      }

      return rows;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read uploaded Excel file", e);
    }
  }

  private String readTextCell(Cell cell) {
    if (cell == null) return null;
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) {
      type = cell.getCachedFormulaResultType();
    }
    String value;
    if (type == CellType.NUMERIC) {
      value = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
    } else if (type == CellType.STRING) {
      value = cell.getStringCellValue();
    } else {
      return null;
    }
    value = value.trim().replaceAll("\\s+", " ");
    return value.isEmpty() ? null : value;
  }

  private Double readNumericCell(Cell cell) {
    if (cell == null) return null;
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) {
      type = cell.getCachedFormulaResultType();
    }
    if (type != CellType.NUMERIC) return null;
    // Excel formulas like dozenWeight/12 produce long repeating decimals (e.g.
    // 0.07916666666666666) that look broken wherever they're displayed downstream.
    return Math.round(cell.getNumericCellValue() * 1000) / 1000d;
  }

  private Integer readIntegerCell(Cell cell) {
    Double value = readNumericCell(cell);
    return value == null ? null : (int) Math.round(value);
  }
}
