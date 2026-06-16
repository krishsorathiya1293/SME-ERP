package com.erp.formsmanagement.service.client.impl;

import com.erp.api.clientmanagement.model.ClientInventoryImportResult;
import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.client.ClientInventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.InventoryEntity;
import com.erp.formsmanagement.domain.entity.inventory.ItemBlueprintDataEntity;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.repository.client.ClientInventoryRepository;
import com.erp.formsmanagement.domain.repository.inventory.ItemBlueprintDataRepository;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.formsmanagement.service.client.ClientInventoryImportService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
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
public class ClientInventoryImportServiceImpl implements ClientInventoryImportService {

  // Column indices (0-based), row 0 = header, data starts at row 1
  private static final int COL_SIZE_IN_INCH   = 1;
  private static final int COL_SIZE_IN_MM     = 2;
  // col 3 = Doz. Weight, col 4 = PCS Weight → skip (stock master owns these)
  private static final int COL_PCS_PER_BOX    = 5;
  private static final int COL_BOX_PER_CARTON = 6;
  private static final int COL_PCS_PER_CARTON = 7;
  private static final int COL_CARTON_WEIGHT  = 8;
  private static final int COL_SSSATINLACQ    = 9;   // S.S.  → the SS base price for this client
  private static final int COL_ANTIQ          = 10;
  private static final int COL_SIDEGOLD       = 11;
  private static final int COL_SARTINLACQ     = 12;  // Sartin Lacqur
  private static final int COL_ZBLACK         = 13;
  private static final int COL_GRBLACK        = 14;
  private static final int COL_MATTSS         = 15;
  private static final int COL_MATTANTIQ      = 16;
  private static final int COL_PVDROSE        = 17;
  private static final int COL_PVDGOLD        = 18;
  private static final int COL_PVDBLACK       = 19;
  private static final int COL_ROSEGOLD       = 20;
  private static final int COL_CLEARLACQ      = 21;

  private final ClientInventoryRepository clientInventoryRepository;
  private final ItemBlueprintDataRepository itemBlueprintDataRepository;
  private final PartyRepository partyRepository;

  public ClientInventoryImportServiceImpl(
      ClientInventoryRepository clientInventoryRepository,
      ItemBlueprintDataRepository itemBlueprintDataRepository,
      PartyRepository partyRepository) {
    this.clientInventoryRepository = clientInventoryRepository;
    this.itemBlueprintDataRepository = itemBlueprintDataRepository;
    this.partyRepository = partyRepository;
  }

  @Override
  public ClientInventoryImportResult importClientInventory(Long clientId, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Uploaded file is empty");
    }

    PartyEntity party = partyRepository.findById(clientId)
        .orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + clientId));

    int rowsImported = 0;
    int rowsSkipped = 0;

    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        String sizeInInch = readTextCell(row.getCell(COL_SIZE_IN_INCH));
        String sizeInMm   = readTextCell(row.getCell(COL_SIZE_IN_MM));

        if (sizeInInch == null && sizeInMm == null) continue;

        String inch = sizeInInch != null ? sizeInInch : "";
        String mm   = sizeInMm   != null ? sizeInMm   : "";

        ItemBlueprintDataEntity size = itemBlueprintDataRepository
            .findFirstBySizeInInchAndSizeInMm(inch, mm)
            .orElse(null);

        if (size == null) {
          rowsSkipped++;
          continue;
        }

        // Base SS price: prefer the client Excel value; fall back to stock master inventory
        InventoryEntity baseInv = size.getInventory();
        Double excelSs = readNumericCell(row.getCell(COL_SSSATINLACQ));
        Double ssBase = excelSs != null ? excelSs
            : (baseInv != null ? baseInv.getSs() : null);

        ClientInventoryEntity entity = clientInventoryRepository
            .findByParty_IdAndSize_Id(clientId, size.getId())
            .orElseGet(ClientInventoryEntity::new);

        entity.setParty(party);
        entity.setSize(size);

        // Packing fields — always numeric in Excel
        apply(entity, "pcsPerBox",    readIntegerCell(row.getCell(COL_PCS_PER_BOX)));
        apply(entity, "boxPerCarton", readIntegerCell(row.getCell(COL_BOX_PER_CARTON)));
        apply(entity, "pcsPerCarton", readIntegerCell(row.getCell(COL_PCS_PER_CARTON)));
        apply(entity, "cartonWeight", readNumericCell(row.getCell(COL_CARTON_WEIGHT)));

        // Pricing fields — may be numeric or "S.S.+XX" expressions
        apply(entity, "sssatinlacq", resolve(row.getCell(COL_SSSATINLACQ), ssBase));
        apply(entity, "antiq",       resolve(row.getCell(COL_ANTIQ),       ssBase));
        apply(entity, "sidegold",    resolve(row.getCell(COL_SIDEGOLD),    ssBase));
        apply(entity, "sartinlacq",  resolve(row.getCell(COL_SARTINLACQ),  ssBase));
        apply(entity, "zblack",      resolve(row.getCell(COL_ZBLACK),      ssBase));
        apply(entity, "grblack",     resolve(row.getCell(COL_GRBLACK),     ssBase));
        apply(entity, "mattss",      resolve(row.getCell(COL_MATTSS),      ssBase));
        apply(entity, "mattantiq",   resolve(row.getCell(COL_MATTANTIQ),   ssBase));
        apply(entity, "pvdrose",     resolve(row.getCell(COL_PVDROSE),     ssBase));
        apply(entity, "pvdgold",     resolve(row.getCell(COL_PVDGOLD),     ssBase));
        apply(entity, "pvdblack",    resolve(row.getCell(COL_PVDBLACK),    ssBase));
        apply(entity, "rosegold",    resolve(row.getCell(COL_ROSEGOLD),    ssBase));
        apply(entity, "clearlacq",   resolve(row.getCell(COL_CLEARLACQ),   ssBase));

        clientInventoryRepository.save(entity);
        rowsImported++;
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read uploaded Excel file", e);
    }

    ClientInventoryImportResult result = new ClientInventoryImportResult();
    result.setRowsImported(rowsImported);
    result.setRowsSkipped(rowsSkipped);
    result.setMessage("Import complete: " + rowsImported + " rows imported, "
        + rowsSkipped + " rows skipped (size not found in stock master).");
    return result;
  }

  /**
   * Resolves a cell value that may be a plain number or a "S.S.+XX" / "S.S." expression.
   * Returns null if the cell is blank or the expression cannot be evaluated (ssBase is null).
   */
  private Double resolve(Cell cell, Double ssBase) {
    if (cell == null) return null;
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) type = cell.getCachedFormulaResultType();

    if (type == CellType.NUMERIC) {
      return cell.getNumericCellValue();
    }

    if (type == CellType.STRING) {
      String text = cell.getStringCellValue().trim();
      if (text.isEmpty()) return null;

      String upper = text.toUpperCase().replaceAll("\\s+", "");

      // "S.S." or "SS" → exactly the SS base price
      if (upper.equals("S.S.") || upper.equals("SS")) {
        return ssBase;
      }

      // "S.S.+XX" or "SS+XX" → SS + offset
      String stripped = upper.replace("S.S.", "").replace("SS", "");
      if (stripped.startsWith("+") || stripped.startsWith("-")) {
        try {
          double offset = Double.parseDouble(stripped);
          return ssBase != null ? ssBase + offset : null;
        } catch (NumberFormatException ignored) {
        }
      }

      // Plain number stored as string
      try {
        return Double.parseDouble(text);
      } catch (NumberFormatException ignored) {
      }
    }

    return null;
  }

  private void apply(ClientInventoryEntity entity, String field, Object value) {
    if (value == null) return;
    switch (field) {
      case "pcsPerBox"    -> entity.setPcsPerBox((Integer) value);
      case "boxPerCarton" -> entity.setBoxPerCarton((Integer) value);
      case "pcsPerCarton" -> entity.setPcsPerCarton((Integer) value);
      case "cartonWeight" -> entity.setCartonWeight((Double) value);
      case "sssatinlacq"  -> entity.setSssatinlacq((Double) value);
      case "antiq"        -> entity.setAntiq((Double) value);
      case "sidegold"     -> entity.setSidegold((Double) value);
      case "sartinlacq"   -> entity.setSartinlacq((Double) value);
      case "zblack"       -> entity.setZblack((Double) value);
      case "grblack"      -> entity.setGrblack((Double) value);
      case "mattss"       -> entity.setMattss((Double) value);
      case "mattantiq"    -> entity.setMattantiq((Double) value);
      case "pvdrose"      -> entity.setPvdrose((Double) value);
      case "pvdgold"      -> entity.setPvdgold((Double) value);
      case "pvdblack"     -> entity.setPvdblack((Double) value);
      case "rosegold"     -> entity.setRosegold((Double) value);
      case "clearlacq"    -> entity.setClearlacq((Double) value);
    }
  }

  private String readTextCell(Cell cell) {
    if (cell == null) return null;
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) type = cell.getCachedFormulaResultType();
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
    if (type == CellType.FORMULA) type = cell.getCachedFormulaResultType();
    if (type != CellType.NUMERIC) return null;
    return cell.getNumericCellValue();
  }

  private Integer readIntegerCell(Cell cell) {
    Double value = readNumericCell(cell);
    return value == null ? null : (int) Math.round(value);
  }
}
