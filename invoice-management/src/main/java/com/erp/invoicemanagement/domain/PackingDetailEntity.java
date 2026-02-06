package com.erp.invoicemanagement.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "invoice_packing_details")
@Data
public class PackingDetailEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String itemNo;

  @Column(columnDefinition = "TEXT")
  private String description;

  private Integer totalQty;
  private Integer qtyPerCarton;
  private Integer noOfCartons;
  private Double grossWeightKg;
  private Double netWeightKg;
  private Integer woodenPallets;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  private InvoiceEntity invoice;
}
