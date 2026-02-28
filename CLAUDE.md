# PPOB Server - Project Context

> **Last Updated**: 2026-02-12
> **Current Phase**: Week 1 - Feature Slice "Browse Products"
> **Status**: Foundation complete, ready to build features

---

## 📋 Quick Overview

**Project**: PPOB (Payment Point Online Bank) Server - Multi-product digital marketplace
**Type**: Spring Boot REST API + Thymeleaf UI
**Approach**: Hybrid Feature Slicing (vertical slices with shared infrastructure)
**Roadmap**: See `ROADMAP.md` for 4-week detailed plan

---

## 🎯 Current Status

### Completed ✅
- Database schema (Categories, Products, ProductDenoms, ProductDenomMeta)
- JPA entities with modern `@UuidGenerator` pattern
- Repository interfaces (CategoryRepository, ProductRepository, etc.)
- User management & Keycloak authentication
- Project structure & dependencies

### In Progress 🔄
- **Week 1**: Browse Products feature (service layer → API → UI)

### Next Up 📌
- Week 2: Purchase flow with mock provider
- Week 3: Balance top-up with mock payment gateway
- Week 4: Admin product management UI

---

## 🏗️ Architecture

### Tech Stack
- **Framework**: Spring Boot 4.0.1
- **Java**: 25
- **Database**: PostgreSQL
- **ORM**: Hibernate 7.x (JPA)
- **Auth**: Keycloak (OAuth2/OIDC)
- **Cache**: Caffeine
- **UI**: Thymeleaf + Tailwind CSS
- **Build**: Maven

### Package Structure
```
com.omnip
├── beans/          - Bean configurations
├── business/       - Business logic layer
├── configs/        - Configuration classes
├── constants/      - Application constants
├── controllers/    - REST & Web controllers
├── converters/     - JPA attribute converters
├── dtos/           - Data Transfer Objects
├── entities/       - JPA entities
├── enums/          - Enumerations (CategoryType, DenomType)
├── exceptions/     - Custom exceptions
├── listeners/      - Event listeners
├── repositories/   - JPA repositories
├── services/       - Service layer
├── utils/          - Utility classes
└── viewmodels/     - View models for UI
```

### Database Schema (Hybrid Approach - Option D)
```
Categories (kategori produk: PULSA, DATA, GAME, PLN_POSTPAID)
    ↓ 1:N
Products (provider: TELKOMSEL, XL, GARENA, PLN)
    ↓ 1:N
ProductDenoms (denominasi: fixed/open amount, prepaid/postpaid)
    ↓ 1:N
ProductDenomMeta (key-value metadata untuk kebutuhan spesifik)
```

### Layers & Flow
```
Controller → Service → Repository → Entity → Database
     ↓          ↓           ↓
    DTO    Business     JPA Query
           Logic
```

---

## 📐 Coding Standards & Patterns

### Entity Pattern
```java
@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
public class EntityName {
    @Id
    @UuidGenerator  // Modern Hibernate 6.5+ (not deprecated @GenericGenerator)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    // Business fields

    // Audit fields (always include these)
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    // Common fields
    private boolean active;
    private boolean deleted;

    @Version  // Optimistic locking
    private Long version;
}
```

### Repository Pattern
```java
@Repository
public interface EntityRepository extends JpaRepository<EntityName, UUID> {
    // Method naming convention
    Optional<EntityName> findByCode(String code);
    List<EntityName> findByActiveTrueAndDeletedFalseOrderBySortOrder();

    // Use @Cacheable for frequently accessed data
    @Cacheable(value = "cacheName", key = "#param", cacheManager = "fastCacheManager")
    EntityName findByParam(String param);
}
```

### Service Pattern
```java
@Service
@Transactional(readOnly = true)  // Default read-only
public class EntityService {
    private final EntityRepository repository;

    // Constructor injection (preferred over @Autowired)
    public EntityService(EntityRepository repository) {
        this.repository = repository;
    }

    @Transactional  // Override for write operations
    public Entity create(EntityDTO dto) {
        // Validation
        // Mapping
        // Save
        return repository.save(entity);
    }
}
```

### Controller Pattern (REST API)
```java
@RestController
@RequestMapping("/api/resource")
public class ResourceController {
    private final ResourceService service;

    public ResourceController(ResourceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ResourceDTO>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    public ResponseEntity<ResourceDTO> create(@Valid @RequestBody ResourceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(dto));
    }
}
```

### Key Decisions & Rationale

1. **UUID over Long for IDs**
   - Distributed-friendly (no auto-increment collision)
   - Security (non-sequential, harder to enumerate)
   - Already established pattern in existing entities

2. **@UuidGenerator instead of @GenericGenerator**
   - @GenericGenerator deprecated since Hibernate 6.5
   - Cleaner syntax, same functionality

3. **LocalDateTime over Date**
   - Modern Java 8+ Time API
   - Better timezone handling
   - Established in Users entity

4. **Soft Delete (deleted flag)**
   - Preserve data for audit/recovery
   - Existing pattern in Users/Stores

5. **Optimistic Locking (@Version)**
   - Prevent lost updates in concurrent scenarios
   - Low overhead, high safety

6. **Mock First, Real Later**
   - Provider integration (MockProviderService → RealProviderService)
   - Payment gateway (MockPaymentGateway → Midtrans/Xendit)
   - Allows rapid development without external dependencies

---

## 🔧 Common Commands

### Development
```bash
# Compile
mvn compile

# Run application
mvn spring-boot:run

# Run with profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
mvn test

# Package
mvn clean package
```

### Database
```bash
# Connect to PostgreSQL
psql -h localhost -U username -d dbname

# Check tables
\dt

# Describe table
\d table_name
```

### Git Workflow
```bash
# Daily commit pattern
git add .
git commit -m "Week X Day Y: Feature description"
git push origin user-management-ui

# Create PR (when week complete)
gh pr create --title "Week X: Feature Slice Name" --body "See ROADMAP.md Week X"
```

---

## 🚨 Important Constraints & Gotchas

### Database
- **DDL Auto**: `update` in dev, `validate` in prod
- **No Flyway/Liquibase**: Schema managed by Hibernate (be careful with changes)
- **PostgreSQL specific**: Use `columnDefinition = "uuid"` for UUID columns

### Security
- **Keycloak Integration**: Existing users have roles in `List<String>`
- **Admin role**: Use `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints
- **CSRF**: Enabled for Thymeleaf forms

### Performance
- **Virtual Threads**: Enabled (Spring Boot 4+)
- **Batch Size**: Configured at 25
- **Caffeine Cache**: Already configured, use `@Cacheable` liberally

### Java 25 Specific
- Lombok warnings about `Unsafe.objectFieldOffset` are normal (Lombok not fully updated yet)
- Virtual threads performance benefit (don't block threads unnecessarily)

---

## 📝 Development Workflow

### Daily Routine
1. **Morning**: Review `ROADMAP.md` for today's tasks
2. **Work**: Code → Test → Commit (small commits)
3. **Evening**: Update progress in `ROADMAP.md` Notes section
4. **Stuck?**: After 30 min, ask for help or pivot to different task

### Weekly Routine
1. **Friday**: Demo week's feature to yourself
2. **Friday**: Update `ROADMAP.md` Week X Status
3. **Friday**: Commit week's work, create PR if needed
4. **Sunday**: Plan next week, review roadmap

### Anti-Burnout Rules
- ✅ 1 feature per week (no scope creep)
- ✅ "Working" > "Perfect"
- ✅ Rest on weekends
- ❌ No 7-day coding streaks
- ❌ No midnight debugging sessions

---

## 🎓 Key Concepts

### Feature Slice (Vertical Slice)
Complete end-to-end implementation of 1 user-facing feature:
```
Database → Entity → Repository → Service → Controller → UI
```

Each slice is **independently deployable and testable**.

### Hybrid Approach
- Use vertical slices for features
- Extract horizontal layers (BaseService, common utils) **after** patterns emerge (2-3 slices)
- Avoid premature abstraction

### Mock-First Integration
- Build against interfaces (`ProviderService`, `PaymentGatewayService`)
- Implement mock versions first (MockProviderService)
- Swap to real implementation when ready (no code change in consumers)

---

## 🔗 Important Files

- **ROADMAP.md**: 4-week detailed plan, updated weekly
- **CLAUDE.md**: This file - project context (always loaded)
- **pom.xml**: Dependencies & build config
- **application.yml**: App configuration (profiles: default, dev, prod)
- **.env**: Local environment variables (database credentials, secrets)

---

## 📞 Next Steps

1. Check current week in `ROADMAP.md`
2. Review tasks for today
3. Start coding!
4. Update progress in ROADMAP.md Notes section

---

## 💡 When Starting New Session

**Say**: "Lanjut dari ROADMAP.md, saya sudah sampai [Week X Day Y / stuck di bagian Z]"

I will:
- Read ROADMAP.md to understand current status
- Continue from where you left off
- Help unblock if stuck

---

**Remember**: Progress > Perfection. Ship working features, refine later.