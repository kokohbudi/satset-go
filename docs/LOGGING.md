# Logging Guide — omnip-services

> **Last Updated**: 2026-03-01
> **Branch**: `feature/logging-profiles`

---

## Overview

Project ini menggunakan **SLF4J** sebagai facade logging. Implementasi backend bisa
diganti tanpa ubah satu baris kode pun, cukup via Maven property.

Dua implementasi tersedia, keduanya sudah dikonfigurasi **async** (non-blocking terhadap main thread):

| Implementasi | Mekanisme Async | Default? |
|---|---|---|
| **Logback** | `AsyncAppender` (queue-based) | Ya |
| **Log4j2** | `AsyncLogger` via LMAX Disruptor (ring buffer) | Tidak |

---

## Cara Switch Implementasi

```bash
# Default: Logback (tidak perlu flag apapun)
mvn spring-boot:run

# Log4j2
mvn spring-boot:run -Dlog.impl=log4j2

# Bisa dikombinasikan dengan env profile
mvn spring-boot:run -P dev
mvn spring-boot:run -P dev -Dlog.impl=log4j2
mvn spring-boot:run -P prod -Dlog.impl=log4j2
```

> **Kenapa property, bukan `-P` flag?**
> Maven `activeByDefault` mati otomatis begitu ada `-P` lain yang aktif.
> Property-based activation independen dari `-P`, jadi Logback tetap jalan meski
> user hanya menambah `-P dev` tanpa specify logging profile.

---

## Config Files

| File | Aktif Saat |
|---|---|
| `src/main/resources/logback-spring.xml` | `log.impl` tidak di-set (default) |
| `src/main/resources/log4j2-spring.xml` | `-Dlog.impl=log4j2` |

Spring Boot deteksi otomatis berdasarkan classpath — tidak perlu `logging.config` di `application.yml`.

---

## Level Logging per Package

Kedua config file dikonfigurasi identik:

| Package | Level | Output |
|---|---|---|
| `com.omnip` | `DEBUG` | Console + File |
| `org.springframework.security` | `INFO` | Console only |
| `org.hibernate.SQL` | `INFO` | Console only |
| Root (semua lain) | `INFO` | Console + File |

---

## File Output

```
logs/
├── omnip-services.log              ← log aktif
└── omnip-services-2026-03-01.log.gz ← rotasi harian, maks 10 file
```

Rotasi otomatis per hari atau saat file mencapai **10MB**.

---

## Detail Async Config

### Logback AsyncAppender

```
Thread Aplikasi → [LinkedBlockingDeque, size 512] → Worker Thread → File/Console
```

| Setting | Value | Alasan |
|---|---|---|
| `queueSize` | 512 | Cukup untuk traffic PPOB normal |
| `neverBlock` | false | Log tidak di-drop, thread tunggu kalau queue penuh |
| `discardingThreshold` | 0 | Default Logback drop INFO saat 80% — kita disable |

### Log4j2 AsyncLogger + LMAX Disruptor

```
Thread Aplikasi → [Ring Buffer, lock-free] → Logger Thread → File/Console
```

Lebih performant dari Logback AsyncAppender karena ring buffer lock-free.
Trade-off: log di ring buffer bisa hilang kalau JVM crash mendadak.

---

## Trade-offs Singkat

| | Logback (default) | Log4j2 |
|---|---|---|
| Setup | Zero dependency change | Requires Disruptor |
| Throughput | Baik | Sangat tinggi |
| Log loss saat crash | Lebih aman (shutdown hook flush) | Berisiko (ring buffer in-memory) |
| Spring Boot compat | Native (auto-config jalan) | Manual setup |
| Cocok untuk | Development, staging | High-traffic production |

---

## Kode — Tidak Ada yang Berubah

Karena pakai SLF4J via Lombok `@Slf4j`, semua kode tetap sama apapun implementasinya:

```java
@Slf4j
@Service
public class ProductService {
    public void doSomething() {
        log.info("browse products");
        log.debug("detail: {}", detail);
        log.error("error occurred", exception);
    }
}
```

---

## pom.xml — Struktur Profiles

Semua starter di-exclude dari `spring-boot-starter-logging` agar tidak ada
SLF4J binding conflict. Logging diinject ulang via profile:

```xml
<!-- Profile aktif saat log.impl tidak di-set (default Logback) -->
<profile>
    <id>logging-logback</id>
    <activation>
        <property><name>!log.impl</name></property>
    </activation>
    ...
</profile>

<!-- Profile aktif saat -Dlog.impl=log4j2 -->
<profile>
    <id>logging-log4j2</id>
    <activation>
        <property><name>log.impl</name><value>log4j2</value></property>
    </activation>
    ...
</profile>
```

---

## Referensi

- [SLF4J Manual](https://www.slf4j.org/manual.html)
- [Logback AsyncAppender docs](https://logback.qos.ch/manual/appenders.html#AsyncAppender)
- [Log4j2 Async Loggers](https://logging.apache.org/log4j/2.x/manual/async.html)
- [LMAX Disruptor](https://lmax-exchange.github.io/disruptor/)
