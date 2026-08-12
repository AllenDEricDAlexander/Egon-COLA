package top.egon.cola.component.gateway.engine.discovery;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.engine.rpc.RpcProviderChannelCache;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：{@code RpcProviderActiveHealthProbe} 是类型，位于当前 Gateway 模块的相关包中，负责Rpc提供方Active健康Probe相关的职责与边界。
 * English summary: {@code RpcProviderActiveHealthProbe} is a type in the current Gateway module; it owns the rpc provider active health probe-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RpcProviderActiveHealthProbe
        implements ProviderActiveHealthProbe {

    /**
     * 中文说明：保存 channels 对应的状态、依赖或配置值；字段类型为 {@code RpcProviderChannelCache}，由 {@code RpcProviderActiveHealthProbe} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by channels; its type is {@code RpcProviderChannelCache}, and {@code RpcProviderActiveHealthProbe} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcProviderActiveHealthProbe} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderActiveHealthProbe}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RpcProviderChannelCache channels;

    /**
     * 中文说明：创建 {@code RpcProviderActiveHealthProbe} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcProviderActiveHealthProbe} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param channels 参数 channels；parameter channels。
     */
    public RpcProviderActiveHealthProbe(
            RpcProviderChannelCache channels) {
        this.channels = channels;
    }

    /**
     * 中文说明：执行 probe 操作；该方法是 {@code RpcProviderActiveHealthProbe} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the probe operation; this method is the invocation entry point on {@code RpcProviderActiveHealthProbe} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderActiveHealthProbe.probe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @param policy 参数 策略；parameter policy。
     * @return 返回 probe 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Boolean> probe(
            ProviderInstance instance,
            ActiveHealthProbePolicy policy) {
        return Mono.fromCallable(() -> check(instance, policy));
    }

    /**
     * 中文说明：执行 check 操作；该方法是 {@code RpcProviderActiveHealthProbe} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the check operation; this method is the invocation entry point on {@code RpcProviderActiveHealthProbe} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderActiveHealthProbe.check(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @param policy 参数 策略；parameter policy。
     * @return 返回 check 的处理结果；returns the result of the operation.
     */
    private boolean check(
            ProviderInstance instance,
            ActiveHealthProbePolicy policy) {
        try (RpcProviderChannelCache.ChannelHandle handle =
                     channels.acquire(instance)) {
            HealthCheckResponse response = HealthGrpc.newBlockingStub(
                            handle.channel()
                    )
                    .withDeadlineAfter(
                            policy.timeout().toMillis(),
                            TimeUnit.MILLISECONDS
                    )
                    .check(HealthCheckRequest.newBuilder()
                            .setService(policy.rpcServiceName(instance))
                            .build());
            return response.getStatus()
                    == HealthCheckResponse.ServingStatus.SERVING;
        } catch (StatusRuntimeException unavailable) {
            if (policy.rpcConnectFallback()
                    && unavailable.getStatus().getCode()
                    == Status.Code.UNIMPLEMENTED) {
                return connect(instance, policy);
            }
            return false;
        }
    }

    /**
     * 中文说明：执行 connect 操作；该方法是 {@code RpcProviderActiveHealthProbe} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the connect operation; this method is the invocation entry point on {@code RpcProviderActiveHealthProbe} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderActiveHealthProbe.connect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @param policy 参数 策略；parameter policy。
     * @return 返回 connect 的处理结果；returns the result of the operation.
     */
    private boolean connect(
            ProviderInstance instance,
            ActiveHealthProbePolicy policy) {
        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(
                            instance.host(),
                            instance.port()
                    ),
                    Math.toIntExact(Math.min(
                            Integer.MAX_VALUE,
                            policy.timeout().toMillis()
                    ))
            );
            return true;
        } catch (java.io.IOException failure) {
            return false;
        }
    }
}
