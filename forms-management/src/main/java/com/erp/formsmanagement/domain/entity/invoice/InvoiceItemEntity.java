package com.erp.formsmanagement.domain.entity.invoice;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "invoice_items")
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof InvoiceItemEntity e))
      return false;
    return id != null && id.equals(e.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
