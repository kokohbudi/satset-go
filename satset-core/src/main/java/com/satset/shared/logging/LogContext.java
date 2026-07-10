package com.satset.shared.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tandai kelas (atau method) sebagai "gate" logging: semua log yang muncul di
 * call-stack-nya ditulis ke folder khusus {@code logs/<value>/} lewat MDC key
 * {@code logctx} + SiftingAppender (lihat {@code logback-spring.xml}).
 *
 * <p>Contoh: {@code @LogContext("CatalogSyncService")} di kelas service →
 * log DigiflazzClient/ProviderHttpConfig/catalog-service selama method service
 * jalan masuk {@code logs/CatalogSyncService/}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface LogContext {
    String value();
}
