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
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RequestMappingHandlerMapping.class)
    static class WebFluxContributorConfiguration {

        /**
         * Creates the WebFlux definition contributor for the application
         * mapping registry.
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
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RpcContractCatalog.class)
    static class RpcContributorConfiguration {

        /**
         * Exposes the Gateway definition identity through RPC provider
         * registration metadata.
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
