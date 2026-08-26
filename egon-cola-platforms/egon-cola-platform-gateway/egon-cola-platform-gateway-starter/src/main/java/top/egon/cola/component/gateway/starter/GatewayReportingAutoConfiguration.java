package top.egon.cola.component.gateway.starter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationContributor;
import top.egon.cola.component.gateway.contract.reporting.GatewayDefinitionIdentity;
import top.egon.cola.component.gateway.starter.discovery.GatewayDefinitionContributor;
import top.egon.cola.component.gateway.starter.discovery.rpc.RpcGatewayDefinitionContributor;
import top.egon.cola.component.gateway.starter.reporting.GatewayDefinitionReportFactory;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportHttpClient;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingCoordinator;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;
import top.egon.cola.component.rpc.contract.catalog.RpcContractCatalog;
import top.egon.cola.component.rpc.config.EgonRpcAutoConfig;
import top.egon.cola.component.rpc.provider.metadata.RpcProviderMetadataContributor;

import java.util.List;
import java.util.Map;

/**
 * Auto-configures RPC descriptor discovery, report construction, transport,
 * and startup lifecycle coordination for Gateway definition reporting.
 *
 * <p>中文：自动装配 RPC 描述符发现、网关定义报告构建、传输以及启动生命周期协调
 * 组件。HTTP 文档由独立的 Springdoc adapter 负责。
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

    /** Creates the factory that converts RPC definitions into reports. */
    @Bean
    @ConditionalOnMissingBean
    public GatewayDefinitionReportFactory gatewayDefinitionReportFactory(
            GatewayReportingProperties properties,
            ObjectProvider<LongIdGenerator> idGenerators) {
        return new GatewayDefinitionReportFactory(
                properties,
                idGenerators.getIfAvailable(),
                java.time.Clock.systemUTC()
        );
    }

    /** Discovers RPC contributors and builds the startup report. */
    @Bean
    @ConditionalOnMissingBean
    public GatewayDefinitionReportFactory.BuiltReport gatewayBuiltReport(
            GatewayDefinitionReportFactory factory,
            ObjectProvider<GatewayDefinitionContributor> contributors) {
        List<GatewayDefinitionContributor.DiscoveredInterfaceGroup> groups =
                contributors.orderedStream()
                        .flatMap(contributor -> contributor.discover().stream())
                        .toList();
        return factory.build(groups);
    }

    /** Exposes the reporting identity derived from the built report. */
    @Bean
    @ConditionalOnMissingBean
    public GatewayDefinitionIdentity gatewayDefinitionIdentity(
            GatewayDefinitionReportFactory.BuiltReport report) {
        return report.identity();
    }

    /** Contributes report identity to DDC HTTP service registration. */
    @Bean
    @ConditionalOnMissingBean(
            name = "gatewayDefinitionIdentityHttpRegistrationContributor"
    )
    public DdcHttpRegistrationContributor
            gatewayDefinitionIdentityHttpRegistrationContributor(
            GatewayDefinitionIdentity identity) {
        return new DdcHttpRegistrationContributor() {
            @Override
            public String serviceVersion() {
                return identity.artifactVersion();
            }

            @Override
            public Map<String, String> metadata() {
                return Map.of(
                        "gateway.definition-set-id", identity.definitionSetId(),
                        "gateway.artifact-version", identity.artifactVersion(),
                        "gateway.build-id", identity.buildId()
                );
            }
        };
    }

    /** Creates the in-memory reporting lifecycle state. */
    @Bean
    @ConditionalOnMissingBean
    public GatewayReportingState gatewayReportingState() {
        return new GatewayReportingState();
    }

    /** Creates the signed HTTP client used to submit reports to Admin. */
    @Bean
    @ConditionalOnMissingBean
    public GatewayReportHttpClient gatewayReportHttpClient(
            GatewayReportingProperties properties) {
        return new GatewayReportHttpClient(properties);
    }

    /** Creates the startup report submission coordinator. */
    @Bean
    @ConditionalOnMissingBean
    public GatewayReportingCoordinator gatewayReportingCoordinator(
            GatewayDefinitionReportFactory.BuiltReport report,
            GatewayReportHttpClient client,
            GatewayReportingState state) {
        return new GatewayReportingCoordinator(report, client, state);
    }

    /**
     * Registers the RPC descriptor contributor and registration metadata.
     * 中文：仅在 RPC 契约目录存在时注册 RPC 描述符事实源。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RpcContractCatalog.class)
    static class RpcContributorConfiguration {

        @Bean
        @ConditionalOnMissingBean(
                name = "gatewayDefinitionIdentityRpcMetadataContributor"
        )
        RpcProviderMetadataContributor
                gatewayDefinitionIdentityRpcMetadataContributor(
                GatewayDefinitionIdentity identity) {
            return ignored -> Map.of(
                    "gateway.definition-set-id", identity.definitionSetId(),
                    "gateway.artifact-version", identity.artifactVersion(),
                    "gateway.build-id", identity.buildId()
            );
        }

        @Bean
        @ConditionalOnBean(RpcContractCatalog.class)
        GatewayDefinitionContributor rpcGatewayDefinitionContributor(
                RpcContractCatalog catalog,
                GatewayReportingProperties properties) {
            return new RpcGatewayDefinitionContributor(catalog, properties);
        }
    }
}
