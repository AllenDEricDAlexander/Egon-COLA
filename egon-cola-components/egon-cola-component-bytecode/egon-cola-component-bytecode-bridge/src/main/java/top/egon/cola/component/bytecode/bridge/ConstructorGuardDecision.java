package top.egon.cola.component.bytecode.bridge;

import java.util.Objects;

public record ConstructorGuardDecision(boolean allowed, Throwable throwable) {

    public ConstructorGuardDecision {
        if (allowed && throwable != null) {
            throw new IllegalArgumentException("ALLOW cannot carry a throwable");
        }
        if (!allowed) {
            Objects.requireNonNull(throwable, "throwable");
        }
    }

    public static ConstructorGuardDecision allow() {
        return new ConstructorGuardDecision(true, null);
    }

    public static ConstructorGuardDecision throwing(Throwable throwable) {
        return new ConstructorGuardDecision(false, throwable);
    }
}
