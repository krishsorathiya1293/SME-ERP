package com.erp.formsmanagement.domain.entity.invoice;

import com.erp.audit.AuditInfo;
import com.erp.encryptionmanagement.converter.EncryptedStringConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "invoices")
@EntityListeners(AuditingEntityListener.class)
public class InvoiceEntity extends AuditInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Shared by the export / commercial / packing-list rows written by one save. */
  private Long groupId;

  /** Print the "supply meant for export under bond or LUT" line with the ARN no. */
  private Boolean lutEnabled;

  // Exporter
  private String exporterCompanyName;
  private String exporterContactNo;

  @Column(columnDefinition = "TEXT")
  private String exporterAddress;

  // Importer (Bill To)
  private String billToCountry;

  @Convert(converter = EncryptedStringConverter.class)
  private String billToName;

  @Convert(converter = EncryptedStringConverter.class)
  private String billToContactNo;

  @Column(columnDefinition = "TEXT")
  @Convert(converter = EncryptedStringConverter.class)
  private String billToAddress;

  // Importer (Ship To)
  private String shipToCountry;

  @Convert(converter = EncryptedStringConverter.class)
  private String shipToName;

  @Convert(converter = EncryptedStringConverter.class)
  private String shipToContactNo;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Enumerated(EnumType.STRING)
  private InvoiceType invoiceType;

  @Column(columnDefinition = "TEXT")
  @Convert(converter = EncryptedStringConverter.class)
  private String shipToAddress;

  // Commercial invoice has its own Bill To / Ship To, typed by hand (often a shipping agent)
  private String commercialBillToCountry;
  private String commercialBillToName;
  private String commercialBillToContactNo;

  @Column(columnDefinition = "TEXT")
  private String commercialBillToAddress;

  private String commercialShipToCountry;
  private String commercialShipToName;
  private String commercialShipToContactNo;

  @Column(columnDefinition = "TEXT")
  private String commercialShipToAddress;

  // Invoice Details
  private String invoiceNo;
  private LocalDate invoiceDate;
  private String gstNo;
  private String iecCode;
  private String poNo;
  private LocalDate poDate;
  private String incoterms;
  private String paymentTerms;
  private String preCarriage;
  private String placeOfPreCarriage;
  private String countryOfOrigin;
  private String countryOfFinalDestination;
  private String portOfLoading;
  private String portOfDischarge;

  // Items (same name, one-to-many)
  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InvoiceItemEntity> items;

  // Bank Details
  private String beneficiaryName;
  private String bankName;
  private String branch;
  private String accountNo;
  private String swiftCode;

  // Currency for the whole invoice, and its INR rate on the day (commercial invoice INR column)
  @Enumerated(EnumType.STRING)
  private ItemCurrency currency;

  private Double exchangeRate;

  /** Add IGST 18% on the commercial invoice. */
  private Boolean igstEnabled;

  /** IGST rate in percent (default 18). */
  private Double igstPercent;

  /** Date printed under the signature. */
  private LocalDate signDate;

  // Cost Details
  private Double freightCost;
  private Double insuranceCost;
  private Double otherCost;
  // Export Declarations
  private String arnNo;
  private String rodTep;
  private String rexNo;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InvoiceEntity)) return false;
    return id != null && id.equals(((InvoiceEntity) o).id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
