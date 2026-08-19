package top.egon.cola.component.ruleengine.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ruleengine.context.RuleContext;
import top.egon.cola.component.ruleengine.result.RuleResult;

public class LoggingRuleExecutionListener implements RuleExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingRuleExecutionListener.class);

    @Override
    public void afterEngineExecute(String modelType, String ruleCode, RuleContext context, RuleResult<?> result) {
        log.info("rule engine executed modelType={} ruleCode={} traceId={} requestId={} status={} costMillis={}",
                modelType, ruleCode, context.getTraceId(), context.getRequestId(), result.getStatus(),
                result.getCostMillis());
    }
}
