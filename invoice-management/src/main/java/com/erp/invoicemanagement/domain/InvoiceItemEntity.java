package com.erp.invoicemanagement.domain;

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

  @Column(columnDefinition = "TEXT")
  private String description;

  private String hsCode;
  private Integer quantity;
  private Double unitPriceUsd;
  private Double currencyCurrentPrice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  private InvoiceEntity invoice;
}
