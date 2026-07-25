package top.egon.cola.component.gateway.engine.traffic;

import java.util.List;

@FunctionalInterface
public interface RedisTokenBucketExecutor {

    List<Long> execute(
            String script,
            List<String> keys,
            List<String> arguments);
}
