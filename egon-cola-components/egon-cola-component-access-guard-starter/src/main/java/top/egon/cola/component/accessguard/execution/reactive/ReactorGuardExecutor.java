package top.egon.cola.component.accessguard.execution.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import top.egon.cola.component.accessguard.api.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.core.GuardExecutionResult;
import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.PreparedGuardExecution;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class ReactorGuardExecutor implements ReactiveGuardExecutor {

    private final GuardEngine engine;

    public ReactorGuardExecutor(GuardEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public boolean supports(Class<?> returnType) {
        return Mono.class.isAssignableFrom(returnType) || Flux.class.isAssignableFrom(returnType);
    }

    @Override
    public Object guard(GuardInvocation invocation, Class<?> returnType) {
        Objects.requireNonNull(invocation, "invocation");
        Objects.requireNonNull(returnType, "returnType");
        if (Mono.class.isAssignableFrom(returnType)) {
            return Mono.defer(() -> prepareMono(invocation));
        }
        if (Flux.class.isAssignableFrom(returnType)) {
            return Flux.defer(() -> prepareFlux(invocation));
        }
        throw new IllegalArgumentException("Unsupported reactive return type " + returnType.getName());
    }

    private Mono<?> prepareMono(GuardInvocation invocation) {
        PreparedGuardExecution prepared;
        try {
            prepared = engine.prepare(invocation);
        } catch (Throwable throwable) {
            return Mono.error(throwable);
        }
        if (!prepared.admitted()) {
            return withCancellation(prepared, resolveMono(() -> prepared.resolveAdmission()));
        }
        Object value;
        try {
            value = invocation.continuation().execute();
        } catch (Throwable throwable) {
            return withCancellation(prepared, resolveMono(() -> prepared.resolveFailure(
                    GuardDecision.BUSINESS_EXCEPTION, "BUSINESS_EXCEPTION")));
        }
        if (!(value instanceof Mono<?> source)) {
            return withCancellation(prepared, resolveMono(() -> prepared.resolveFailure(
                    GuardDecision.BUSINESS_EXCEPTION, "BUSINESS_EXCEPTION")));
        }
        Mono<Object> timed = mono(applyTimeout(source, prepared.execution().timeLimit()));
        return withCancellation(prepared, timed
                .doOnSuccess(prepared::complete)
                .onErrorResume(error -> resolveMonoFailure(prepared, error)));
    }

    private Flux<?> prepareFlux(GuardInvocation invocation) {
        PreparedGuardExecution prepared;
        try {
            prepared = engine.prepare(invocation);
        } catch (Throwable throwable) {
            return Flux.error(throwable);
        }
        if (!prepared.admitted()) {
            return withCancellation(prepared, resolveFlux(() -> prepared.resolveAdmission()));
        }
        Object value;
        try {
            value = invocation.continuation().execute();
        } catch (Throwable throwable) {
            return withCancellation(prepared, resolveFlux(() -> prepared.resolveFailure(
                    GuardDecision.BUSINESS_EXCEPTION, "BUSINESS_EXCEPTION")));
        }
        if (!(value instanceof Flux<?> source)) {
            return withCancellation(prepared, resolveFlux(() -> prepared.resolveFailure(
                    GuardDecision.BUSINESS_EXCEPTION, "BUSINESS_EXCEPTION")));
        }
        Flux<Object> timed = flux(applyTimeout(source, prepared.execution().timeLimit()));
        return withCancellation(prepared, timed
                .doOnComplete(() -> prepared.complete(null))
                .onErrorResume(error -> resolveFluxFailure(prepared, error)));
    }

    private Mono<Object> resolveMonoFailure(PreparedGuardExecution prepared, Throwable error) {
        if (error instanceof AccessGuardRejectedException) {
            return Mono.error(error);
        }
        GuardDecision decision = error instanceof TimeoutException
                ? GuardDecision.TIME_LIMIT_EXCEEDED
                : GuardDecision.BUSINESS_EXCEPTION;
        String code = decision == GuardDecision.TIME_LIMIT_EXCEEDED
                ? "TIME_LIMIT_EXCEEDED"
                : "BUSINESS_EXCEPTION";
        return resolveMono(() -> prepared.resolveFailure(decision, code));
    }

    private Flux<Object> resolveFluxFailure(PreparedGuardExecution prepared, Throwable error) {
        if (error instanceof AccessGuardRejectedException) {
            return Flux.error(error);
        }
        GuardDecision decision = error instanceof TimeoutException
                ? GuardDecision.TIME_LIMIT_EXCEEDED
                : GuardDecision.BUSINESS_EXCEPTION;
        String code = decision == GuardDecision.TIME_LIMIT_EXCEEDED
                ? "TIME_LIMIT_EXCEEDED"
                : "BUSINESS_EXCEPTION";
        return resolveFlux(() -> prepared.resolveFailure(decision, code));
    }

    private static Mono<Object> resolveMono(Resolution resolution) {
        try {
            Object value = resolution.resolve().value();
            if (value instanceof Mono<?> mono) {
                return mono(mono);
            }
            if (value instanceof Flux<?>) {
                return Mono.error(new IllegalStateException("Mono guard fallback returned Flux"));
            }
            return Mono.justOrEmpty(value);
        } catch (Throwable throwable) {
            return Mono.error(throwable);
        }
    }

    private static Flux<Object> resolveFlux(Resolution resolution) {
        try {
            Object value = resolution.resolve().value();
            if (value instanceof Flux<?> flux) {
                return flux(flux);
            }
            if (value instanceof Mono<?>) {
                return Flux.error(new IllegalStateException("Flux guard fallback returned Mono"));
            }
            return value == null ? Flux.empty() : Flux.just(value);
        } catch (Throwable throwable) {
            return Flux.error(throwable);
        }
    }

    private static <T> Mono<T> applyTimeout(Mono<T> source, ExecutionConfig.TimeLimitConfig timeLimit) {
        return timeLimit.enabled() && timeLimit.mode() == TimeLimitMode.ENFORCE
                ? source.timeout(timeLimit.timeout())
                : source;
    }

    private static <T> Flux<T> applyTimeout(Flux<T> source, ExecutionConfig.TimeLimitConfig timeLimit) {
        return timeLimit.enabled() && timeLimit.mode() == TimeLimitMode.ENFORCE
                ? source.timeout(timeLimit.timeout())
                : source;
    }

    @SuppressWarnings("unchecked")
    private static Mono<Object> mono(Mono<?> source) {
        return (Mono<Object>) source;
    }

    @SuppressWarnings("unchecked")
    private static Flux<Object> flux(Flux<?> source) {
        return (Flux<Object>) source;
    }

    private static Mono<?> withCancellation(PreparedGuardExecution prepared, Mono<?> source) {
        return source.doFinally(signal -> cancelIfNecessary(prepared, signal));
    }

    private static Flux<?> withCancellation(PreparedGuardExecution prepared, Flux<?> source) {
        return source.doFinally(signal -> cancelIfNecessary(prepared, signal));
    }

    private static void cancelIfNecessary(PreparedGuardExecution prepared, SignalType signal) {
        if (signal == SignalType.CANCEL) {
            prepared.cancel();
        }
    }

    @FunctionalInterface
    private interface Resolution {

        GuardExecutionResult<Object> resolve() throws Throwable;
    }
}
