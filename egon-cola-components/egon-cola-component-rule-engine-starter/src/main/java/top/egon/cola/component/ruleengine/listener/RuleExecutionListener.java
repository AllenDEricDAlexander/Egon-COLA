package top.egon.cola.component.ruleengine.listener;

import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;
import top.egon.cola.component.ruleengine.tree.RouteDecision;

public interface RuleExecutionListener {

    default void beforeEngineExecute(String modelType, String ruleCode, RuleContext context) {
    }

    default void afterEngineExecute(String modelType, String ruleCode, RuleContext context, RuleResult<?> result) {
    }

    default void beforeNodeExecute(String nodeCode, RuleContext context) {
    }

    default void afterNodeExecute(String nodeCode, RuleContext context, RuleResult<?> result) {
    }

    default void beforeRoute(String nodeCode, RuleContext context) {
    }

    default void afterRoute(String nodeCode, RuleContext context, RouteDecision decision) {
    }

    default void onNodeError(String nodeCode, RuleContext context, Throwable error) {
    }

    default void onEngineError(String ruleCode, RuleContext context, Throwable error) {
    }

    default void onStop(String nodeCode, RuleContext context, RuleResult<?> result) {
    }

    default void onTimeout(String ruleCode, RuleContext context) {
    }

    default void onMaxStepsExceeded(String ruleCode, RuleContext context) {
    }
}
