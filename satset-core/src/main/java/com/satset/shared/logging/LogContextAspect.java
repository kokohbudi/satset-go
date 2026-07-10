package com.satset.shared.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Set MDC {@code logctx} = {@link LogContext#value()} selama method kelas ber-anotasi
 * jalan, restore nilai lama di {@code finally} (aman kalau gate nested / proceed melempar).
 * Downstream di thread yang sama mewarisi MDC → log-nya ke-route ke folder context.
 */
@Aspect
@Component
public class LogContextAspect {

    static final String KEY = "logctx";

    @Around("@within(ctx)")
    public Object scope(ProceedingJoinPoint pjp, LogContext ctx) throws Throwable {
        String prior = MDC.get(KEY);
        MDC.put(KEY, ctx.value());
        try {
            return pjp.proceed();
        } finally {
            if (prior != null) {
                MDC.put(KEY, prior);
            } else {
                MDC.remove(KEY);
            }
        }
    }
}
