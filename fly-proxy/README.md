# fly-proxy — outbound IP gateway (HTTP proxy)

> Awalnya coba WireGuard, gagal di Fly: image `linuxserver/wireguard` pakai s6
> yang wajib PID 1, Fly microVM jalanin `/init` sebagai PID 1 →
> `s6-overlay-suexec: fatal: can only run as pid 1` → crash loop. Pindah ke
> **HTTP proxy (tinyproxy)** userspace — jalan mulus, tujuan sama.

Tujuan: kasih **IP outbound tetap** buat di-whitelist supplier, tanpa deploy app utama.

## Yang sudah jalan (app `satset-wg-gateway`)

| Hal | Nilai |
|-----|-------|
| Proxy endpoint (buat app lu connect) | `137.66.22.249:8888` (dedicated ingress v4) |
| **Egress IP → WHITELIST INI ke supplier** | **`209.71.95.98`** |
| Auth | BasicAuth `satset` / `<PROXY_PASS>` (di `fly secrets`) |
| Region | `sin` · 1 machine `shared-cpu-1x` |

⚠️ Ingress ≠ egress. App connect ke `137.66.22.249`; supplier lihat `209.71.95.98`.
Jangan `fly ips release` dua-duanya selama whitelist aktif.

## Cara app nembak lewat proxy

### Spring (WebClient / reactor-netty) — disarankan
```java
HttpClient httpClient = HttpClient.create()
    .proxy(spec -> spec
        .type(ProxyProvider.Proxy.HTTP)
        .host("137.66.22.249").port(8888)
        .username("satset")
        .password(p -> System.getenv("PROXY_PASS")));

WebClient client = WebClient.builder()
    .clientConnector(new ReactorClientHttpConnector(httpClient))
    .build();
```

### JVM flag (semua koneksi JVM) — ADA GOTCHA auth
```bash
java -Dhttps.proxyHost=137.66.22.249 -Dhttps.proxyPort=8888 \
     -Dhttp.proxyHost=137.66.22.249 -Dhttp.proxyPort=8888 \
     -Djdk.http.auth.tunneling.disabledSchemes= \
     -jar app.jar
```
`disabledSchemes=` WAJIB — JDK 8u111+ matiin Basic-auth buat HTTPS CONNECT by
default. Plus butuh `java.net.Authenticator` buat kirim user/pass. Ribet →
pakai cara WebClient di atas.

### Docker
```yaml
services:
  app:
    environment:
      PROXY_PASS: "${PROXY_PASS}"   # kode baca via getenv (cara WebClient)
```

## Operasional

```bash
# ganti password proxy
fly secrets set PROXY_PASS=$(openssl rand -hex 16) -a satset-wg-gateway

# lihat IP
fly ips list -a satset-wg-gateway          # ingress + egress
fly logs -a satset-wg-gateway

# test egress dari lokal
curl -x http://satset:PASS@137.66.22.249:8888 https://api.ipify.org   # → 209.71.95.98
```

## Biaya ~$6/bln
ingress v4 $2 + egress v4 ~$2 + machine $1.94. Dev = egress receh.

## Bongkar pas kelar
```bash
fly apps destroy satset-wg-gateway
```
