package top.egon.cola.component.common.trace;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable snapshot of the current trace context.
 */
public class TraceSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String traceId;

    public TraceSnapshot(String traceId) {
        this.traceId = traceId;
    }

    public String getTraceId() {
        return traceId;
    }
}
