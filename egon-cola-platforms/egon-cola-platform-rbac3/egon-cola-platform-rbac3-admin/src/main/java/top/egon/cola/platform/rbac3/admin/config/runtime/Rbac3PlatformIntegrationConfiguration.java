package top.egon.cola.platform.rbac3.admin.config.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationRuntime;
import top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationProperties;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.integration.ddc.DdcProviderLeaseStatusService;
import top.egon.cola.platform.rbac3.admin.integration.ddc.DdcConfigClientStatusService;
import top.egon.cola.platform.rbac3.admin.config.flyway.Rbac3FlywayConfiguration;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayAdminControlPlaneStatusClient;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayAdminStatusCredentialProvider;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayDefinitionStatusService;
import top.egon.cola.platform.rbac3.admin.integration.outbox.TransactionalOutboxAuthorizationEventAdapter;
import top.egon.cola.platform.rbac3.admin.integration.runtime.GatewayDdcRuntimeStatusService;
import top.egon.cola.platform.rbac3.admin.integration.runtime.Rbac3HttpProviderPublicationGate;
import top.egon.cola.platform.rbac3.admin.integration.runtime.Rbac3OperationalRuntimeStatusService;
import top.egon.cola.platform.rbac3.admin.integration.runtime.Rbac3ReadinessIndicator;
import top.egon.cola.platform.rbac3.admin.config.properties.Rbac3GatewayStatusProperties;
import top.egon.cola.platform.rbac3.admin.runtime.application.ControlPlaneRuntimeStatusPort;

import java.time.Clock;
import java.util.List;

/**
 * 类型 `Rbac3PlatformIntegrationConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Platform Integration Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3PlatformIntegrationConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Platform Integration Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Wires component-owned Gateway, DDC and Outbox runtimes into RBAC3 ports.
 */
@Configuration(proxyBeanMethods = false)
public class Rbac3PlatformIntegrationConfiguration {

    /**
     * 方法 `rbac3Clock` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `rbac3 Clock` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3Clock` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `rbac3 Clock` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3Clock` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3Clock`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean
    Clock rbac3Clock() {
        return Clock.systemUTC();
    }

    /**
     * 方法 `authorizationEventPort` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `authorization Event Port` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationEventPort` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `authorization Event Port` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationEventPort` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationEventPort`, then continue the business flow using its result, exception, or side effect.
     *
     * @param outbox 输入参数 `outbox`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnMissingBean(AuthorizationEventPort.class)
    AuthorizationEventPort authorizationEventPort(
            TransactionalOutbox outbox,
            Clock clock) {
        return new TransactionalOutboxAuthorizationEventAdapter(outbox, clock);
    }

    /**
     * 方法 `rbac3ServiceIdentity` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `rbac3 Service Identity` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3ServiceIdentity` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `rbac3 Service Identity` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3ServiceIdentity` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3ServiceIdentity`, then continue the business flow using its result, exception, or side effect.
     *
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    GatewayDdcRuntimeStatusService.ServiceIdentity rbac3ServiceIdentity(
            GatewayReportingProperties properties) {
        return new GatewayDdcRuntimeStatusService.ServiceIdentity(
                properties.getBizCode(), properties.getApplicationCode(),
                properties.getEnv(), properties.getNamespace(),
                "HTTP_PROVIDER", "http", properties.getApplicationCode(),
                "default", properties.getArtifactVersion());
    }

    /**
     * 方法 `gatewayAdminStatusCredentialProvider` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `gateway Admin Status Credential Provider` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `gatewayAdminStatusCredentialProvider` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `gateway Admin Status Credential Provider` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `gatewayAdminStatusCredentialProvider` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `gatewayAdminStatusCredentialProvider`, then continue the business flow using its result, exception, or side effect.
     *
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    GatewayAdminStatusCredentialProvider gatewayAdminStatusCredentialProvider(
            Rbac3GatewayStatusProperties properties,
            ObjectMapper objectMapper,
            Clock clock) {
        return GatewayAdminStatusCredentialProvider.rotatingFile(
                properties.requireOauthTokenFile(), objectMapper, clock);
    }

    /**
     * 方法 `gatewayAdminControlPlaneStatusClient` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `gateway Admin Control Plane Status Client` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `gatewayAdminControlPlaneStatusClient` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `gateway Admin Control Plane Status Client` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `gatewayAdminControlPlaneStatusClient` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `gatewayAdminControlPlaneStatusClient`, then continue the business flow using its result, exception, or side effect.
     *
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identity 输入参数 `identity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentials 输入参数 `credentials`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    GatewayAdminControlPlaneStatusClient gatewayAdminControlPlaneStatusClient(
            Rbac3GatewayStatusProperties properties,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity,
            GatewayAdminStatusCredentialProvider credentials,
            ObjectMapper objectMapper,
            Clock clock) {
        return new GatewayAdminControlPlaneStatusClient(
                properties.requireAdminBaseUrl(),
                properties.requireGatewayGroupId(),
                properties.requireReleaseId(),
                new GatewayAdminControlPlaneStatusClient.ServiceKey(
                        identity.bizCode(), identity.appCode(),
                        identity.env(), identity.namespace(), identity.serviceKind(),
                        identity.protocol(), identity.serviceName(), identity.group(),
                        identity.version()),
                credentials, objectMapper, clock, properties.requireTimeout());
    }

    /**
     * 方法 `gatewayDefinitionStatusService` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `gateway Definition Status Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `gatewayDefinitionStatusService` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `gateway Definition Status Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `gatewayDefinitionStatusService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `gatewayDefinitionStatusService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param state 输入参数 `state`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    GatewayDefinitionStatusService gatewayDefinitionStatusService(
            GatewayReportingState state,
            GatewayReportingProperties properties) {
        return new GatewayDefinitionStatusService(state, properties);
    }

    /**
     * 方法 `ddcProviderLeaseStatusService` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `ddc Provider Lease Status Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ddcProviderLeaseStatusService` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `ddc Provider Lease Status Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ddcProviderLeaseStatusService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ddcProviderLeaseStatusService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param runtime 输入参数 `runtime`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identity 输入参数 `identity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean(GatewayDdcRuntimeStatusService.ServiceIdentity.class)
    @ConditionalOnProperty(prefix = "egon.cola.component.ddc.registry.http",
            name = "enabled", havingValue = "true")
    DdcProviderLeaseStatusService ddcProviderLeaseStatusService(
            DdcHttpRegistrationRuntime runtime,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity) {
        return new DdcProviderLeaseStatusService(runtime, identity);
    }

    /**
     * 方法 `ddcHttpRegistrationServerReadyListener` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `ddc Http Registration Server Ready Listener` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ddcHttpRegistrationServerReadyListener` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `ddc Http Registration Server Ready Listener` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ddcHttpRegistrationServerReadyListener` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ddcHttpRegistrationServerReadyListener`, then continue the business flow using its result, exception, or side effect.
     *
     * @param coordinator 输入参数 `coordinator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param providerRuntime 输入参数 `providerRuntime`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param providerProperties 输入参数 `providerProperties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean(name = "ddcHttpRegistrationServerReadyListener")
    @ConditionalOnProperty(prefix = "egon.cola.component.ddc",
            name = "enabled", havingValue = "true")
    ApplicationListener<ApplicationEvent> ddcHttpRegistrationServerReadyListener(
            DdcRuntimeCoordinator coordinator,
            DdcHttpRegistrationRuntime providerRuntime,
            DdcHttpRegistrationProperties providerProperties) {
        return new Rbac3HttpProviderPublicationGate(
                coordinator, providerRuntime, providerProperties);
    }

    /**
     * 方法 `gatewayDdcRuntimeStatusService` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `gateway Ddc Runtime Status Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `gatewayDdcRuntimeStatusService` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `gateway Ddc Runtime Status Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `gatewayDdcRuntimeStatusService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `gatewayDdcRuntimeStatusService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param definition 输入参数 `definition`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param lease 输入参数 `lease`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param gatewayAdmin 输入参数 `gatewayAdmin`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identity 输入参数 `identity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    GatewayDdcRuntimeStatusService gatewayDdcRuntimeStatusService(
            GatewayDefinitionStatusService definition,
            DdcProviderLeaseStatusService lease,
            GatewayAdminControlPlaneStatusClient gatewayAdmin,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity,
            Clock clock) {
        return new GatewayDdcRuntimeStatusService(
                definition, lease, gatewayAdmin, identity, clock);
    }

    /**
     * 方法 `rbac3ControlPlaneRuntimeStatusPort` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `rbac3 Control Plane Runtime Status Port` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3ControlPlaneRuntimeStatusPort` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `rbac3 Control Plane Runtime Status Port` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3ControlPlaneRuntimeStatusPort` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3ControlPlaneRuntimeStatusPort`, then continue the business flow using its result, exception, or side effect.
     *
     * @param runtimeStatus 输入参数 `runtimeStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ddcConfigStatus 输入参数 `ddcConfigStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param operationalStatus 输入参数 `operationalStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @Primary
    ControlPlaneRuntimeStatusPort rbac3ControlPlaneRuntimeStatusPort(
            ObjectProvider<GatewayDdcRuntimeStatusService> runtimeStatus,
            ObjectProvider<DdcConfigClientStatusService> ddcConfigStatus,
            Rbac3OperationalRuntimeStatusService operationalStatus,
            Clock clock) {
        return () -> {
            GatewayDdcRuntimeStatusService available = runtimeStatus.getIfAvailable();
            ControlPlaneRuntimeStatusPort.RuntimeStatus controlPlane = available != null
                    ? available.status()
                    : new ControlPlaneRuntimeStatusPort.RuntimeStatus(
                    new ControlPlaneRuntimeStatusPort.DefinitionStatus(
                            "UNKNOWN", null, List.of("CONTROL_PLANE_DISABLED")),
                    new ControlPlaneRuntimeStatusPort.ProviderLeaseStatus(
                            "STOPPED", null, null),
                    new ControlPlaneRuntimeStatusPort.GatewayReleaseStatus(
                            null, "UNKNOWN", null),
                    clock.instant());
            var operational = operationalStatus.status();
            DdcConfigClientStatusService availableDdcConfig =
                    ddcConfigStatus.getIfAvailable();
            return new ControlPlaneRuntimeStatusPort.RuntimeStatus(
                    availableDdcConfig == null
                            ? ControlPlaneRuntimeStatusPort.DdcConfigClientStatus.unknown()
                            : availableDdcConfig.status(),
                    controlPlane.definition(), controlPlane.providerLease(),
                    controlPlane.gatewayRelease(), operational.flyway(),
                    operational.redisProjection(), operational.fence(),
                    operational.outbox(), clock.instant());
        };
    }

    /**
     * 方法 `rbac3ReadinessIndicator` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `rbac3 Readiness Indicator` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3ReadinessIndicator` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `rbac3 Readiness Indicator` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3ReadinessIndicator` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3ReadinessIndicator`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rbac3Flyway 输入参数 `rbac3Flyway`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param outboxFlyway 输入参数 `outboxFlyway`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param entityManagerFactory 输入参数 `entityManagerFactory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param outbox 输入参数 `outbox`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeRedis 输入参数 `runtimeRedis`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param definition 输入参数 `definition`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param lease 输入参数 `lease`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ddcConfigStatus 输入参数 `ddcConfigStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeStatus 输入参数 `runtimeStatus`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reportingProperties 输入参数 `reportingProperties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean("rbac3Readiness")
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    Rbac3ReadinessIndicator rbac3ReadinessIndicator(
            @Qualifier(Rbac3FlywayConfiguration.RBAC3_FLYWAY) Flyway rbac3Flyway,
            @Qualifier(Rbac3FlywayConfiguration.OUTBOX_FLYWAY) Flyway outboxFlyway,
            EntityManagerFactory entityManagerFactory,
            TransactionalOutbox outbox,
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient runtimeRedis,
            GatewayDefinitionStatusService definition,
            DdcProviderLeaseStatusService lease,
            ObjectProvider<DdcConfigClientStatusService> ddcConfigStatus,
            GatewayDdcRuntimeStatusService runtimeStatus,
            GatewayReportingProperties reportingProperties) {
        boolean production = !List.of("local", "test").contains(
                reportingProperties.getEnv().toLowerCase(java.util.Locale.ROOT));
        return new Rbac3ReadinessIndicator(List.of(
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "rbac3Flyway", () -> migrated(rbac3Flyway)),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "outboxFlyway", () -> migrated(outboxFlyway)),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "jpa", entityManagerFactory::isOpen),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "outboxSchema", () -> outbox != null),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "rbac3RuntimeRedis", () -> redisAvailable(runtimeRedis)),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "gatewayDefinition", () -> definition.status().accepted()),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "ddcProviderLease", () -> !production
                                || "REGISTERED".equals(lease.status().state())),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "ddcConfigClient", () -> !production
                                || ddcConfigReady(ddcConfigStatus))),
                () -> runtimeStatus.status().gatewayRelease().status());
    }

    /**
     * 方法 `rbac3TrafficDrainListener` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `rbac3 Traffic Drain Listener` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3TrafficDrainListener` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `rbac3 Traffic Drain Listener` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3TrafficDrainListener` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3TrafficDrainListener`, then continue the business flow using its result, exception, or side effect.
     *
     * @param readiness 输入参数 `readiness`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    @ConditionalOnBean(name = "rbac3Readiness")
    ApplicationListener<ContextClosedEvent> rbac3TrafficDrainListener(
            @Qualifier("rbac3Readiness") Rbac3ReadinessIndicator readiness) {
        return ignored -> readiness.stopAcceptingTraffic();
    }

    /**
     * 方法 `migrated` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `migrated` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `migrated` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `migrated` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `migrated` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `migrated`, then continue the business flow using its result, exception, or side effect.
     *
     * @param flyway 输入参数 `flyway`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static boolean migrated(Flyway flyway) {
        return flyway.info().pending().length == 0
                && flyway.info().current() != null;
    }

    /**
     * 方法 `redisAvailable` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `redis Available` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `redisAvailable` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `redis Available` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `redisAvailable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `redisAvailable`, then continue the business flow using its result, exception, or side effect.
     *
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static boolean redisAvailable(RedissonClient redisson) {
        redisson.getKeys().count();
        return true;
    }

    /**
     * 方法 `ddcConfigReady` 按照 `Rbac3PlatformIntegrationConfiguration` 的职责处理输入，完成 `ddc Config Ready` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `ddcConfigReady` processes its inputs according to `Rbac3PlatformIntegrationConfiguration`'s responsibility, performs the `ddc Config Ready` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `ddcConfigReady` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `ddcConfigReady`, then continue the business flow using its result, exception, or side effect.
     *
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static boolean ddcConfigReady(
            ObjectProvider<DdcConfigClientStatusService> status) {
        DdcConfigClientStatusService available = status.getIfAvailable();
        return available != null && available.ready();
    }
}
