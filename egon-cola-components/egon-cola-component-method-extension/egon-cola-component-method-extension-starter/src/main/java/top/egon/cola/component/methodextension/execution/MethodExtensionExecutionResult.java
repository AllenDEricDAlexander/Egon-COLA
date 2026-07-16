package top.egon.cola.component.methodextension.execution;

public record MethodExtensionExecutionResult(boolean proceed, Object rejectionValue) {

    public static MethodExtensionExecutionResult proceedInvocation() {
        return new MethodExtensionExecutionResult(true, null);
    }

    public static MethodExtensionExecutionResult reject(Object value) {
        return new MethodExtensionExecutionResult(false, value);
    }
}
