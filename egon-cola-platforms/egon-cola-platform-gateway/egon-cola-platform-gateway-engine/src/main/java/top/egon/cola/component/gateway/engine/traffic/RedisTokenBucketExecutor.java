package top.egon.cola.component.gateway.engine.traffic;

import java.util.List;

/**
 * 中文说明：{@code RedisTokenBucketExecutor} 是接口契约，位于当前 Gateway 模块的相关包中，负责RedisTokenBucketExecutor相关的职责与边界。
 * English summary: {@code RedisTokenBucketExecutor} is an interface contract in the current Gateway module; it owns the redis token bucket executor-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface RedisTokenBucketExecutor {

    /**
     * 中文说明：执行 execute 操作；该方法是 {@code RedisTokenBucketExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute operation; this method is the invocation entry point on {@code RedisTokenBucketExecutor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisTokenBucketExecutor.execute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param script 参数 script；parameter script。
     * @param keys 参数 keys；parameter keys。
     * @param arguments 参数 arguments；parameter arguments。
     * @return 返回 execute 的处理结果；returns the result of the operation.
     */
    List<Long> execute(
            String script,
            List<String> keys,
            List<String> arguments);
}
