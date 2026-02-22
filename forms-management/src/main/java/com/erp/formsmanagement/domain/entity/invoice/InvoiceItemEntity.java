package com.erp.formsmanagement.domain.entity.invoice;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "invoice_items")
@Data
public class InvoiceItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String itemNo;
  private String partNo;
  private String description;
  private String hsCode;
  private Double unitPriceUsd;
  private Double currencyCurrentPrice;
  private Double inrPrice;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Enumerated(EnumType.STRING)
  private ItemCurrency currency;

  private Integer totalQuantity;
  private Integer qtyPerCarton;
  private Integer noOfCartons;
  private Double grossWeightKg;
  private Double netWeightKg;
  private Integer woodenPallets;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  private InvoiceEntity invoice;
}
