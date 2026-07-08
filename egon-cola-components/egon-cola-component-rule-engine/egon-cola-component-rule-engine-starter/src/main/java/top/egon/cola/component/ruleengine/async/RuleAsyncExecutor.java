package top.egon.cola.component.ruleengine.async;

import top.egon.cola.component.ruleengine.context.RuleContext;

import java.time.Duration;
import java.util.concurrent.Callable;

public interface RuleAsyncExecutor {

    <T> T load(Callable<T> loader, RuleContext context, Duration timeout);

    <T> void loadToContext(String key, Callable<T> loader, RuleContext context, Duration timeout);
}
