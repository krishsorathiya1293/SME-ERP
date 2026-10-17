package com.erp.formsmanagement.service.attendance;

import com.erp.api.attendancemanagement.model.AttendanceDay;
import com.erp.api.attendancemanagement.model.AttendanceDayStatus;
import com.erp.api.attendancemanagement.model.MonthlyAttendance;
import com.erp.api.attendancemanagement.model.MonthlyAttendanceRow;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * The monthly register as the office keeps it on paper: one row per worker, one column per day,
 * P / MP / A / WO in colour, totals at the end. A second sheet lists every day's in/out time for
 * whoever does the wages.
 */
@Component
public class AttendanceExcelExporter {

  private static final int FIXED_COLS = 3; // code, name, department

  public byte[] export(MonthlyAttendance register) {
    YearMonth ym = YearMonth.parse(register.getMonth());
    int days = register.getDaysInMonth();

    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      Styles st = new Styles(wb);
      writeRegister(wb.createSheet("Register " + register.getMonth()), register, ym, days, st);
      writeTimes(wb.createSheet("In-Out times"), register, st);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to generate attendance register", e);
    }
  }

  private void writeRegister(
      Sheet sheet, MonthlyAttendance register, YearMonth ym, int days, Styles st) {
    String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    Row title = sheet.createRow(0);
    cell(title, 0, "Attendance register - " + monthName + " " + ym.getYear(), st.title);
    sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, FIXED_COLS + days + 3));

    Row h1 = sheet.createRow(2);
    Row h2 = sheet.createRow(3);
    String[] fixed = {"Emp code", "Name", "Department"};
    for (int i = 0; i < fixed.length; i++) {
      cell(h1, i, fixed[i], st.header);
      cell(h2, i, "", st.header);
      sheet.addMergedRegion(new CellRangeAddress(2, 3, i, i));
    }
    for (int d = 1; d <= days; d++) {
      cell(h1, FIXED_COLS + d - 1, String.valueOf(d), st.header);
      String dow = ym.atDay(d).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
      cell(h2, FIXED_COLS + d - 1, dow.substring(0, 2), st.header);
    }
    String[] totals = {"Present", "Missed punch", "Absent", "Hours"};
    for (int i = 0; i < totals.length; i++) {
      int c = FIXED_COLS + days + i;
      cell(h1, c, totals[i], st.header);
      cell(h2, c, "", st.header);
      sheet.addMergedRegion(new CellRangeAddress(2, 3, c, c));
    }

    int r = 4;
    for (MonthlyAttendanceRow row : register.getRows()) {
      Row x = sheet.createRow(r++);
      cell(x, 0, row.getEmpCode(), st.text);
      cell(x, 1, row.getEmployeeName() == null ? "" : row.getEmployeeName(), st.text);
      cell(x, 2, row.getDepartment() == null ? "" : row.getDepartment(), st.text);
      for (int i = 0; i < row.getDays().size(); i++) {
        AttendanceDayStatus s = row.getDays().get(i).getStatus();
        cell(x, FIXED_COLS + i, code(s), st.status.get(s));
      }
      int c = FIXED_COLS + days;
      num(x, c, row.getPresentDays(), st.number);
      num(x, c + 1, row.getMissedPunchDays(), st.number);
      num(x, c + 2, row.getAbsentDays(), st.number);
      cell(x, c + 3, hours(row.getTotalWorkedMinutes()), st.number);
    }

    Row legend = sheet.createRow(r + 1);
    cell(legend, 0, "P = present (in + out)   IN = at work today, not out yet   "
        + "MP = missed punch (only one punch)   A = absent   WO = weekly off", st.text);

    sheet.setColumnWidth(0, 10 * 256);
    sheet.setColumnWidth(1, 24 * 256);
    sheet.setColumnWidth(2, 14 * 256);
    for (int d = 0; d < days; d++) sheet.setColumnWidth(FIXED_COLS + d, 4 * 256);
    for (int i = 0; i < 4; i++) sheet.setColumnWidth(FIXED_COLS + days + i, 12 * 256);
    sheet.createFreezePane(FIXED_COLS, 4);
  }

  private void writeTimes(Sheet sheet, MonthlyAttendance register, Styles st) {
    String[] head = {"Emp code", "Name", "Date", "Day", "In", "Out", "Hours", "Punches", "Status"};
    Row h = sheet.createRow(0);
    for (int i = 0; i < head.length; i++) cell(h, i, head[i], st.header);
    int r = 1;
    for (MonthlyAttendanceRow row : register.getRows()) {
      for (AttendanceDay d : row.getDays()) {
        if (d.getPunchCount() == null || d.getPunchCount() == 0) continue;
        Row x = sheet.createRow(r++);
        cell(x, 0, row.getEmpCode(), st.text);
        cell(x, 1, row.getEmployeeName() == null ? "" : row.getEmployeeName(), st.text);
        cell(x, 2, d.getDate().toString(), st.text);
        cell(x, 3, d.getDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), st.text);
        cell(x, 4, hhmm(d.getFirstIn()), st.text);
        cell(x, 5, hhmm(d.getLastOut()), st.text);
        cell(x, 6, hours(d.getWorkedMinutes()), st.number);
        num(x, 7, d.getPunchCount(), st.number);
        cell(x, 8, code(d.getStatus()), st.status.get(d.getStatus()));
      }
    }
    int[] widths = {10, 24, 12, 6, 8, 8, 8, 9, 8};
    for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
    sheet.createFreezePane(0, 1);
  }

  static String code(AttendanceDayStatus s) {
    return switch (s) {
      case PRESENT -> "P";
      case AT_WORK -> "IN";
      case MISSED_PUNCH -> "MP";
      case ABSENT -> "A";
      case WEEKLY_OFF -> "WO";
      case UPCOMING, NOT_JOINED -> "";
    };
  }

  /** 545 minutes → "9:05". */
  static String hours(Integer minutes) {
    int m = minutes == null ? 0 : minutes;
    return (m / 60) + ":" + String.format("%02d", m % 60);
  }

  private static String hhmm(String time) {
    return time == null ? "" : time.length() >= 5 ? time.substring(0, 5) : time;
  }

  private static void cell(Row row, int col, String value, CellStyle style) {
    Cell c = row.createCell(col);
    c.setCellValue(value);
    c.setCellStyle(style);
  }

  private static void num(Row row, int col, Integer value, CellStyle style) {
    Cell c = row.createCell(col);
    c.setCellValue(value == null ? 0 : value);
    c.setCellStyle(style);
  }

  private static final class Styles {
    final CellStyle title;
    final CellStyle header;
    final CellStyle text;
    final CellStyle number;
    final Map<AttendanceDayStatus, CellStyle> status = new EnumMap<>(AttendanceDayStatus.class);

    Styles(XSSFWorkbook wb) {
      Font bold = wb.createFont();
      bold.setBold(true);
      Font big = wb.createFont();
      big.setBold(true);
      big.setFontHeightInPoints((short) 14);

      title = wb.createCellStyle();
      title.setFont(big);

      header = bordered(wb);
      header.setFont(bold);
      header.setAlignment(HorizontalAlignment.CENTER);
      header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      text = bordered(wb);
      number = bordered(wb);
      number.setAlignment(HorizontalAlignment.CENTER);

      status.put(AttendanceDayStatus.PRESENT, filled(wb, IndexedColors.LIGHT_GREEN));
      status.put(AttendanceDayStatus.AT_WORK, filled(wb, IndexedColors.LIGHT_TURQUOISE));
      status.put(AttendanceDayStatus.MISSED_PUNCH, filled(wb, IndexedColors.LIGHT_ORANGE));
      status.put(AttendanceDayStatus.ABSENT, filled(wb, IndexedColors.CORAL));
      status.put(AttendanceDayStatus.WEEKLY_OFF, filled(wb, IndexedColors.GREY_25_PERCENT));
      status.put(AttendanceDayStatus.UPCOMING, number);
      status.put(AttendanceDayStatus.NOT_JOINED, number);
    }

    private static CellStyle bordered(XSSFWorkbook wb) {
      CellStyle s = wb.createCellStyle();
      s.setBorderTop(BorderStyle.THIN);
      s.setBorderBottom(BorderStyle.THIN);
      s.setBorderLeft(BorderStyle.THIN);
      s.setBorderRight(BorderStyle.THIN);
      return s;
    }

    private static CellStyle filled(XSSFWorkbook wb, IndexedColors color) {
      CellStyle s = bordered(wb);
      s.setAlignment(HorizontalAlignment.CENTER);
      s.setFillForegroundColor(color.getIndex());
      s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      return s;
    }
  }
}
