package ${package}.application.context;

import java.util.Objects;
import java.util.Optional;

public final class OrganizationRequestContextHolder {

    private static final ThreadLocal<OrganizationRequestContext> CURRENT = new ThreadLocal<>();

    private OrganizationRequestContextHolder() {
    }

    public static Optional<OrganizationRequestContext> current() { return Optional.ofNullable(CURRENT.get()); }

    public static void set(OrganizationRequestContext context) {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("Organization request context is already set");
        }
        CURRENT.set(Objects.requireNonNull(context));
    }

    public static void clear() { CURRENT.remove(); }
}
