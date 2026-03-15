# SME-ERP — Comprehensive AI Context File (`GEMINI.md`)

> **Purpose**: This file is the definitive single-source-of-truth for any AI agent (Gemini, Antigravity, Copilot, etc.) working on this project.  
> Read this file **once** at the start of every new chat. You do **not** need to re-read any other source file unless you are making targeted edits to it.  
> Last updated: 2026-03-15

---

## 1. Project Identity

| Field | Value |
|---|---|
| **Name** | SME-ERP (ERP System) |
| **GroupId** | `com.erp` |
| **ArtifactId** | `SME-ERP` |
| **Version** | `1.0.0-SNAPSHOT` |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.0 |
| **Build Tool** | Maven (multi-module, `pom.xml` at root) |
| **Database** | PostgreSQL — schema `sme_erp` |
| **Migrations** | Flyway (V1…V20 scripts in `web-app/src/main/resources/schema/`) |
| **Code Style** | Google Java Style via `fmt-maven-plugin` (Spotify) |

---

## 2. Module Map

```
SME-ERP/                             ← Root POM (parent)
├── core-service-management/         ← Shared abstractions & OpenAPI YAML specs
├── encryption-management/           ← AES-GCM envelope encryption
├── user-management/                 ← Spring Security, JWT auth
├── export-management/               ← PDF/PNG rendering (Thymeleaf + iText7 + PDFBox)
├── forms-management/                ← All business domain: master, inventory, orders, invoices
└── web-app/                         ← Spring Boot entry point, application.yaml, SQL migrations, HTML templates
```

Each module has `src/main/java/...` and its own `pom.xml`. Only `web-app` has a `main()` class (`WebAppApplication`).

---

## 3. Technology Stack & Key Versions

| Library | Version | Purpose |
|---|---|---|
| Spring Boot | 3.5.0 | Core framework |
| Spring Framework | 6.2.0 | BOM override |
| Spring Cloud | 2025.0.0 | BOM (future microservice extraction) |
| Spring Security | (boot managed) | JWT auth, method security |
| Spring Data JPA | 3.5.6 | ORM |
| Hibernate / PostgreSQL | 42.7.5 | DB driver |
| Flyway | 11.13.2 | Schema migrations |
| Lombok | 1.18.30 | Boilerplate reduction |
| MapStruct | 1.5.5.Final | DTO ↔ Entity mapping (Spring component model) |
| SpringDoc OpenAPI | 2.8.2 | Swagger UI + API doc generation |
| iText7 (`html2pdf`) | (managed) | HTML → PDF |
| PDFBox | (managed) | PDF → PNG at 300 DPI |
| Thymeleaf | (boot managed) | HTML template engine |
| Caffeine Cache | (boot managed) | PDF caching |
| Twilio | (managed) | WhatsApp messaging |
| Java Mail | (managed) | SMTP email |

> **Annotation processors**: Lombok and MapStruct are both in `annotationProcessorPaths`. MapStruct component model is forced to `spring` via `-Amapstruct.defaultComponentModel=spring`.

---

## 4. Generic CRUD Architecture

This is the most important pattern in the codebase. **Every** feature follows it.

### 4.1 Type Parameters Convention

```
E  = JPA Entity
R  = Request DTO (from OpenAPI-generated model, e.g. NewInvoice)
S  = Response DTO (from OpenAPI-generated model, e.g. Invoice)
F  = Filter type
L  = List/paginated response DTO
P  = Parent ID type (used in V2 for nested resources)
I  = ID type (always Long for entities)
```

### 4.2 Interfaces

```java
// V1 — standalone entity (no parent)
interface CoreServiceV1<R, S, I> {
    S getById(I id);
    CreateResult<S> save(R request);
    S update(I id, R request);
    void deleteById(I id);
}

// V2 — child entity (has a parent context)
interface CoreServiceV2<P, R, S, I> {
    S getById(P parentId, I id);
    CreateResult<S> save(P parentId, R request);
    S update(P parentId, I id, R request);
    void deleteById(P parentId, I id);
}

// Pagination & listing
interface GetAllServiceV1<F, L>   // getAll(GetAllQuery<F>) → Page/list
interface GetAllServiceV2<P, F, L>
```

### 4.3 Abstract Service Hierarchy

```
AbstractCrudServiceV1<E,R,S>
    └─ AbstractSpecificationServiceV1<E,R,S>   ← preferred, injects repo + mapper via constructor
         └─ ConcreteXxxService (in forms-management)
```

- `AbstractCrudServiceV1` holds template methods: `getById`, `save`, `update`, `deleteById`
- Hook methods `afterCreate(E, R)` and `afterUpdate(E, R)` are empty by default — override to run post-save logic (e.g., cache eviction via `FormChangedEvent`)
- `AbstractSpecificationServiceV1` injects `JpaRepository<E, Long>` and `EntityMapper<E,R,S>` via constructor (prevents field injection)

### 4.4 EntityMapper Interface

```java
interface EntityMapper<E, R, S> {
    S toDomain(E entity);   // Entity → Response DTO
    E toEntity(R request);  // Request DTO → new Entity
    void updateEntity(E entity, R request);  // Merge request into existing entity
}
```
All mappers are implemented with **MapStruct** (`@Mapper`). They are Spring beans.

### 4.5 CreateResult — Sealed Interface

```java
sealed interface CreateResult<S> permits CreateOne, CreateMany, CreateNone {}
record CreateOne<S>(S item) implements CreateResult<S> {}
record CreateMany<S>(List<S> items) implements CreateResult<S> {}
record CreateNone<S>() implements CreateResult<S> {}
```
The `save()` method returns a `CreateResult` so a single request can create 1, many, or 0 records. The delegate unpacks it using **Java 21 pattern matching**.

### 4.6 Controller Delegate Pattern

Controllers **do not** call services directly. They use delegate objects:

```java
// Controller holds two delegates:
GenericCrudDelegateV1<R, S, Long>   crud  // wraps CoreServiceV1
GetAllDelegateV1<F, L>              page  // wraps GetAllServiceV1

// In a controller method:
public ResponseEntity<S> getById(Long id) { return crud().getById(id); }
public ResponseEntity<List<S>> create(R req) { return crud().createMany(req); }
```

### 4.7 Concrete Controller Pattern (Example — InvoiceController)

```java
@RestController
public class InvoiceController
    extends AbstractCrudControllerV1<NewInvoice, Invoice, String, PaginatedResultInvoice>
    implements InvoiceInvoiceManagementApi {   // ← OpenAPI-generated interface

  public InvoiceController(InvoiceService s) { super(s, s); }

  @Override
  public ResponseEntity<PaginatedResultInvoice> getAllInvoice(...) {
    return page().getAll(GetAllQuery.of(filterByType, search, page, size, sortByFields, direction));
  }
}
```

**Rule**: Every controller extends an `AbstractCrudController*` AND implements the OpenAPI-generated API interface. The OpenAPI interface defines the actual `@RequestMapping`.

---

## 5. Document Export Architecture

### 5.1 Interface — `DocumentDataProvider`

```java
interface DocumentDataProvider {
    String formType();                               // unique key e.g. "invoice"
    List<String> variants();                         // supported variants e.g. ["COMMERCIAL","EXPORT"]
    DocumentData resolve(Long id, String variant);   // load entity, return template + variables
    record DocumentData(String templateName, Map<String, Object> variables) {}
}
```

Implementations are in `forms-management` (one per document type). They extend `AbstractDocumentProvider<V extends Enum>` which provides `variants()` automatically from the enum constants and `toVariant(String)` for safe conversion.

### 5.2 Export Pipeline

```
HTTP GET /api/v1/export/{formType}/{id}?variant=XXX&format=PDF
    → ExportController
    → ExportServiceImpl.getCachedDocument(formType, id, variant, format)
         → [cache HIT] return cached bytes
         → [cache MISS] ExportServiceImpl.generateDocument(...)
              → provider.resolve(id, variant)  → DocumentData
              → DocumentRenderService.renderDocument(templateName, variables, format)
                   → Thymeleaf renders HTML
                   → iText7 converts HTML → PDF bytes
                   → [if PNG] PDFBox renders page 0 at 300 DPI → PNG bytes
```

### 5.3 Cache

- Cache name: `"pdfCache"` (Caffeine)
- Cache key: `"{formType}-{id}-{variant}-{format}"`
- **Eviction**: Prefix-based — when an entity is saved/updated, `FormChangedEvent` triggers eviction of all `"{formType}-{id}-*"` keys

### 5.4 Document Formats

```java
enum DocumentFormat { PDF, PNG }
```

### 5.5 Fonts & Assets

Embedded in classpath:
- **Fonts**: `KantumruyPro-{Regular,Medium,Bold,Light}.ttf`, `NotoSans-{Regular,Bold}.ttf`
- **Images**: `company-logo.png.b64`, `stamp.jpg.b64` injected as `companyLogoDataUri` / `stampDataUri` into every Thymeleaf context automatically

### 5.6 HTML Templates (in `web-app/src/main/resources/templates/`)

| File | Purpose |
|---|---|
| `invoice-commercial.html` | Commercial invoice PDF |
| `invoice-export.html` | Export invoice PDF |
| `invoice-packaging-list.html` | Packaging list PDF |
| `jobwork-avak.html` | Job-work incoming (Avak) PDF |
| `jobwork-javak.html` | Job-work outgoing (Javak) PDF |
| `packing-invoice-party.html` | Packing invoice for party |

---

## 6. Encryption Architecture

Module: `encryption-management`  
Package: `com.erp.encryptionmanagement`

### 6.1 Algorithm

**AES-256-GCM Envelope Encryption** (defense-in-depth):

```
plaintext
  → encrypted with random per-value DATA KEY (32 bytes, AES-GCM)
  → data key encrypted with MASTER KEY (AES-GCM)
  → JSON payload stored in DB column:
      { "v": 1, "mkv": "master-v1", "dk": {...}, "d": {...} }
```

- `"mkv"` = master key version (string alias)
- `"dk"` = wrapped data key (GcmBlob in base64 map)
- `"d"` = encrypted data (GcmBlob in base64 map)

### 6.2 Key Storage

Master key loaded from (priority order):
1. Environment variable `MASTER_KEY_BASE64` (base64-encoded 32-byte key)
2. PKCS12 keystore at `MASTER_KEYSTORE_PATH` (default `/opt/app/keys/master.p12`)

Config key: `master-key-store.*` in `application.yaml`  
Key rotation: `EnvelopeCryptoService.rotateKey()`

### 6.3 JPA Integration

`EncryptedStringConverter` is a `@Converter(autoApply=false)` — must be applied explicitly:

```java
@Convert(converter = EncryptedStringConverter.class)
private String billToName;  // stored encrypted in DB
```

### 6.4 Backward Compatibility

If a column value does not start with `{` and doesn't contain `"mkv"`, `"dk"`, `"d"`, it is treated as **plaintext** (no exception thrown). This handles legacy unencrypted rows.

---

## 7. Security & Authentication

Module: `user-management`  
Package: `com.erp.usermanagement`

### 7.1 JWT Flow

```
POST /api/v1/auth/signin  { email, password }
  → AuthenticationService.authenticate()
  → Spring AuthenticationManager validates credentials
  → JwtTokenService.generateAccessToken() + generateRefreshToken()
  → Returns { accessToken, refreshToken }

POST /api/v1/auth/refresh { refreshToken }
  → AuthenticationService.refreshToken()
  → validateRefreshToken, issue new accessToken (same refreshToken)
```

### 7.2 Security Filter Chain

- **Stateless** sessions (`SessionCreationPolicy.STATELESS`)
- CSRF **disabled**
- `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`
- Public endpoints (from `security.public-access` list in YAML):
  - `POST /api/v1/auth/signin`
  - `POST /api/v1/users/register`
  - `/swagger-ui/**`, `/v3/api-docs/**`, `/doc/v3/**`
- All other endpoints require a valid JWT

### 7.3 Password & Roles

- Passwords hashed with **BCryptPasswordEncoder**
- Role stored as `UserGroup` enum in `UserEntity`
- Method security enabled via `@EnableMethodSecurity(securedEnabled=true)`
- `@Admin` is a custom meta-annotation for role-checking

### 7.4 CORS

Configured via `security.cors.mappings` list in YAML. Allowed headers: `Authorization`, `Content-Type`, `Cache-Control`. Comma-separated origin strings from `.env` are auto-split and flattened.

### 7.5 UserEntity Fields

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Auto-generated |
| `userEmail` | `String` | Username for auth |
| `password` | `String` | BCrypt hash |
| `userGroup` | `UserGroup` (enum) | Role |
| `enabled` | `Boolean` | Default `true` |
| `createdAt` | `LocalDateTime` | From `AuditInfo` |
| `lastUpdatedAt` | `LocalDateTime` | From `AuditInfo` |

---

## 8. Domain Entities (forms-management)

Package root: `com.erp.formsmanagement.domain.entity`  
All entities extend `AuditInfo` (`createdAt`, `lastUpdatedAt` via `@EntityListeners(AuditingEntityListener.class)`).  
All use `@GeneratedValue(strategy = IDENTITY)` with `Long` PK.  
All override `equals`/`hashCode` based only on `id` (JPA best practice).

### 8.1 Master Data

#### `PartyEntity` — table `party`

| Field | Type |
|---|---|
| `id` | Long |
| `name` | String |
| `gst` | String |
| `contactNo` | String |
| `email` | String |
| `partyType` | `PartyType` enum: `SUPPLIER / BUYER / JOB_WORKER / ...` |

#### `CategoryEntity` — table `category`

| Field | Type |
|---|---|
| `id` | Long |
| `name` | String |

#### `ItemEntity` — table `item`

| Field | Type | Notes |
|---|---|---|
| `id` | Long | |
| `size` | `@OneToOne(LAZY)` → `ItemBlueprintDataEntity` | FK `size_id` |
| `itemKg` | Double | |
| `weightPerPc` | Double | |
| `totalPc` | Double | |
| `lowStockWarning` | Double | |
| `stockStatus` | `StockStatus` enum | |

### 8.2 Inventory

#### `ItemBlueprintEntity` — blueprint/template for item sizes

Defines blueprint groups (e.g., product type/category).

#### `ItemBlueprintDataEntity` — concrete size record

Each row is a specific size within a blueprint. It is referenced by `item.size_id`, `inventory.size_id`, `job_works.size_id`, etc.

#### `InventoryEntity` — table `inventory`

One row = one size (unique constraint on `size_id`).

| Field | Type | Notes |
|---|---|---|
| `id` | Long | |
| `size` | `@OneToOne(LAZY) unique` → `ItemBlueprintDataEntity` | |
| `pcsPerBox` | Integer | |
| `boxPerCarton` | Integer | |
| `pcsPerCarton` | Integer | |
| `cartonWeight` | Double | |
| `sssatinlacq` | Double | Price per kg for SS satin lacquer finish |
| `antiq` | Double | Price per kg for antique finish |
| `sidegold` | Double | |
| `zblack` | Double | |
| `grblack` | Double | |
| `mattss` | Double | |
| `mattantiq` | Double | |
| `pvdrose` | Double | |
| `pvdgold` | Double | |
| `pvdblack` | Double | |
| `rosegold` | Double | |
| `clearlacq` | Double | |

#### `ClientInventoryEntity` — table `client_inventory`

Tracks inventory at client side (separate from main inventory).

### 8.3 Orders

#### `OrderEntity` — table `orders`

| Field | Type |
|---|---|
| `id` | Long |
| `party` | `@ManyToOne(LAZY)` → `PartyEntity` |
| `orderDate` | `LocalDate` |
| `orderItems` | `@OneToMany(CASCADE_ALL, orphanRemoval)` → `List<OrderItemEntity>` |

#### `OrderItemEntity`

Each line item of an order. References `ItemBlueprintDataEntity` for size.

#### `OrderDispatchEntity` — table `order_dispatch`

Tracks dispatch of order items.

#### `JobWorkEntity` — table `job_works`

Subcontracting work sent out for processing.

| Field | Type | Notes |
|---|---|---|
| `id` | Long | |
| `orderItem` | `@OneToOne(LAZY) unique` → `OrderItemEntity` | FK `order_item_id` |
| `party` | `@ManyToOne(LAZY)` → `PartyEntity` | Job worker |
| `size` | `@ManyToOne(LAZY)` → `ItemBlueprintDataEntity` | |
| `jobDate` | `LocalDate` | |
| `qtyPc` | Double | Quantity in pieces |
| `qtyKg` | Double | Quantity in kg |
| `finish` | String | Finish type name |
| `elementCount` | Double | |
| `elementType` | `ElementType` enum | |
| `status` | `JobWorkStatus` enum | e.g., `PENDING`, `COMPLETED` |
| `jobWorkType` | `JobWorkType` enum | e.g., `AVAK` (outgoing), `JAVAK` (return/incoming) |
| `jobWorkReturns` | `@OneToMany(CASCADE_ALL)` → `List<JobWorkReturnEntity>` | Multiple returns allowed |

#### `JobWorkReturnEntity`

Return records for a job work. Has `returnDate` column. Multiple returns per job work are allowed (V19 migration).

### 8.4 Invoices

#### `InvoiceEntity` — table `invoices`

Full export/commercial invoice with PII fields encrypted at rest.

| Field | Type | Notes |
|---|---|---|
| `id` | Long | |
| `exporterCompanyName` | String | |
| `exporterContactNo` | String | |
| `exporterAddress` | TEXT | |
| `billToCountry` | String | |
| `billToName` | String | **@Convert(EncryptedStringConverter)** |
| `billToContactNo` | String | **Encrypted** |
| `billToAddress` | TEXT | **Encrypted** |
| `shipToCountry` | String | |
| `shipToName` | String | **Encrypted** |
| `shipToContactNo` | String | **Encrypted** |
| `shipToAddress` | TEXT | **Encrypted** |
| `invoiceType` | `InvoiceType` enum | `COMMERCIAL / EXPORT / PACKAGING_LIST` |
| `invoiceNo` | String | |
| `invoiceDate` | `LocalDate` | |
| `gstNo` | String | |
| `iecCode` | String | |
| `poNo` | String | |
| `incoterms` | String | |
| `paymentTerms` | String | |
| `preCarriage` | String | |
| `countryOfOrigin` | String | |
| `countryOfFinalDestination` | String | |
| `portOfLoading` | String | |
| `portOfDischarge` | String | |
| `items` | `@OneToMany(CASCADE_ALL)` → `List<InvoiceItemEntity>` | |
| `beneficiaryName` | String | Bank |
| `bankName` | String | |
| `branch` | String | |
| `accountNo` | String | |
| `swiftCode` | String | |
| `freightCost` | Double | |
| `insuranceCost` | Double | |
| `otherCost` | Double | |
| `arnNo` | String | Export declaration |
| `rodTep` | String | |
| `rexNo` | String | |

#### `InvoiceItemEntity`

Line items. Has `ItemCurrency` enum and references `InvoiceEntity` via FK.

#### `PackingInvoiceEntity` — table `packing_invoice`

Simple domestic packing/dispatch invoice.

| Field | Type | Notes |
|---|---|---|
| `id` | Long | |
| `invoiceDate` | `LocalDate` | |
| `invoiceNo` | String | Auto-generated: sequential per party+date (`01`, `02`, …) |
| `party` | `@ManyToOne(LAZY)` → `PartyEntity` | |
| `cartoonNo` | String | Auto-generated: cumulative per party across all dates |
| `items` | `@OneToMany(CASCADE_ALL)` → `List<PackingInvoiceItemEntity>` | |

**Special method**: `@Transient getSummaryItems()` — aggregates items by `size_id + finish`, summing `totalPc` and `totalRs`. Used directly in Thymeleaf templates.

#### `PackingInvoiceItemEntity`

| Key Fields | Notes |
|---|---|
| `size` | `@ManyToOne` → `ItemBlueprintDataEntity` |
| `finish` | String |
| `totalPc` | Double |
| `totalRs` | Double |
| `scrap` | Double |
| `rsKg` | Double |
| `ratePc` | Double |

---

## 9. API Design — OpenAPI First

All REST API interfaces are **generated from OpenAPI YAML specs** stored in:
```
core-service-management/src/main/resources/
├── client-management.yaml
├── export-management.yaml
├── invoice-management.yaml
├── item-management.yaml
├── master.yaml
├── order-management.yaml
├── packing-invoice-management.yaml
└── user-management.yaml
```

The generated Java interfaces (e.g., `InvoiceInvoiceManagementApi`) contain `@RequestMapping` annotations. Concrete controllers **only** implement business logic — they **never** define URL mappings themselves.

### 9.1 Swagger UI Groups (from `application.yaml`)

| Name | URL |
|---|---|
| User Management | `/doc/v3/user-api` |
| Master Management | `/doc/v3/master` |
| Invoice Management | `/doc/v3/invoice` |
| Export Management | `/doc/v3/export` |
| Item Management | `/doc/v3/item` |
| Client Management | `/doc/v3/client` |
| Order Management | `/doc/v3/orders` |
| Packing Invoice | `/doc/v3/packing-invoice` |

---

## 10. Database Schema & Migrations

Schema: `sme_erp`  
Migration files: `web-app/src/main/resources/schema/V{n}__{description}.sql`

| Version | Description |
|---|---|
| V1 | Create schema `sme_erp` |
| V2 | Create `users` table |
| V3 | Create `party` table |
| V4 | Create `category` table |
| V5 | Create `item` table |
| V6 | Create `invoices` table |
| V7 | Update invoice table |
| V8 | Update encrypted column types to TEXT |
| V9 | Fix invoice response time (`createdAt` indexing) |
| V10 | Add database indexes |
| V11 | Create `inventory` table |
| V12 | Create `client_inventory` table |
| V13 | Create `orders` + `order_items` tables |
| V14 | Add unique constraint on `inventory.size_id` |
| V15 | Create `job_works` table |
| V16 | Merge `item` and `blueprint` (consolidation) |
| V17 | Create `packing_invoice` + `packing_invoice_items` tables |
| V18 | Fix `order_items.size_id` unique constraint |
| V19 | Allow multiple `job_work_returns` (remove unique constraint) |
| V20 | Add `return_date` column to `job_work_returns` |

> **Note**: `ddl-auto: validate` — Hibernate never modifies schema. All changes MUST go through Flyway scripts.

---

## 11. Configuration Reference (`application.yaml`)

### Environment Variables Required

| Env Var | Purpose |
|---|---|
| `DB_HOST` | PostgreSQL host |
| `DB_PORT` | PostgreSQL port |
| `DB_NAME` | Database name |
| `DB_USER` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET_KEY` | JWT HMAC signing key (base64) |
| `ACCESS_TOKEN_EXPIRY` | Access token TTL (ms) |
| `REFRESH_TOKEN_EXPIRY` | Refresh token TTL (ms) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins |
| `THYMLEAF_CACHE_ENABLED` | `true`/`false` |
| `MAIL_EMAIL` | SMTP sender email |
| `MAIL_PASSWORD` | SMTP password |
| `TWILIO_ACCOUNT_SID` | Twilio SID |
| `TWILIO_AUTH_TOKEN` | Twilio auth |
| `TWILIO_WHATSAPP_FROM` | WhatsApp sender number |
| `MASTER_KEY_BASE64` | 32-byte master key base64 (encryption) |
| `MASTER_KEY_VERSION` | e.g., `master-v1` |
| `MASTER_KEYSTORE_PATH` | Path to PKCS12 keystore file |
| `MASTER_KEYSTORE_PASSWORD` | Keystore password |
| `MASTER_KEYSTORE_ACTIVE_ALIAS` | Path to active alias file |
| `MASTER_KEYSTORE_CREATE_IF_MISSING` | `true`/`false` |

### Spring Profiles

- `application-dev.yaml` — development overrides
- `application-local.yaml` — local overrides
- `application-prod.yaml` — production overrides

### HikariCP Pool

- `maximum-pool-size: 30`, `minimum-idle: 10`
- `connection-timeout: 30s`, `idle-timeout: 10min`, `max-lifetime: 30min`

---

## 12. Pagination & Filtering Pattern

All list endpoints use `GetAllQuery<F>` record:

```java
record GetAllQuery<F>(
    Optional<F> filter,     // typed filter object (e.g., InvoiceType enum)
    Optional<String> search,
    Optional<Integer> page,
    Optional<Integer> size,
    Optional<String> sortBy,
    Optional<String> direction
)
```

Default pagination: `page=0`, `size=10`, `sortBy="createdAt"`, `direction="DESC"`.

Filter objects (e.g., `ClientInventoryFilter`, `InventoryFilter`, `PackingInvoiceFilter`) are passed as query parameters and used in Spring Data JPA `Specification` queries.

---

## 13. Core Infrastructure Classes (Quick Reference)

| Class | Package | Role |
|---|---|---|
| `AuditInfo` | `com.erp.audit` | `@MappedSuperclass` with `createdAt`, `lastUpdatedAt` |
| `EntityMapper<E,R,S>` | `com.erp.mapper` | MapStruct interface contract |
| `CoreRepository` | `com.erp.repository` | Custom base repository (if any) |
| `AbstractCrudServiceV1<E,R,S>` | `com.erp.service` | CRUD template (standalone) |
| `AbstractCrudServiceV2<E,R,S,P>` | `com.erp.service` | CRUD template (with parent) |
| `AbstractSpecificationServiceV1` | `com.erp.service` | Injects repo+mapper, extends V1 |
| `AbstractSpecificationServiceV2` | `com.erp.service` | Injects repo+mapper, extends V2 |
| `AbstractDocumentProvider<V>` | `com.erp.service` | Base for PDF providers, auto-derives variants from enum |
| `DocumentDataProvider` | `com.erp.service` | Interface: formType/variants/resolve |
| `DocumentFormat` | `com.erp.service` | Enum: `PDF`, `PNG` |
| `AbstractCrudControllerV1` | `com.erp.controller` | Holds `crud()` + `page()` delegates |
| `AbstractCrudControllerV2` | `com.erp.controller` | Same, with parent ID in signatures |
| `AbstractDocumentController` | `com.erp.controller` | `buildPdfResponse()` + `buildPngResponse()` helpers |
| `GenericCrudDelegateV1` | `com.erp.controller` | HTTP response construction for V1 |
| `GenericCrudDelegateV2` | `com.erp.controller` | HTTP response construction for V2 |
| `GetAllDelegateV1/V2` | `com.erp.controller` | Pagination delegate |
| `CreateResult<S>` | `com.erp.wrappers` | Sealed: `CreateOne`, `CreateMany`, `CreateNone` |
| `GetAllQuery<F>` | `com.erp.util` | Pagination + filter container |
| `PaginationUtils` | `com.erp.util` | `getPageRequest(page, size, dir, sortBy)` |
| `PageMapper` | `com.erp.util` | Converts Spring `Page<E>` to DTO |
| `FormChangedEvent` | `com.erp.event` | Record: `formType`, `entityId`, `action` (for cache eviction) |
| `TransliterationService` | `com.erp.config.transliteration` | REST client for text transliteration |
| `OpenApiConfig` | `com.erp.config` | SpringDoc beans |
| `GlobalExceptionHandler` | `com.erp.exceptionhandler` (web-app) | `@RestControllerAdvice` |
| `WebAppApplication` | `com.erp` (web-app) | `main()` entry point |

---

## 14. Exception Types

| Exception | When Thrown |
|---|---|
| `EntityNotFoundException` | Entity not found by ID |
| `ForeignKeyReferenceException` | Delete blocked by FK constraint |
| `EncryptionException` | Encrypt/decrypt failure |
| `PdfGenerationFailedException` | PDF/PNG rendering error |

---

## 15. Service Layer Structure (forms-management)

Package: `com.erp.formsmanagement.service`

| Sub-package | Services |
|---|---|
| `master` | `PartyService`, `ItemService`, `CategoryService` |
| `inventory` | `InventoryService`, `ItemBlueprintService`, `ItemBlueprintDataService` |
| `order` | `OrderService`, `OrderDispatchService`, `JobWorkService`, `JobWorkReturnService` |
| `invoice` | `InvoiceService` |
| `packinginvoice` | `PackingInvoiceService` |
| `client` | `ClientInventoryService` |

Each service has a corresponding `impl/` sub-package. Service interfaces extend `CoreServiceV1` + `GetAllServiceV1` (or V2 variants). Implementations extend `AbstractSpecificationServiceV1/V2`.

---

## 16. Controller Map (forms-management)

| Controller | Package | Entity |
|---|---|---|
| `PartyController` | `controller.master` | `party` |
| `CategoryController` | `controller.master` | `category` |
| `ItemController` | `controller.master` | `item` |
| `InventoryController` | `controller.inventory` | `inventory` |
| `ItemBlueprintController` | `controller.inventory` | `item_blueprint` |
| `ItemBlueprintDataController` | `controller.inventory` | `item_blueprint_data` |
| `OrderController` | `controller.order` | `orders` |
| `OrderDispatchController` | `controller.order` | `order_dispatch` |
| `JobWorkController` | `controller.order` | `job_works` |
| `JobWorkReturnController` | `controller.order` | `job_work_returns` |
| `InvoiceController` | `controller.invoice` | `invoices` |
| `PackingInvoiceController` | `controller.packinginvoice` | `packing_invoice` |
| `ClientInventoryController` | `controller.client` | `client_inventory` |

---

## 17. Coding Conventions

1. **Google Java Style** — always run `mvn fmt:format` before committing.
2. **Lombok**: Use `@Getter @Setter @NoArgsConstructor` on entities (NOT `@Data` — avoid `equals`/`hashCode` issues with JPA). Use `@Data` only on simple non-entity POJOs.
3. **JPA `equals`/`hashCode`**: Always implement based on `id` only. Never use Lombok `@EqualsAndHashCode` on `@Entity` classes.
4. **No field injection**: Always use constructor injection. `@RequiredArgsConstructor` preferred.
5. **OpenAPI First**: Never write `@RequestMapping` in concrete controllers. Always derive from generated interface.
6. **Readonly**: No `@Transient` business logic in Entities except `getSummaryItems()` pattern (already established in `PackingInvoiceEntity`).
7. **Flyway**: Every schema change = new migration script. Never modify existing scripts.
8. **Encryption**: Only apply `@Convert(EncryptedStringConverter.class)` to PII fields. Never encrypt IDs or enums.
9. **`afterCreate` / `afterUpdate` hooks**: Override in service implementations when post-save side effects are needed (e.g., publishing `FormChangedEvent`).
10. **`CreateResult`**: Return `CreateOne` for single saves, `CreateMany` when a single request creates multiple rows (e.g., creating multiple invoice items), `CreateNone` when nothing should be returned.

---

## 18. How to Add a New Feature (Standard Template)

1. **Define API** in the relevant YAML file in `core-service-management/src/main/resources/`
2. **Run** OpenAPI generator to produce API interface
3. **Create Entity** extending `AuditInfo`, add `equals`/`hashCode` by id
4. **Write Flyway migration** `V{n+1}__{description}.sql`
5. **Create Repository** extending `JpaRepository<Entity, Long>` (+ `JpaSpecificationExecutor` if filtering)
6. **Create MapStruct Mapper** implementing `EntityMapper<E, R, S>`
7. **Create Service interface** extending `CoreServiceV1<R, S, Long>` + `GetAllServiceV1<F, L>`
8. **Create Service implementation** extending `AbstractSpecificationServiceV1<E, R, S>`, implementing the service interface
9. **Create Controller** extending `AbstractCrudControllerV1<R, S, F, L>`, implementing the generated API interface
10. **Register as Spring bean** — all annotated with `@Service`, `@RestController`, `@Repository`, `@Mapper` — auto-detected by component scan

---

## 19. Running & Building

```bash
# Run locally (dev profile)
./mvnw spring-boot:run -pl web-app -Dspring-boot.run.profiles=local

# Compile
./mvnw compile

# Format code
./mvnw fmt:format

# Package
./mvnw package -DskipTests

# Docker
docker build -t sme-erp .
```

> `Dockerfile` is at project root. It produces a single runnable JAR from the `web-app` module.

---

## 20. Key Architectural Decisions (ADRs in Brief)

| Decision | Rationale |
|---|---|
| Multi-module Maven | Clear dependency boundaries; each module is a distinct library |
| OpenAPI-first | Contract-driven dev; Swagger UI always in sync with code |
| Sealed `CreateResult` | Type-safe alternative to overloaded return values; uses Java 21 pattern matching |
| Generic abstract layers (V1/V2) | Eliminates boilerplate; forces consistent behavior across 13+ CRUD features |
| Envelope encryption | Master key rotation without re-encrypting all data; per-value unique data keys |
| Caffeine PDF cache + `FormChangedEvent` | Expensive PDF generation cached; invalidated on data change |
| Flyway + `ddl-auto=validate` | Production-safe schema management; prevents accidental schema drift |
| `AbstractDocumentProvider` with enum variants | Type-safe variant resolution; variants are self-documenting from enum |

---

*This file was auto-generated by AI analysis of the complete project source tree on 2026-03-15. Update this file whenever significant architectural changes are made.*
