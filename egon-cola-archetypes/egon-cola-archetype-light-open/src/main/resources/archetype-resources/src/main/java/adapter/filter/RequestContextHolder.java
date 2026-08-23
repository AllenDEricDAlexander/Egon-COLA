package ${package}.adapter.filter;

import java.util.Optional;

public final class RequestContextHolder {
    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static void set(RequestContext context) {
        CONTEXT.set(context);
    }

    public static Optional<RequestContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static RequestContext getRequired() {
        return get().orElseThrow(() -> new IllegalStateException("request context is not available"));
    }

    public static RequestContext currentOrAnonymous() {
        return get().orElseGet(() -> RequestContext.anonymous("local"));
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
