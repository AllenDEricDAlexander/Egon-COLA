package top.egon.cola.component.common.trace.autoconfigure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.trace.TraceContext;

import static org.assertj.core.api.Assertions.assertThat;

class TraceThreadLocalAccessorTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void transfersCompleteMdcWithoutExtraThreadLocalState() {
        TraceThreadLocalAccessor accessor = new TraceThreadLocalAccessor();
        TraceContext context = TraceContext.root("request-1");
        try {
            try (TraceContext.Scope ignored = context.open()) {
                MDC.put("tenantId", "tenant-1");
                context = accessor.getValue();
            }
            MDC.clear();

            accessor.setValue(context);

            assertThat(TraceContext.getTraceId())
                    .isEqualTo(context.traceId());
            assertThat(MDC.get("tenantId")).isEqualTo("tenant-1");
            accessor.setValue();
            assertThat(MDC.getCopyOfContextMap()).isNull();
        } finally {
            accessor.close();
        }
    }
}
