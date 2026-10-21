package top.egon.cola.component.dtp.exception;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.common.core.exception.ExceptionLogger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 使用公共异常分类、级别和脱敏堆栈，默认不触发业务重试。 */
@Slf4j
public class LoggingDtpTaskExceptionHandler implements DtpTaskExceptionHandler {
    @Override
    public void handle(DtpTaskFailureBO failure) {
        Objects.requireNonNull(failure, "failure");
        if (!failure.isLogEligible()) { return; }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("failureId", failure.getFailureId());
        fields.put("appName", failure.getAppName());
        fields.put("instanceId", failure.getInstanceId());
        fields.put("executorName", failure.getExecutorName());
        fields.put("executorKind", failure.getExecutorKind() == null ? null : failure.getExecutorKind().name());
        fields.put("source", failure.getSource());
        fields.put("threadName", failure.getThreadName());
        fields.put("methodName", failure.getMethodName());
        fields.put("observedAt", failure.getObservedAt().toString());
        fields.put("suppressedCount", Long.toString(failure.getSuppressedCount()));
        if (failure.getCause() instanceof DtpContextPropagationException propagation) {
            fields.put("contextPhase", propagation.getPhase());
            fields.put("accessorKey", propagation.getAccessorKey());
        }
        ExceptionLogger.log(log, failure.getCause(), fields);
    }
}
