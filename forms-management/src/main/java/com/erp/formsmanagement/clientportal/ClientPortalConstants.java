package com.erp.formsmanagement.clientportal;

import java.util.List;

/** Shared constants for the Client Portal "Shop" feature. */
public final class ClientPortalConstants {

  private ClientPortalConstants() {}

  /**
   * Plating/finish options offered to clients in the catalog. There is currently no
   * per-item plating master in the database, so the same list is offered for every item
   * (mirrors the frontend's mock catalog finish list).
   */
  public static final List<String> FINISH_OPTIONS =
      List.of(
          "S.S & Sartin Lacq",
          "ANTQ",
          "Side Gold",
          "Z-Black.",
          "Gr. Black.",
          "Matt S.S",
          "Matt ANTQ",
          "PVD Rose",
          "PVD Gold",
          "PVD Black",
          "Rose Gold",
          "Clear Lacq.");
}
