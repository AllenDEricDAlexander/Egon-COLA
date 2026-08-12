package top.egon.cola.component.gateway.admin.rule;

import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.model.management.DdcInstanceStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayDdcRulePublisher} 是类型，位于当前 Gateway 模块的相关包中，负责网关Ddc规则发布器相关的职责与边界。
 * English summary: {@code GatewayDdcRulePublisher} is a type in the current Gateway module; it owns the gateway ddc rule publisher-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayDdcRulePublisher {

    /**
     * 中文说明：表示 ACTIVECONFIG键 这一固定值；它属于 {@code GatewayDdcRulePublisher} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value active config key; it is a state, type, or protocol value of {@code GatewayDdcRulePublisher} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDdcRulePublisher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcRulePublisher}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String ACTIVE_CONFIG_KEY =
            GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY;

    /**
     * 中文说明：保存 客户端 对应的状态、依赖或配置值；字段类型为 {@code DdcManagementClient}，由 {@code GatewayDdcRulePublisher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by client; its type is {@code DdcManagementClient}, and {@code GatewayDdcRulePublisher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDdcRulePublisher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDdcRulePublisher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DdcManagementClient client;

    /**
     * 中文说明：创建 {@code GatewayDdcRulePublisher} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDdcRulePublisher} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param client 参数 客户端；parameter client。
     */
    public GatewayDdcRulePublisher(DdcManagementClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    /**
     * 中文说明：执行 publish 操作；该方法是 {@code GatewayDdcRulePublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the publish operation; this method is the invocation entry point on {@code GatewayDdcRulePublisher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcRulePublisher.publish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param command 参数 command；parameter command。
     * @return 返回 publish 的处理结果；returns the result of the operation.
     */
    public DdcManagementPublishResult publish(
            GatewayDdcPublicationCommand command) {
        Objects.requireNonNull(command, "command");
        return client.publish(new DdcManagementPublishRequest(
                command.bizCode(),
                command.env(),
                command.appCode(),
                GatewayDdcYamlDocument.RESOURCE_NAME,
                command.value(),
                GatewayDdcYamlDocument.FORMAT,
                command.expectedVersion(),
                command.changeId(),
                command.timeout().toMillis(),
                command.operator()
        ));
    }

    /**
     * 中文说明：执行 ensureReadyTarget 操作；该方法是 {@code GatewayDdcRulePublisher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ensure ready target operation; this method is the invocation entry point on {@code GatewayDdcRulePublisher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDdcRulePublisher.ensureReadyTarget(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param env 参数 env；parameter env。
     * @param appCode 参数 appCode；parameter app code。
     */
    public void ensureReadyTarget(
            String bizCode,
            String env,
            String appCode) {
        List<DdcManagementConfigClientInstance> targets =
                client.getConfigClients(new DdcManagementInstanceQuery(
                        bizCode,
                        env,
                        appCode
                ));
        Instant now = Instant.now();
        boolean ready = targets != null && targets.stream()
                .filter(Objects::nonNull)
                .anyMatch(target -> target.normalizedStatus()
                        == DdcInstanceStatus.ONLINE
                        && target.expireAt() != null
                        && target.expireAt().isAfter(now));
        if (!ready) {
            throw new IllegalStateException(
                    "GATEWAY_RELEASE_NO_READY_TARGET"
            );
        }
    }

}
