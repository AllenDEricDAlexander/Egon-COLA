package top.egon.cola.component.bytecode.bridge;

public final class EgonObservationBridge {

    private EgonObservationBridge() {
    }

    public static ObservationToken enter(Class<?> declaringClass, long methodId) {
        BytecodeRuntimeDispatcher dispatcher = DispatcherRegistry
                .dispatcher(declaringClass, BridgeCapability.OBSERVATION)
                .orElse(null);
        if (dispatcher == null) {
            return ObservationToken.noop();
        }
        try {
            ObservationToken token = dispatcher.enterObservation(declaringClass, methodId);
            return token == null ? ObservationToken.noop() : token;
        } catch (Throwable ignored) {
            return ObservationToken.noop();
        }
    }

    public static void success(ObservationToken token) {
        if (inactive(token)) {
            return;
        }
        try {
            token.dispatcher().observationSuccess(token);
        } catch (Throwable ignored) {
            // Observation must never change the business method result.
        }
    }

    public static void error(ObservationToken token, Throwable throwable) {
        if (inactive(token)) {
            return;
        }
        try {
            token.dispatcher().observationError(token, throwable);
        } catch (Throwable ignored) {
            // Observation must never replace the original business Throwable.
        }
    }

    public static void exit(ObservationToken token) {
        if (inactive(token)) {
            return;
        }
        try {
            token.dispatcher().observationExit(token);
        } catch (Throwable ignored) {
            // Cleanup reporting is best effort.
        }
    }

    private static boolean inactive(ObservationToken token) {
        return token == null || token.noOp() || token.dispatcher() == null;
    }
}
