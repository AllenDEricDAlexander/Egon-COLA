package top.egon.cola.component.gateway.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.component.gateway.contract.reporting.GatewayDefinitionIdentity;
import top.egon.cola.component.gateway.starter.discovery.GatewayDefinitionContributor;
import top.egon.cola.component.gateway.starter.discovery.MvcGatewayDefinitionContributor;
import top.egon.cola.component.gateway.starter.discovery.RpcGatewayDefinitionContributor;
import top.egon.cola.component.gateway.starter.discovery.WebFluxGatewayDefinitionContributor;
import top.egon.cola.component.gateway.starter.reporting.GatewayDefinitionReportFactory;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportHttpClient;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingCoordinator;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingStateStore;
import top.egon.cola.component.rpc.contract.RpcContractCatalog;
import top.egon.cola.component.rpc.config.EgonRpcAutoConfig;
import top.egon.cola.component.rpc.provider.RpcProviderMetadataContributor;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Auto-configures discovery, report construction, transport, persistence, and
 * lifecycle coordination for Gateway interface definition reporting.
 *
 * <p>中文：自动装配网关接口定义的发现、报告构建、传输、持久化以及
 * 生命周期协调组件。
 */
@AutoConfiguration
@AutoConfigureAfter(EgonRpcAutoConfig.class)
@EnableConfigurationProperties(GatewayReportingProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.gateway.reporting",
        name = "enabled",
        havingValue = "true"
)
public class GatewayReportingAutoConfiguration {

    /**
     * Creates the factory that converts discovered definitions into reports.
     * 中文：创建负责将发现结果转换为接口定义报告的工厂。
     *
     * @param properties reporting configuration
     * @return report factory
     */
    @Bean
    @ConditionalOnMissingBean
    public GatewayDefinitionReportFactory gatewayDefinitionReportFactory(
            GatewayReportingProperties properties) {
        return new GatewayDefinitionReportFactory(properties);
    }

    /**
     * Discovers all contributed interface groups and builds the startup report.
     * 中文：收集所有贡献者提供的接口分组并构建启动阶段报告。
     *
     * @param factory report factory
     * @param contributors available definition contributors in Spring order
     * @return immutable report payload and identity
     */
    @Bean
    @ConditionalOnMissingBean
    public GatewayDefinitionReportFactory.BuiltReport gatewayBuiltReport(
            GatewayDefinitionReportFactory factory,
            ObjectProvider<GatewayDefinitionContributor> contributors) {
        List<GatewayDefinitionContributor.DiscoveredInterfaceGroup> groups =
                contributors.orderedStream()
                        .flatMap(contributor ->
                                contributor.discover().stream())
                        .toList();
        return factory.build(groups);
    }

    /**
     * Exposes the reporting identity derived from the built report.
     * 中文：暴露由已构建报告派生出的上报身份标识。
     *
     * @param report built report
     * @return reporting definition identity
     */
    @Bean
    @ConditionalOnMissingBean
    public GatewayDefinitionIdentity gatewayDefinitionIdentity(
            GatewayDefinitionReportFactory.BuiltReport report) {
        return report.identity();
    }

    /**
     * Adapts the reporting identity to the provider registration contract.
     * 中文：将上报身份适配为 Provider 注册契约所需的身份对象。
     *
     * @param identity reporting definition identity
     * @return provider-facing definition identity
     */
    @Bean
    @ConditionalOnMissingBean
    public top.egon.cola.component.gateway.contract.definition
            .GatewayDefinitionIdentity gatewayProviderDefinitionIdentity(
            GatewayDefinitionIdentity identity) {
        return new top.egon.cola.component.gateway.contract.definition
                .GatewayDefinitionIdentity(
                identity.definitionSetId(),
                identity.artifactVersion(),
                identity.buildId()
        );
    }

    /**
     * Creates the in-memory observable state of the reporting lifecycle.
     * 中文：创建用于观察上报生命周期的内存状态对象。
     *
     * @return reporting state
     */
    @Bean
    @ConditionalOnMissingBean
    public GatewayReportingState gatewayReportingState() {
        return new GatewayReportingState();
    }

    /**
     * Creates the signed HTTP client used to communicate with Gateway Admin.
     * 中文：创建与 Gateway Admin 通信并执行请求签名的 HTTP 客户端。
     *
     * @param properties reporting configuration
     * @return report HTTP client
     */
    @Bean
    @ConditionalOnMissingBean
    public GatewayReportHttpClient gatewayReportHttpClient(
            GatewayReportingProperties properties) {
        return new GatewayReportHttpClient(properties);
    }

    /**
     * Creates the persistent report state store at the configured path.
     * 中文：按照配置路径创建报告状态持久化存储。
     *
     * @param properties reporting configuration
     * @return reporting state store
     */
    @Bean
    @ConditionalOnMissingBean
    public GatewayReportingStateStore gatewayReportingStateStore(
            GatewayReportingProperties properties) {
        return new GatewayReportingStateStore(
                Path.of(properties.getStateFile())
        );
    }

    /**
     * Creates the lifecycle coordinator that reconciles reports with Admin.
     * 中文：创建负责将本地报告与 Admin 回执进行协调的生命周期组件。
     *
     * @param properties reporting configuration
     * @param report initial built report
     * @param client report HTTP client
     * @param state in-memory reporting state
     * @param stateStore persistent reporting state store
     * @return reporting lifecycle coordinator
     */
    @Bean
    @ConditionalOnMissingBean
    public GatewayReportingCoordinator gatewayReportingCoordinator(
            GatewayReportingProperties properties,
            GatewayDefinitionReportFactory.BuiltReport report,
            GatewayReportHttpClient client,
            GatewayReportingState state,
            GatewayReportingStateStore stateStore) {
        return new GatewayReportingCoordinator(
                properties,
                report,
                client,
                state,
                stateStore
        );
    }

    /**
     * Contributes Spring MVC handler mappings when the MVC stack is active.
     * 中文：MVC 技术栈启用时，注册 Spring MVC 处理器映射发现贡献者。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(
            name = "org.springframework.web.servlet.mvc.method.annotation."
                    + "RequestMappingHandlerMapping"
    )
    static class MvcContributorConfiguration {

        /**
         * Creates the MVC definition contributor for the application mapping
         * registry.
         * 中文：基于应用的 MVC 映射注册表创建接口定义发现贡献者。
         *
         * @param mappings MVC handler mappings
         * @param properties reporting configuration
         * @param objectMapper application JSON mapper
         * @return MVC definition contributor
         */
        @Bean
        @ConditionalOnBean(
                org.springframework.web.servlet.mvc.method.annotation
                        .RequestMappingHandlerMapping.class
        )
        GatewayDefinitionContributor mvcGatewayDefinitionContributor(
                @Qualifier("requestMappingHandlerMapping")
                org.springframework.web.servlet.mvc.method.annotation
                        .RequestMappingHandlerMapping mappings,
                GatewayReportingProperties properties,
                ObjectMapper objectMapper) {
            return new MvcGatewayDefinitionContributor(
                    mappings,
                    properties,
                    objectMapper
            );
        }
    }

    /**
     * Contributes Spring WebFlux handler mappings when WebFlux is active.
     * 中文：WebFlux 启用时，注册 Spring WebFlux 处理器映射发现贡献者。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RequestMappingHandlerMapping.class)
    static class WebFluxContributorConfiguration {

        /**
         * Creates the WebFlux definition contributor for the application
         * mapping registry.
         * 中文：基于应用的 WebFlux 映射注册表创建接口定义发现贡献者。
         *
         * @param mappings WebFlux handler mappings
         * @param properties reporting configuration
         * @param objectMapper application JSON mapper
         * @return WebFlux definition contributor
         */
        @Bean
        @ConditionalOnBean(RequestMappingHandlerMapping.class)
        GatewayDefinitionContributor webFluxGatewayDefinitionContributor(
                @Qualifier("requestMappingHandlerMapping")
                RequestMappingHandlerMapping mappings,
                GatewayReportingProperties properties,
                ObjectMapper objectMapper) {
            return new WebFluxGatewayDefinitionContributor(
                    mappings,
                    properties,
                    objectMapper
            );
        }
    }

    /**
     * Contributes RPC definitions and Gateway identity metadata when the RPC
     * contract catalog is available.
     * 中文：RPC 契约目录可用时，注册 RPC 接口定义和网关身份元数据
     * 贡献者。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RpcContractCatalog.class)
    static class RpcContributorConfiguration {

        /**
         * Exposes the Gateway definition identity through RPC provider
         * registration metadata.
         * 中文：通过 RPC Provider 注册元数据暴露网关接口定义身份。
         *
         * @param identity reporting definition identity
         * @return RPC provider metadata contributor
         */
        @Bean
        @ConditionalOnMissingBean(
                name = "gatewayDefinitionIdentityRpcMetadataContributor"
        )
        RpcProviderMetadataContributor
                gatewayDefinitionIdentityRpcMetadataContributor(
                        GatewayDefinitionIdentity identity) {
            return ignored -> Map.of(
                    "gateway.definition-set-id",
                    identity.definitionSetId(),
                    "gateway.artifact-version",
                    identity.artifactVersion(),
                    "gateway.build-id",
                    identity.buildId()
            );
        }

        /**
         * Creates the RPC definition contributor backed by the contract
         * catalog.
         * 中文：基于 RPC 契约目录创建接口定义发现贡献者。
         *
         * @param catalog RPC contract catalog
         * @param properties reporting configuration
         * @return RPC definition contributor
         */
        @Bean
        @ConditionalOnBean(RpcContractCatalog.class)
        GatewayDefinitionContributor rpcGatewayDefinitionContributor(
                RpcContractCatalog catalog,
                GatewayReportingProperties properties) {
            return new RpcGatewayDefinitionContributor(catalog, properties);
        }
    }
}
