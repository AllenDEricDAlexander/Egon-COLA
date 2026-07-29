package top.egon.cola.component.accessguard.execution.async;

import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.core.GuardExecutionResult;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.PreparedGuardExecution;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public final class CompletionStageGuardExecutor {

    private final GuardEngine engine;

    public CompletionStageGuardExecutor(GuardEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    public <T> CompletionStage<T> guard(GuardInvocation invocation) {
        PreparedGuardExecution prepared;
        try {
            prepared = engine.prepare(Objects.requireNonNull(invocation, "invocation"));
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }
        if (!prepared.admitted()) {
            prepared.stage("admission", prepared.admission());
            return resolve(prepared, prepared::resolveAdmission);
        }
        prepared.stage("admission", prepared.admission());

        CompletionStage<?> source;
        try {
            Object value = invocation.continuation().execute();
            if (!(value instanceof CompletionStage<?> stage)) {
                throw new IllegalStateException("CompletionStage method returned " + typeName(value));
            }
            source = applyTimeout(stage, prepared.execution().timeLimit());
        } catch (Throwable throwable) {
            return resolve(prepared, () -> prepared.resolveFailure(
                    GuardDecision.BUSINESS_EXCEPTION, "BUSINESS_EXCEPTION"));
        }

        CompletionStage<CompletionStage<T>> composed = source.handle((value, failure) -> {
                    if (failure == null) {
                        return CompletionStageGuardExecutor.<T>completed(prepared, prepared.complete(value));
                    }
                    Throwable cause = unwrap(failure);
                    if (cause instanceof CancellationException) {
                        prepared.cancel();
                        return CompletableFuture.<T>failedFuture(cause);
                    }
                    if (cause instanceof AccessGuardRejectedException rejected) {
                        prepared.finish(rejected.outcome());
                        return CompletableFuture.<T>failedFuture(cause);
                    }
                    if (cause instanceof TimeoutException) {
                        return CompletionStageGuardExecutor.<T>resolve(prepared, () -> prepared.resolveFailure(
                                GuardDecision.TIME_LIMIT_EXCEEDED, "TIME_LIMIT_EXCEEDED"));
                    }
                    return CompletionStageGuardExecutor.<T>resolve(prepared, () -> prepared.resolveFailure(
                            GuardDecision.BUSINESS_EXCEPTION, "BUSINESS_EXCEPTION"));
                });
        return composed.thenCompose(Function.identity());
    }

    private static CompletionStage<?> applyTimeout(
            CompletionStage<?> source,
            ExecutionConfig.TimeLimitConfig timeLimit
    ) {
        if (!timeLimit.enabled() || timeLimit.mode() != TimeLimitMode.ENFORCE) {
            return source;
        }
        return source.toCompletableFuture().orTimeout(timeLimit.timeout().toNanos(), TimeUnit.NANOSECONDS);
    }

    private static <T> CompletionStage<T> resolve(
            PreparedGuardExecution prepared,
            Resolution resolution
    ) {
        try {
            return completed(prepared, resolution.resolve());
        } catch (AccessGuardRejectedException exception) {
            prepared.finish(exception.outcome());
            return CompletableFuture.failedFuture(exception);
        } catch (Throwable throwable) {
            prepared.finishResolutionFailure(prepared.admission());
            return CompletableFuture.failedFuture(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> CompletionStage<T> completed(
            PreparedGuardExecution prepared,
            GuardExecutionResult<Object> result
    ) {
        Object value = result.value();
        if (value instanceof CompletionStage<?> rawStage) {
            CompletionStage<T> typedStage = (CompletionStage<T>) rawStage;
            return typedStage.whenComplete((ignored, failure) -> finish(prepared, result, failure));
        }
        CompletionStage<T> completed = CompletableFuture.completedFuture((T) value);
        return completed.whenComplete((ignored, failure) -> finish(prepared, result, failure));
    }

    private static void finish(
            PreparedGuardExecution prepared,
            GuardExecutionResult<Object> result,
            Throwable failure
    ) {
        if (failure == null) {
            prepared.finish(result);
            return;
        }
        Throwable cause = unwrap(failure);
        if (cause instanceof CancellationException) {
            prepared.cancel();
        } else if (cause instanceof AccessGuardRejectedException rejected) {
            prepared.finish(rejected.outcome());
        } else {
            prepared.finishResolutionFailure(result.outcome());
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        if ((throwable instanceof CompletionException || throwable instanceof ExecutionException)
                && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    private static String typeName(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    @FunctionalInterface
    private interface Resolution {

        GuardExecutionResult<Object> resolve() throws Throwable;
    }
}
