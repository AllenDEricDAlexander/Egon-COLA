package top.egon.cola.component.gateway.engine.common.traffic.adapter;

import top.egon.cola.component.gateway.engine.common.traffic.service.RedisTokenBucketExecutor;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.List;
import java.util.Objects;

/**
 * 中文说明：{@code RedissonRedisTokenBucketExecutor} 是类型，位于当前 Gateway 模块的相关包中，负责RedissonRedisTokenBucketExecutor相关的职责与边界。
 * English summary: {@code RedissonRedisTokenBucketExecutor} is a type in the current Gateway module; it owns the redisson redis token bucket executor-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RedissonRedisTokenBucketExecutor
        implements RedisTokenBucketExecutor {

    /**
     * 中文说明：保存 redisson 对应的状态、依赖或配置值；字段类型为 {@code RedissonClient}，由 {@code RedissonRedisTokenBucketExecutor} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by redisson; its type is {@code RedissonClient}, and {@code RedissonRedisTokenBucketExecutor} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RedissonRedisTokenBucketExecutor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RedissonRedisTokenBucketExecutor}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RedissonClient redisson;

    /**
     * 中文说明：创建 {@code RedissonRedisTokenBucketExecutor} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RedissonRedisTokenBucketExecutor} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param redisson 参数 redisson；parameter redisson。
     */
    public RedissonRedisTokenBucketExecutor(RedissonClient redisson) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
    }

    /**
     * 中文说明：执行 execute 操作；该方法是 {@code RedissonRedisTokenBucketExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute operation; this method is the invocation entry point on {@code RedissonRedisTokenBucketExecutor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedissonRedisTokenBucketExecutor.execute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param script 参数 script；parameter script。
     * @param keys 参数 keys；parameter keys。
     * @param arguments 参数 arguments；parameter arguments。
     * @return 返回 execute 的处理结果；returns the result of the operation.
     */
    @Override
    public List<Long> execute(
            String script,
            List<String> keys,
            List<String> arguments) {
        List<?> result = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.MULTI,
                keys.stream().map(value -> (Object) value).toList(),
                arguments.toArray()
        );
        if (result == null) {
            return null;
        }
        return result.stream()
                .map(value -> value instanceof Number number
                        ? number.longValue()
                        : Long.parseLong(value.toString()))
                .toList();
    }
}
