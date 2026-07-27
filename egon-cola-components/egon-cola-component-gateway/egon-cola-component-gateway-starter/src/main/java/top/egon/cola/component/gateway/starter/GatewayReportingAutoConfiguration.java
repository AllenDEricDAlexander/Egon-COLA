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

@AutoConfiguration
@AutoConfigureAfter(EgonRpcAutoConfig.class)
@EnableConfigurationProperties(GatewayReportingProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.gateway.reporting",
        name = "enabled",
        havingValue = "true"
)
public class GatewayReportingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GatewayDefinitionReportFactory gatewayDefinitionReportFactory(
            GatewayReportingProperties properties) {
        return new GatewayDefinitionReportFactory(properties);
    }

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

    @Bean
    @ConditionalOnMissingBean
    public GatewayDefinitionIdentity gatewayDefinitionIdentity(
            GatewayDefinitionReportFactory.BuiltReport report) {
        return report.identity();
    }

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

    @Bean
    @ConditionalOnMissingBean
    public GatewayReportingState gatewayReportingState() {
        return new GatewayReportingState();
    }

    @Bean
    @ConditionalOnMissingBean
    public GatewayReportHttpClient gatewayReportHttpClient(
            GatewayReportingProperties properties) {
        return new GatewayReportHttpClient(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public GatewayReportingStateStore gatewayReportingStateStore(
            GatewayReportingProperties properties) {
        return new GatewayReportingStateStore(
                Path.of(properties.getStateFile())
        );
    }

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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(
            name = "org.springframework.web.servlet.mvc.method.annotation."
                    + "RequestMappingHandlerMapping"
    )
    static class MvcContributorConfiguration {

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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RequestMappingHandlerMapping.class)
    static class WebFluxContributorConfiguration {

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
                    "gateway.definition-set-id",
                    identity.definitionSetId(),
                    "gateway.artifact-version",
                    identity.artifactVersion(),
                    "gateway.build-id",
                    identity.buildId()
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
