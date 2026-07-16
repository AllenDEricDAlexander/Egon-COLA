package top.egon.cola.component.bytecode.api.executor;

public record ExecutorEvent(
        long callSiteId,
        String executorName,
        String executorType,
        String phase,
        String result,
        String exceptionGroup,
        boolean virtualThread,
        long submittedNanos,
        long startedNanos,
        long completedNanos
) {
}
