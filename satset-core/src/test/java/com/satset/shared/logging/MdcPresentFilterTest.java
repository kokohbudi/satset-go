package com.satset.shared.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MdcPresentFilterTest {

    @AfterEach
    void clear() {
        MDC.clear();
    }

    private ILoggingEvent eventWith(String logctx) {
        ILoggingEvent e = mock(ILoggingEvent.class);
        when(e.getMDCPropertyMap()).thenReturn(logctx == null ? Map.of() : Map.of("logctx", logctx));
        return e;
    }

    @Test
    void expectPresent_true_keepsWhenMdcSet_deniesWhenAbsent() {
        MdcPresentFilter filter = new MdcPresentFilter();
        filter.setExpectPresent(true);
        assertThat(filter.decide(eventWith("CatalogSyncService"))).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(eventWith(null))).isEqualTo(FilterReply.DENY);
    }

    @Test
    void expectPresent_false_keepsWhenAbsent_deniesWhenSet() {
        MdcPresentFilter filter = new MdcPresentFilter();
        filter.setExpectPresent(false);
        assertThat(filter.decide(eventWith(null))).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(eventWith("CatalogSyncService"))).isEqualTo(FilterReply.DENY);
    }
}
