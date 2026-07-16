package top.egon.cola.component.accessguard.execution;

import java.util.Objects;

public record ConstructorGuardResult(boolean allowed, Throwable throwable) {

    public ConstructorGuardResult {
        if (allowed && throwable != null) {
            throw new IllegalArgumentException("ALLOW cannot carry a throwable");
        }
        if (!allowed) {
            Objects.requireNonNull(throwable, "throwable");
        }
    }

    public static ConstructorGuardResult allow() {
        return new ConstructorGuardResult(true, null);
    }

    public static ConstructorGuardResult reject(Throwable throwable) {
        return new ConstructorGuardResult(false, throwable);
    }
}
