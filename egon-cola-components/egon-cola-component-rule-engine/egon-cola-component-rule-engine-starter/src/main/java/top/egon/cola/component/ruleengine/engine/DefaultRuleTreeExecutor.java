package top.egon.cola.component.ruleengine.engine;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.listener.RuleExecutionListener;
import top.egon.cola.component.ruleengine.listener.RuleExecutionListenerComposite;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.result.RuleStatus;
import top.egon.cola.component.ruleengine.trace.NodeTrace;
import top.egon.cola.component.ruleengine.trace.RuleTrace;
import top.egon.cola.component.ruleengine.trace.RuleTraceRecorder;
import top.egon.cola.component.ruleengine.tree.RouteDecision;
import top.egon.cola.component.ruleengine.tree.RuleNode;
import top.egon.cola.component.ruleengine.tree.RuleTree;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultRuleTreeExecutor implements RuleTreeExecutor {

    private final boolean traceEnabled;

    private final boolean throwException;

    private final RuleExecutionListener listener;

    public DefaultRuleTreeExecutor(boolean traceEnabled, boolean throwException) {
        this(traceEnabled, throwException, new RuleExecutionListenerComposite(List.of(), true));
    }

    public DefaultRuleTreeExecutor(boolean traceEnabled, boolean throwException, RuleExecutionListener listener) {
        this.traceEnabled = traceEnabled;
        this.throwException = throwException;
        this.listener = listener == null ? new RuleExecutionListenerComposite(List.of(), true) : listener;
    }

    @Override
    public <T, R> RuleResult<R> execute(RuleTree<T, R> ruleTree, T request, RuleContext context) {
        RuleContext actualContext = context == null ? RuleContext.create() : context;
        RuleTraceRecorder recorder = new RuleTraceRecorder(traceEnabled);
        Instant start = Instant.now();
        String ruleCode = ruleTree == null ? "empty" : ruleTree.code();
        listener.beforeEngineExecute("TREE", ruleCode, actualContext);
        if (ruleTree == null || ruleTree.root() == null) {
            RuleTrace trace = recorder.finish("empty", "empty", "TREE", actualContext, RuleStatus.EMPTY_TREE, null);
            RuleResult<R> result = RuleResult.<R>failure(RuleStatus.EMPTY_TREE, RuleStatus.EMPTY_TREE.getMessage(), null)
                    .withTrace(trace)
                    .withCostMillis(Duration.between(start, Instant.now()).toMillis());
            return complete(ruleCode, actualContext, result);
        }
        actualContext.maxSteps(ruleTree.maxSteps()).timeout(Duration.ofMillis(ruleTree.timeoutMillis()));
        try {
            return runTree(ruleTree, request, actualContext, recorder, start);
        } catch (RuntimeException ex) {
            actualContext.addError(ex);
            listener.onEngineError(ruleCode, actualContext, ex);
            if (throwException) {
                throw ex;
            }
            RuleTrace trace = recorder.finish(ruleTree.code(), ruleTree.name(), "TREE", actualContext, RuleStatus.NODE_ERROR, ex);
            RuleResult<R> result = RuleResult.<R>failure(RuleStatus.NODE_ERROR, ex.getMessage(), ex)
                    .withTrace(trace)
                    .withCostMillis(Duration.between(start, Instant.now()).toMillis());
            return complete(ruleCode, actualContext, result);
        }
    }

    private <T, R> RuleResult<R> runTree(RuleTree<T, R> tree, T request, RuleContext context,
                                         RuleTraceRecorder recorder, Instant start) {
        RuleNode<T, R> current = tree.root();
        Map<String, Integer> visits = new HashMap<>();
        RuleResult<R> last = RuleResult.success(null);
        while (current != null) {
            if (context.isTimeout()) {
                listener.onTimeout(tree.code(), context);
                RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, RuleStatus.TIMEOUT, null);
                RuleResult<R> result = RuleResult.<R>timeout(RuleStatus.TIMEOUT.getMessage())
                        .withTrace(trace)
                        .withCostMillis(Duration.between(start, Instant.now()).toMillis());
                return complete(tree.code(), context, result);
            }
            context.incrementStep();
            if (context.isExceededMaxSteps()) {
                listener.onMaxStepsExceeded(tree.code(), context);
                RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, RuleStatus.MAX_STEPS_EXCEEDED, null);
                RuleResult<R> result = RuleResult.<R>maxStepsExceeded(RuleStatus.MAX_STEPS_EXCEEDED.getMessage())
                        .withTrace(trace)
                        .withCostMillis(Duration.between(start, Instant.now()).toMillis());
                return complete(tree.code(), context, result);
            }
            int order = context.getStepCount();
            int visitCount = visits.merge(current.code(), 1, Integer::sum);
            Instant nodeStart = Instant.now();
            context.enterNode(current.code());
            listener.beforeNodeExecute(current.code(), context);
            try {
                last = current.execute(request, context);
            } catch (RuntimeException ex) {
                listener.onNodeError(current.code(), context, ex);
                throw ex;
            }
            listener.afterNodeExecute(current.code(), context, last);
            listener.beforeRoute(current.code(), context);
            RouteDecision route;
            try {
                route = current.route(request, context);
            } catch (RuntimeException ex) {
                listener.onNodeError(current.code(), context, ex);
                throw ex;
            }
            route = route == null ? RouteDecision.noRoute("route decision is null") : route;
            listener.afterRoute(current.code(), context, route);
            Instant nodeEnd = Instant.now();
            recorder.addNodeTrace(new NodeTrace(current.code(), current.name(), current.type(), order, visitCount,
                    nodeStart, nodeEnd, Duration.between(nodeStart, nodeEnd).toMillis(), route.routeTo(),
                    route.getReason(), last.getStatus(), null));
            if (!last.isSuccess() || context.isStopped() || route.isEnd()) {
                RuleResult<R> result = context.isStopped() && last.isSuccess()
                        ? RuleResult.stop(RuleStatus.STOPPED.getCode(), RuleStatus.STOPPED.getMessage(), last.getData())
                        : last;
                RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, result.getStatus(), null);
                RuleResult<R> completed = result.withTrace(trace)
                        .withHitNode(current.code())
                        .withCostMillis(Duration.between(start, Instant.now()).toMillis());
                if (result.getStatus() == RuleStatus.STOPPED || context.isStopped()) {
                    listener.onStop(current.code(), context, completed);
                }
                return complete(tree.code(), context, completed);
            }
            if (route.isNoRoute()) {
                RuleNode<T, R> defaultNode = resolveDefault(tree);
                if (defaultNode == null) {
                    RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, RuleStatus.NO_ROUTE, null);
                    RuleResult<R> result = RuleResult.<R>noRoute(route.getReason())
                            .withTrace(trace)
                            .withCostMillis(Duration.between(start, Instant.now()).toMillis());
                    return complete(tree.code(), context, result);
                }
                current = defaultNode;
            } else {
                current = resolveRoute(tree, route);
                if (current == null) {
                    RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, RuleStatus.NO_ROUTE, null);
                    RuleResult<R> result = RuleResult.<R>noRoute(RuleStatus.NO_ROUTE.getMessage())
                            .withTrace(trace)
                            .withCostMillis(Duration.between(start, Instant.now()).toMillis());
                    return complete(tree.code(), context, result);
                }
            }
        }
        RuleTrace trace = recorder.finish(tree.code(), tree.name(), "TREE", context, RuleStatus.NO_ROUTE, null);
        RuleResult<R> result = RuleResult.<R>noRoute(RuleStatus.NO_ROUTE.getMessage())
                .withTrace(trace)
                .withCostMillis(Duration.between(start, Instant.now()).toMillis());
        return complete(tree.code(), context, result);
    }

    @SuppressWarnings("unchecked")
    private <T, R> RuleNode<T, R> resolveRoute(RuleTree<T, R> tree, RouteDecision route) {
        if (route.getTargetNode() != null) {
            return (RuleNode<T, R>) route.getTargetNode();
        }
        return tree.nodes().get(route.getTargetCode());
    }

    private <T, R> RuleNode<T, R> resolveDefault(RuleTree<T, R> tree) {
        if (tree.defaultEndNodeCode() == null || tree.defaultEndNodeCode().isBlank()) {
            return null;
        }
        return tree.nodes().get(tree.defaultEndNodeCode());
    }

    private <R> RuleResult<R> complete(String ruleCode, RuleContext context, RuleResult<R> result) {
        listener.afterEngineExecute("TREE", ruleCode, context, result);
        return result;
    }
}
