package top.egon.cola.component.tianshu.admin.security.tenancy;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@Component("ddcTenantContextExecutor")
public class DdcTenantContextExecutor {

    public Scope bind(DdcTenantContextDTO context) {
        Objects.requireNonNull(context, "context");
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            MDC.put("tenantId", Long.toString(context.tenantId()));
            MDC.put("userId", context.actor());
        } catch (RuntimeException failure) {
            restore(previous);
            throw failure;
        }
        return () -> restore(previous);
    }

    public void run(DdcTenantContextDTO context, Runnable action) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = bind(context)) {
            action.run();
        }
    }

    public <T> T call(DdcTenantContextDTO context, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        try (Scope ignored = bind(context)) {
            return action.get();
        }
    }

    private void restore(Map<String, String> previous) {
        if (previous == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(previous);
        }
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
