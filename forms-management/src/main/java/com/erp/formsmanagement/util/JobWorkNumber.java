package com.erp.formsmanagement.util;

/**
 * Builds the party-wise job work number shown on the app list and the printed chitthi, e.g.
 * {@code AZ-1} (Azzad) or {@code ZP-2} (Ziya Process). The party code is derived from the party
 * name: multi-word names use each of the first two words' initials, a single-word name uses its
 * first two letters. All uppercase.
 */
public final class JobWorkNumber {

  private JobWorkNumber() {}

  /** Party code prefix derived from the party name (e.g. "Ziya Process" -> "ZP", "Azzad" -> "AZ"). */
  public static String partyCode(String partyName) {
    if (partyName == null) {
      return "XX";
    }
    String[] words = partyName.trim().split("\\s+");
    StringBuilder code = new StringBuilder();
    if (words.length >= 2) {
      for (String w : words) {
        if (!w.isEmpty()) {
          code.append(Character.toUpperCase(w.charAt(0)));
          if (code.length() == 2) {
            break;
          }
        }
      }
    } else if (words.length == 1 && !words[0].isEmpty()) {
      String w = words[0];
      code.append(Character.toUpperCase(w.charAt(0)));
      if (w.length() > 1) {
        code.append(Character.toUpperCase(w.charAt(1)));
      }
    }
    return code.length() == 0 ? "XX" : code.toString();
  }

  /**
   * Full label "{@code <CODE>-<N>}" for a job work, or {@code null} when no number has been assigned
   * yet.
   */
  public static String label(String partyName, Integer jobWorkNo) {
    if (jobWorkNo == null) {
      return null;
    }
    return partyCode(partyName) + "-" + jobWorkNo;
  }
}
