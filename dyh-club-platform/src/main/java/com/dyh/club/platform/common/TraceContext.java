package com.dyh.club.platform.common;

import org.slf4j.MDC;

public final class TraceContext {
    public static final String TRACE_ID = "traceId";

    private TraceContext() {}

    public static String traceId() {
        String value = MDC.get(TRACE_ID);
        return value == null ? "" : value;
    }
}
