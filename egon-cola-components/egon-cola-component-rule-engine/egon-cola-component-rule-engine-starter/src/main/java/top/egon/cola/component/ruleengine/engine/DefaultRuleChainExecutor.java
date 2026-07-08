package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.chain.ChainHandler;
import top.egon.cola.component.ruleengine.chain.RuleChain;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.result.RuleStatus;
import top.egon.cola.component.ruleengine.trace.NodeTrace;
import top.egon.cola.component.ruleengine.trace.RuleTrace;
import top.egon.cola.component.ruleengine.trace.RuleTraceRecorder;
import top.egon.cola.component.ruleengine.tree.NodeType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class DefaultRuleChainExecutor implements RuleChainExecutor {

    private final boolean traceEnabled;

    private final boolean throwException;

    public DefaultRuleChainExecutor(boolean traceEnabled, boolean throwException) {
        this.traceEnabled = traceEnabled;
        this.throwException = throwException;
    }

    @Override
    public <T, R> RuleResult<R> execute(RuleChain<T, R> ruleChain, T request, RuleContext context) {
        RuleContext actualContext = context == null ? RuleContext.create() : context;
        RuleTraceRecorder recorder = new RuleTraceRecorder(traceEnabled);
        Instant start = Instant.now();
        if (ruleChain == null || ruleChain.handlers().isEmpty()) {
            RuleTrace trace = recorder.finish("empty", "empty", "CHAIN", actualContext, RuleStatus.EMPTY_CHAIN, null);
            return RuleResult.<R>failure(RuleStatus.EMPTY_CHAIN, RuleStatus.EMPTY_CHAIN.getMessage(), null)
                    .withTrace(trace)
                    .withCostMillis(Duration.between(start, Instant.now()).toMillis());
        }
        actualContext.maxSteps(ruleChain.maxSteps()).timeout(Duration.ofMillis(ruleChain.timeoutMillis()));
        try {
            return runHandlers(ruleChain, request, actualContext, recorder, start);
        } catch (RuntimeException ex) {
            actualContext.addError(ex);
            if (throwException) {
                throw ex;
            }
            RuleTrace trace = recorder.finish(ruleChain.code(), ruleChain.name(), "CHAIN", actualContext, RuleStatus.NODE_ERROR, ex);
            return RuleResult.<R>failure(RuleStatus.NODE_ERROR, ex.getMessage(), ex)
                    .withTrace(trace)
                    .withCostMillis(Duration.between(start, Instant.now()).toMillis());
        }
    }

    private <T, R> RuleResult<R> runHandlers(RuleChain<T, R> ruleChain, T request, RuleContext context,
                                             RuleTraceRecorder recorder, Instant start) {
        RuleResult<R> last = RuleResult.success(null);
        List<ChainHandler<T, R>> handlers = ruleChain.handlers();
        for (int i = 0; i < handlers.size(); i++) {
            if (context.isTimeout()) {
                RuleTrace trace = recorder.finish(ruleChain.code(), ruleChain.name(), "CHAIN", context, RuleStatus.TIMEOUT, null);
                return RuleResult.<R>timeout(RuleStatus.TIMEOUT.getMessage())
                        .withTrace(trace)
                        .withCostMillis(Duration.between(start, Instant.now()).toMillis());
            }
            context.incrementStep();
            if (context.isExceededMaxSteps()) {
                RuleTrace trace = recorder.finish(ruleChain.code(), ruleChain.name(), "CHAIN", context, RuleStatus.MAX_STEPS_EXCEEDED, null);
                return RuleResult.<R>maxStepsExceeded(RuleStatus.MAX_STEPS_EXCEEDED.getMessage())
                        .withTrace(trace)
                        .withCostMillis(Duration.between(start, Instant.now()).toMillis());
            }
            String nodeCode = "handler-" + (i + 1);
            Instant nodeStart = Instant.now();
            context.enterNode(nodeCode);
            last = handlers.get(i).handle(request, context);
            Instant nodeEnd = Instant.now();
            recorder.addNodeTrace(new NodeTrace(nodeCode, nodeCode, NodeType.BIZ, i + 1, 1, nodeStart, nodeEnd,
                    Duration.between(nodeStart, nodeEnd).toMillis(), null, null, last.getStatus(), null));
            if (!last.isSuccess() || context.isStopped() || !context.isProceed()) {
                RuleResult<R> result = context.isStopped() && last.isSuccess()
                        ? RuleResult.stop(RuleStatus.STOPPED.getCode(), RuleStatus.STOPPED.getMessage(), last.getData())
                        : last;
                RuleTrace trace = recorder.finish(ruleChain.code(), ruleChain.name(), "CHAIN", context, result.getStatus(), null);
                return result.withTrace(trace)
                        .withStoppedNode(nodeCode)
                        .withCostMillis(Duration.between(start, Instant.now()).toMillis());
            }
        }
        RuleTrace trace = recorder.finish(ruleChain.code(), ruleChain.name(), "CHAIN", context, last.getStatus(), null);
        return last.withTrace(trace).withCostMillis(Duration.between(start, Instant.now()).toMillis());
    }
}
