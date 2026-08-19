package top.egon.cola.component.gateway.engine.common.provider.adapter;

import top.egon.cola.component.gateway.engine.common.provider.domain.ActiveHealthProbePolicy;
import top.egon.cola.component.gateway.engine.common.provider.service.ProviderActiveHealthProbe;

import io.netty.handler.codec.http.HttpMethod;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 中文说明：{@code HttpProviderActiveHealthProbe} 是类型，位于当前 Gateway 模块的相关包中，负责Http提供方Active健康Probe相关的职责与边界。
 * English summary: {@code HttpProviderActiveHealthProbe} is a type in the current Gateway module; it owns the http provider active health probe-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class HttpProviderActiveHealthProbe
        implements ProviderActiveHealthProbe {

    /**
     * 中文说明：保存 客户端 对应的状态、依赖或配置值；字段类型为 {@code HttpClient}，由 {@code HttpProviderActiveHealthProbe} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by client; its type is {@code HttpClient}, and {@code HttpProviderActiveHealthProbe} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code HttpProviderActiveHealthProbe} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpProviderActiveHealthProbe}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final HttpClient client;

    /**
     * 中文说明：创建 {@code HttpProviderActiveHealthProbe} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code HttpProviderActiveHealthProbe} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param client 参数 客户端；parameter client。
     */
    public HttpProviderActiveHealthProbe(HttpClient client) {
        this.client = client;
    }

    /**
     * 中文说明：执行 probe 操作；该方法是 {@code HttpProviderActiveHealthProbe} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the probe operation; this method is the invocation entry point on {@code HttpProviderActiveHealthProbe} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpProviderActiveHealthProbe.probe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @param policy 参数 策略；parameter policy。
     * @return 返回 probe 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Boolean> probe(
            ProviderInstance instance,
            ActiveHealthProbePolicy policy) {
        return client.request(HttpMethod.valueOf(
                        policy.httpMethod(instance)
                ))
                .uri(uri(instance, policy.httpPath(instance)))
                .responseSingle((response, content) -> content.then(
                        Mono.fromSupplier(() -> policy
                                .httpSuccessStatuses(instance)
                                .contains(response.status().code()))
                ));
    }

    /**
     * 中文说明：执行 uri 操作；该方法是 {@code HttpProviderActiveHealthProbe} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the uri operation; this method is the invocation entry point on {@code HttpProviderActiveHealthProbe} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpProviderActiveHealthProbe.uri(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @param path 参数 path；parameter path。
     * @return 返回 uri 的处理结果；returns the result of the operation.
     */
    private String uri(ProviderInstance instance, String path) {
        try {
            return new URI(
                    instance.secure() ? "https" : "http",
                    null,
                    instance.host(),
                    instance.port(),
                    path,
                    null,
                    null
            ).toASCIIString();
        } catch (URISyntaxException invalid) {
            throw new IllegalArgumentException(
                    "invalid provider active health URI",
                    invalid
            );
        }
    }
}
