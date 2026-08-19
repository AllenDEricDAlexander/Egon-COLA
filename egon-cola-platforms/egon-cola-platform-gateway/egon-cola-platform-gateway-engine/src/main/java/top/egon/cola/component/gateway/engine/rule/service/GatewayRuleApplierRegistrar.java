package top.egon.cola.component.gateway.engine.rule.service;

import top.egon.cola.component.gateway.engine.rule.repository.GatewayRuleChunkStore;

import top.egon.cola.component.ddc.api.refresh.DdcConfigApplierRegistry;

import java.util.Objects;

/**
 * 中文说明：{@code GatewayRuleApplierRegistrar} 是类型，位于当前 Gateway 模块的相关包中，负责网关规则ApplierRegistrar相关的职责与边界。
 * English summary: {@code GatewayRuleApplierRegistrar} is a type in the current Gateway module; it owns the gateway rule applier registrar-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayRuleApplierRegistrar {

    /**
     * 中文说明：创建 {@code GatewayRuleApplierRegistrar} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayRuleApplierRegistrar} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    private GatewayRuleApplierRegistrar() {
    }

    /**
     * 中文说明：执行 register 操作；该方法是 {@code GatewayRuleApplierRegistrar} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the register operation; this method is the invocation entry point on {@code GatewayRuleApplierRegistrar} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleApplierRegistrar.register(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param registry 参数 注册表；parameter registry。
     * @param activation 参数 activation；parameter activation。
     * @param chunks 参数 chunks；parameter chunks。
     */
    public static void register(
            DdcConfigApplierRegistry registry,
            GatewayRuleActivationApplier activation,
            GatewayRuleChunkStore chunks) {
        Objects.requireNonNull(registry, "registry").registerExact(
                GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY,
                Objects.requireNonNull(activation, "activation")
        );
        registry.registerPrefix(
                "gateway.rules.chunk.",
                Objects.requireNonNull(chunks, "chunks")
        );
    }
}
