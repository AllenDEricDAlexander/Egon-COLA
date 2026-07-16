package top.egon.cola.component.bytecode.bridge;

public record ObservationToken(
        boolean noOp,
        BytecodeRuntimeDispatcher dispatcher,
        Object state
) {
    private static final ObservationToken NOOP = new ObservationToken(true, null, null);

    public ObservationToken {
        if (!noOp && dispatcher == null) {
            throw new IllegalArgumentException("Active observation token requires a dispatcher");
        }
    }

    public static ObservationToken noop() {
        return NOOP;
    }

    public static ObservationToken active(BytecodeRuntimeDispatcher dispatcher, Object state) {
        return new ObservationToken(false, dispatcher, state);
    }
}
