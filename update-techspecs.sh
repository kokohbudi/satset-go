cat << 'INNER_EOF' >> TechSpecs.md

---

## 💸 Purchase Flow (Week 2) — Technical Blueprint

### 1. Data Models: Transactions & Balance

**A. Stores Table Update (Balance)**
```java
// Stores.java
@Column(precision = 15, scale = 2)
private BigDecimal balance = BigDecimal.ZERO; // Default 0
```
> **Neo's Note**: We use `BigDecimal` for money. NEVER use Double/Float.

**B. Transactions Entity (Flat Table)**
*Note: PPOB transactions are typically 1 item per order, so we flatten it instead of having TransactionItems.*
```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Transactions {
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Stores store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_denom_id", nullable = false)
    private ProductDenoms productDenom;

    @Column(name = "target_number", nullable = false, length = 50)
    private String targetNumber; // No HP / ID Pelanggan

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal price; // Harga dasar produk

    @Column(name = "admin_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal adminFee; // Biaya admin

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal total; // price + adminFee

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "provider_ref", length = 100)
    private String providerRef; // ID dari supplier/provider

    @Column(name = "serial_number", length = 100)
    private String serialNumber; // SN/Voucher dari provider

    // Audit fields (createdAt, updatedAt, createdBy, updatedBy)
    // ...
}
```

**C. TransactionStatus Enum**
```java
public enum TransactionStatus {
    PENDING,     // Dibuat, saldo belum dipotong
    PROCESSING,  // Saldo dipotong, dikirim ke provider
    SUCCESS,     // Provider sukses
    FAILED,      // Provider gagal (membutuhkan refund otomatis)
    REFUNDED     // Saldo sudah dikembalikan ke store
}
```

### 2. Concurrency Control & Balance Management

> **CRITICAL**: The most dangerous part of a PPOB system is Race Conditions resulting in negative balances or double spending.

**Pessimistic Locking in StoreRepository:**
```java
@Repository
public interface StoreRepository extends JpaRepository<Stores, UUID> {
    
    // Pessimistic Write Lock: memblokir transaksi lain yang mencoba membaca/menulis record Stores ini
    // sampai transaksi saat ini commit atau rollback.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stores s WHERE s.id = :id")
    Optional<Stores> findByIdWithPessimisticLock(@Param("id") UUID id);
}
```

**BalanceService Protocol:**
```java
@Service
public class BalanceService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deductBalance(UUID storeId, BigDecimal amount) {
        Stores store = storeRepository.findByIdWithPessimisticLock(storeId)
            .orElseThrow(() -> new EntityNotFoundException());
            
        if (store.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Saldo tidak cukup");
        }
        
        store.setBalance(store.getBalance().subtract(amount));
        storeRepository.save(store);
    }
}
```
*Menggunakan `REQUIRES_NEW` agar lock segera dilepas setelah update saldo selesai, tidak menunggu seluruh HTTP request selesai (menghindari koneksi DB antri kelamaan).*

### 3. API Contract & Mock Provider Integration

**API Request: `POST /api/transactions/purchase`**
```json
{
  "productDenomId": "uuid-here",
  "targetNumber": "08123456789"
}
```

**Transaction Service Flow (Saga / Step-by-step):**
1. Validasi `ProductDenom` aktif dan `Store` aktif.
2. Hitung `total = price + adminFee`.
3. Simpan transaksi dengan status `PENDING`.
4. Panggil `BalanceService.deductBalance(storeId, total)`.
5. Update status transaksi menjadi `PROCESSING`.
6. Panggil `ProviderService.sendTransaction` (async atau sync, untuk MVP kita pakai sync mock dulu yang merespon dalam 500ms).
7. Jika sukses: Update status `SUCCESS`, isi `serialNumber`.
8. Jika gagal/timeout: Update status `FAILED`, panggil `BalanceService.addBalance(storeId, total)` (Refund otomatis), lalu update status `REFUNDED`.

**Mock Provider Design:**
```java
@Service
public class MockProviderService implements ProviderService {
    
    @Override
    public ProviderResponse sendTransaction(String code, String target) {
        // Simulasi delay jaringan (Thread.sleep 500ms - 2s)
        // Simulasi success rate 90% (Math.random())
        // Return object berisi: status (SUKSES/GAGAL), providerRef (random UUID string), sn (random string "SN-XXXXX")
    }
}
```

### 4. Edge Cases & Mitigations

| Edge Case | Impact | Mitigation (Neo's Design) |
|---|---|---|
| User double-click "Buy" | 2 transaksi identik, saldo terpotong 2x | Tambahkan validasi anti-spam: tolak transaksi jika ada transaksi PENDING/PROCESSING dengan target nomor & produk yang sama dalam 1 menit terakhir. |
| DB Timeout saat deduct balance | Saldo tidak terpotong, tapi transaksi ke-record | Karena `@Transactional`, jika timeout, insert Transaksi juga akan ter-rollback. Aman. |
| Provider timeout | Saldo sudah terpotong, status gantung di PROCESSING | Transaksi tetap di `PROCESSING`. Butuh Worker (Cron Job) untuk nge-cek status transaksi gantung ke provider. (Untuk MVP, kita anggap timeout = FAILED langsung). |

INNER_EOF
bash update-techspecs.sh
rm update-techspecs.sh
