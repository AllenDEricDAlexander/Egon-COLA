package top.egon.cola.component.dtp.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.core.exception.ExceptionLogger;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties;
import top.egon.cola.component.dtp.context.DtpTaskObserver;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.enums.ExceptionCategory;

/** Spring void异常必须在外层Callable返回前向同任务发出失败信号。 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DtpAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
    @Qualifier("dtpTaskExceptionHandler")
    private final ObjectProvider<DtpTaskExceptionHandler> handlers;
    @Qualifier("dtpValidationUtils")
    private final ObjectProvider<ValidationUtils> validation;
    @Qualifier("egon.cola.component.dtp-top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties")
    private final DynamicThreadPoolAutoProperties properties;
    private final AtomicLong lastSystemLog = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong lastBusinessLog = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong suppressedSystem = new AtomicLong();
    private final AtomicLong suppressedBusiness = new AtomicLong();

    @Override
    public void handleUncaughtException(Throwable failure, Method method, Object... params) {
        String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
        if (DtpTaskObserver.asyncFailure(failure, methodName)) { return; }
        if (!properties.getExceptionHandling().isEnabled() || !properties.getExceptionHandling().isAsyncVoidEnabled()) { return; }
        TraceContext trace = TraceContext.capture();
        try (TraceContext.Scope ignored = trace.open()) {
            Throwable classified = failure;
            Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            while ((classified instanceof CompletionException || classified instanceof ExecutionException)
                    && classified.getCause() != null && seen.add(classified)) { classified = classified.getCause(); }
            boolean business = classified instanceof CommonException common
                    && common.getExceptionCategory() == ExceptionCategory.BUSINESS;
            AtomicLong last = business ? lastBusinessLog : lastSystemLog;
            AtomicLong suppressed = business ? suppressedBusiness : suppressedSystem;
            long now = System.nanoTime();
            long previous = last.get();
            boolean eligible = (previous == Long.MIN_VALUE || now - previous >= properties.getExceptionHandling().getLogInterval().toNanos())
                    && last.compareAndSet(previous, now);
            if (!eligible) { suppressed.incrementAndGet(); }
            DtpTaskFailureBO report = DtpTaskFailureBO.builder().failureId(UUID.randomUUID().toString())
                .appName(properties.getAppName()).instanceId(properties.getInstanceId()).source("ASYNC_VOID")
                .traceId(trace.mdcContext().get(TraceContext.TRACE_ID)).requestId(trace.requestId())
                .threadName(Thread.currentThread().getName()).methodName(methodName).observedAt(Instant.now())
                .cause(failure).logEligible(eligible).suppressedCount(eligible ? suppressed.getAndSet(0) : 0).build();
            ValidationUtils checker = validation.getIfAvailable();
            if (checker != null) { checker.validate(report, DtpTaskFailureBO.Report.class); }
            handlers.getObject().handle(report);
        } catch (Throwable reportingFailure) {
            if (reportingFailure instanceof VirtualMachineError fatal) { throw fatal; }
            if (reportingFailure instanceof ThreadDeath fatal) { throw fatal; }
            // 此处没有可证明的池身份，不能制造完整的池任务/指标记录。
            try { ExceptionLogger.log(log, reportingFailure, Map.of("source", "ASYNC_REPORTER", "methodName", methodName)); }
            catch (RuntimeException ignored) { /* 原void结果不能被诊断后端故障替换。 */ }
        }
    }
}
