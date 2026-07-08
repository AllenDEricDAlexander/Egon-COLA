package top.egon.cola.component.ruleengine.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.tree.RouteDecision;

import java.util.List;
import java.util.function.Consumer;

public class RuleExecutionListenerComposite implements RuleExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(RuleExecutionListenerComposite.class);

    private final List<RuleExecutionListener> listeners;

    private final boolean ignoreErrors;

    public RuleExecutionListenerComposite(List<RuleExecutionListener> listeners, boolean ignoreErrors) {
        this.listeners = listeners == null ? List.of() : List.copyOf(listeners);
        this.ignoreErrors = ignoreErrors;
    }

    @Override
    public void beforeEngineExecute(String modelType, String ruleCode, RuleContext context) {
        each(listener -> listener.beforeEngineExecute(modelType, ruleCode, context));
    }

    @Override
    public void afterEngineExecute(String modelType, String ruleCode, RuleContext context, RuleResult<?> result) {
        each(listener -> listener.afterEngineExecute(modelType, ruleCode, context, result));
    }

    @Override
    public void beforeNodeExecute(String nodeCode, RuleContext context) {
        each(listener -> listener.beforeNodeExecute(nodeCode, context));
    }

    @Override
    public void afterNodeExecute(String nodeCode, RuleContext context, RuleResult<?> result) {
        each(listener -> listener.afterNodeExecute(nodeCode, context, result));
    }

    @Override
    public void beforeRoute(String nodeCode, RuleContext context) {
        each(listener -> listener.beforeRoute(nodeCode, context));
    }

    @Override
    public void afterRoute(String nodeCode, RuleContext context, RouteDecision decision) {
        each(listener -> listener.afterRoute(nodeCode, context, decision));
    }

    @Override
    public void onNodeError(String nodeCode, RuleContext context, Throwable error) {
        each(listener -> listener.onNodeError(nodeCode, context, error));
    }

    @Override
    public void onEngineError(String ruleCode, RuleContext context, Throwable error) {
        each(listener -> listener.onEngineError(ruleCode, context, error));
    }

    @Override
    public void onStop(String nodeCode, RuleContext context, RuleResult<?> result) {
        each(listener -> listener.onStop(nodeCode, context, result));
    }

    @Override
    public void onTimeout(String ruleCode, RuleContext context) {
        each(listener -> listener.onTimeout(ruleCode, context));
    }

    @Override
    public void onMaxStepsExceeded(String ruleCode, RuleContext context) {
        each(listener -> listener.onMaxStepsExceeded(ruleCode, context));
    }

    private void each(Consumer<RuleExecutionListener> consumer) {
        for (RuleExecutionListener listener : listeners) {
            try {
                consumer.accept(listener);
            } catch (RuntimeException ex) {
                if (!ignoreErrors) {
                    throw ex;
                }
                log.warn("Rule execution listener failed: {}", ex.getMessage(), ex);
            }
        }
    }
}
