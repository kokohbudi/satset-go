package com.satset.shared.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogContextAspectTest {

    private final LogContextAspect aspect = new LogContextAspect();

    @AfterEach
    void clear() {
        MDC.clear();
    }

    private LogContext ctx(String value) {
        LogContext c = mock(LogContext.class);
        when(c.value()).thenReturn(value);
        return c;
    }

    @Test
    void setsMdcDuringProceed_clearsAfter() throws Throwable {
        String[] seenInside = new String[1];
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenAnswer(inv -> {
            seenInside[0] = MDC.get("logctx");
            return "ok";
        });

        Object result = aspect.scope(pjp, ctx("CatalogSyncService"));

        assertThat(result).isEqualTo("ok");
        assertThat(seenInside[0]).isEqualTo("CatalogSyncService"); // set selama proceed
        assertThat(MDC.get("logctx")).isNull();                    // clear setelah advice
    }

    @Test
    void restoresPriorValue_whenNested() throws Throwable {
        MDC.put("logctx", "Outer");
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("inner");

        aspect.scope(pjp, ctx("Inner"));

        assertThat(MDC.get("logctx")).isEqualTo("Outer"); // nilai lama dipulihkan, bukan dihapus
    }

    @Test
    void clearsMdc_evenWhenProceedThrows() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        try {
            aspect.scope(pjp, ctx("CatalogSyncService"));
        } catch (IllegalStateException expected) {
            // propagate
        }
        assertThat(MDC.get("logctx")).isNull();
    }
}
