package top.egon.cola.component.bytecode.bridge;

import java.util.Objects;

public record InvocationDecision(DecisionKind kind, Object value, Throwable throwable) {

    public InvocationDecision {
        Objects.requireNonNull(kind, "kind");
        switch (kind) {
            case PROCEED, RETURN_NULL -> requireEmpty(value, throwable, kind);
            case RETURN_VALUE -> {
                Objects.requireNonNull(value, "value");
                if (throwable != null) {
                    throw new IllegalArgumentException("RETURN_VALUE cannot carry a throwable");
                }
            }
            case THROW -> {
                Objects.requireNonNull(throwable, "throwable");
                if (value != null) {
                    throw new IllegalArgumentException("THROW cannot carry a value");
                }
            }
        }
    }

    public static InvocationDecision proceed() {
        return new InvocationDecision(DecisionKind.PROCEED, null, null);
    }

    public static InvocationDecision returnNull() {
        return new InvocationDecision(DecisionKind.RETURN_NULL, null, null);
    }

    public static InvocationDecision returnValue(Object value) {
        return new InvocationDecision(DecisionKind.RETURN_VALUE, value, null);
    }

    public static InvocationDecision throwing(Throwable throwable) {
        return new InvocationDecision(DecisionKind.THROW, null, throwable);
    }

    private static void requireEmpty(Object value, Throwable throwable, DecisionKind kind) {
        if (value != null || throwable != null) {
            throw new IllegalArgumentException(kind + " cannot carry a value or throwable");
        }
    }
}
