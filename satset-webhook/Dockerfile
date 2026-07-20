# Build context MUST be the repo root (this module depends on satset-core):
#   docker build -f satset-webhook/Dockerfile .
#   fly deploy --config fly.toml --dockerfile satset-webhook/Dockerfile

FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build
COPY pom.xml .
COPY satset-core satset-core
COPY satset-webhook satset-webhook
RUN mvn -q -pl satset-webhook -am package -DskipTests

FROM eclipse-temurin:25-jre-jammy
RUN useradd --system --create-home --shell /usr/sbin/nologin webhook
WORKDIR /app
COPY --from=builder /build/satset-webhook/target/satset-webhook-*.jar app.jar
# logback writes logs/<file> relative to WORKDIR — must exist and be writable by the non-root user.
RUN mkdir -p /app/logs && chown -R webhook:webhook /app
USER webhook
EXPOSE 8080
# ponytail: tried AppCDS (AutoCreateSharedArchive) for faster cold-start after
# auto-suspend — measured WORSE (67s vs ~21s baseline) on one real test, likely
# confounded by Neon's own compute cold-start, not cleanly separable. Reverted;
# revisit only with a controlled A/B (same DB warm-state) if this matters again.
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
