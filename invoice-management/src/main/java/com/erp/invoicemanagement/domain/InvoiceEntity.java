package com.erp.invoicemanagement.domain;

import com.erp.audit.AuditInfo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@Table(name = "invoices")
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(callSuper = true)
public class InvoiceEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Exporter
  private String exporterCompanyName;
  private String exporterContactNo;

  @Column(columnDefinition = "TEXT")
  private String exporterAddress;

  private String currency;
  // Importer (Bill To)
  private String billToCountry;
  private String billToName;
  private String billToContactNo;

  @Column(columnDefinition = "TEXT")
  private String billToAddress;

  // Importer (Ship To)
  private String shipToCountry;
  private String shipToName;
  private String shipToContactNo;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Enumerated(EnumType.STRING)
  private InvoiceType invoiceType;

  @Column(columnDefinition = "TEXT")
  private String shipToAddress;

  // Invoice Details
  private String invoiceNo;
  private LocalDate invoiceDate;
  private String gstNo;
  private String iecCode;
  private String poNo;
  private String incoterms;
  private String paymentTerms;
  private String preCarriage;
  private String countryOfOrigin;
  private String countryOfFinalDestination;
  private String portOfLoading;
  private String portOfDischarge;

  // Items (same name, one-to-many)
  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InvoiceItemEntity> items;

  // Packing Details (same name, one-to-many)
  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PackingDetailEntity> packingDetails;

  // Bank Details
  private String beneficiaryName;
  private String bankName;
  private String branch;
  private String accountNo;
  private String swiftCode;

  // Cost Details
  private Double freightCost;
  private Double insuranceCost;
  private Double otherCost;
  // Export Declarations
  private String arnNo;
  private String rodTep;
  private String rexNo;
}
