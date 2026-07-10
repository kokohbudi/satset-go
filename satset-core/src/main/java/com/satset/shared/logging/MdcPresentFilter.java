package com.satset.shared.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Logback filter berdasar ada/tidaknya MDC {@code logctx}. Dipakai di SiftingAppender
 * ({@code expectPresent=true}) supaya event tanpa {@code logctx} di-DENY — gak bikin
 * bucket folder kosong. Tanpa Janino (config XML pakai setter, bukan expression).
 */
public class MdcPresentFilter extends Filter<ILoggingEvent> {

    private boolean expectPresent = true;

    public void setExpectPresent(boolean expectPresent) {
        this.expectPresent = expectPresent;
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        boolean present = event.getMDCPropertyMap().get("logctx") != null;
        return present == expectPresent ? FilterReply.NEUTRAL : FilterReply.DENY;
    }
}
