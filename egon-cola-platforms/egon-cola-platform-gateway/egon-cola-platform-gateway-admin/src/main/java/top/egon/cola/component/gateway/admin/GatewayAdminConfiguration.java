package top.egon.cola.component.gateway.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.gateway.admin.application.observability.GatewayCallEventIngestService;
import top.egon.cola.component.gateway.admin.application.observability.GatewayObservabilityQueryService;
import top.egon.cola.component.gateway.admin.application.observability.GatewayObservabilityStore;
import top.egon.cola.component.gateway.admin.application.projection.GatewayProjectionService;
import top.egon.cola.component.gateway.admin.application.credential.GatewaySecretProtector;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleasePublicationCoordinator;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleasePublicationStore;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleaseStore;
import top.egon.cola.component.gateway.admin.config.GatewayAdminProperties;
import top.egon.cola.component.gateway.admin.infrastructure.messaging.GatewayCallEventCodec;
import top.egon.cola.component.gateway.admin.infrastructure.messaging.GatewayCallEventConsumerHandler;
import top.egon.cola.component.gateway.admin.infrastructure.messaging.GatewayKafkaCallEventConsumer;
import top.egon.cola.component.gateway.admin.infrastructure.messaging.GatewayKafkaConsumerMetrics;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.JdbcGatewayObservabilityStore;
import top.egon.cola.component.gateway.admin.infrastructure.security.AesGcmGatewaySecretProtector;
import top.egon.cola.component.gateway.admin.interfaces.scheduled.GatewayObservabilityRetentionReaper;
import top.egon.cola.component.gateway.admin.rule.GatewayDdcRulePublisher;
import top.egon.cola.component.gateway.admin.mcp.artifact.FileSystemMcpAppArtifactStore;
import top.egon.cola.component.gateway.mcp.app.McpAppSecurityValidator;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientHandle;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(GatewayAdminProperties.class)
public class GatewayAdminConfiguration {

    @Bean
    FileSystemMcpAppArtifactStore mcpAppArtifactStore(
            @Value(
                    "${gateway.admin.mcp.artifact-root:"
                            + "${java.io.tmpdir}/egon-cola/"
                            + "gateway-mcp-artifacts}"
            ) String artifactRoot) {
        return new FileSystemMcpAppArtifactStore(Path.of(artifactRoot));
    }

    @Bean
    McpAppSecurityValidator mcpAppSecurityValidator() {
        return new McpAppSecurityValidator();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(
            name = "gateway.admin.ddc.enabled",
            havingValue = "true"
    )
    DdcRpcClientHandle<DdcManagementClient>
            gatewayDdcManagementClientHandle(DdcRpcClientFactory factory) {
        return factory.managementClient();
    }

    @Bean
    @ConditionalOnProperty(
            name = "gateway.admin.ddc.enabled",
            havingValue = "true"
    )
    DdcManagementClient ddcManagementClient(
            @Qualifier("gatewayDdcManagementClientHandle")
            DdcRpcClientHandle<DdcManagementClient> handle) {
        return handle.client();
    }

    @Bean
    @ConditionalOnBean(DdcManagementClient.class)
    GatewayDdcRulePublisher gatewayDdcRulePublisher(
            DdcManagementClient client) {
        return new GatewayDdcRulePublisher(client);
    }

    @Bean
    @ConditionalOnBean({
            DdcManagementClient.class,
            GatewayDdcRulePublisher.class
    })
    GatewayReleasePublicationCoordinator
            gatewayReleasePublicationCoordinator(
            GatewayReleasePublicationStore journal,
            GatewayReleaseStore releases,
            DdcManagementClient client,
            GatewayDdcRulePublisher publisher,
            GatewayAdminProperties properties,
            @Value("${gateway.admin.ddc.publish-timeout:PT30S}")
            Duration timeout) {
        return new GatewayReleasePublicationCoordinator(
                journal,
                releases,
                client,
                publisher,
                Clock.systemUTC(),
                timeout,
                properties.getDdc().getTargetBizCode(),
                properties.getDdc().getTargetAppCode()
        );
    }

    @Bean
    @ConditionalOnProperty(
            name = "gateway.admin.secrets.master-key-base64"
    )
    GatewaySecretProtector gatewaySecretProtector(
            @Value("${gateway.admin.secrets.master-key-base64}")
            String masterKey,
            @Value("${gateway.admin.secrets.key-version:v1}")
            String keyVersion) {
        return new AesGcmGatewaySecretProtector(
                java.util.Base64.getDecoder().decode(masterKey),
                keyVersion
        );
    }

    @Bean
    GatewayObservabilityStore gatewayObservabilityStore(
            JdbcTemplate jdbcTemplate) {
        return new JdbcGatewayObservabilityStore(jdbcTemplate);
    }

    @Bean
    GatewayCallEventIngestService gatewayCallEventIngestService(
            GatewayObservabilityStore store,
            @Value(
                    "${gateway.admin.observability.retention:PT168H}"
            ) Duration retention) {
        return new GatewayCallEventIngestService(
                store,
                Clock.systemUTC(),
                retention
        );
    }

    @Bean
    GatewayObservabilityQueryService gatewayObservabilityQueryService(
            GatewayObservabilityStore store,
            GatewayProjectionService projections) {
        return new GatewayObservabilityQueryService(
                store,
                Clock.systemUTC(),
                projections
        );
    }

    @Bean
    GatewayCallEventCodec gatewayCallEventCodec(ObjectMapper objectMapper) {
        return new GatewayCallEventCodec(objectMapper);
    }

    @Bean
    GatewayCallEventConsumerHandler gatewayCallEventConsumerHandler(
            GatewayCallEventCodec codec,
            GatewayCallEventIngestService service) {
        return new GatewayCallEventConsumerHandler(
                codec,
                service,
                Clock.systemUTC()
        );
    }

    @Bean
    GatewayObservabilityRetentionReaper
            gatewayObservabilityRetentionReaper(
            GatewayCallEventIngestService service) {
        return new GatewayObservabilityRetentionReaper(service);
    }

    @Bean
    @ConditionalOnProperty(
            name = "gateway.admin.observability.kafka.enabled",
            havingValue = "true"
    )
    GatewayKafkaCallEventConsumer gatewayKafkaCallEventConsumer(
            GatewayCallEventConsumerHandler handler,
            MeterRegistry meterRegistry,
            @Value(
                    "${gateway.admin.observability.kafka.bootstrap-servers}"
            ) String bootstrapServers,
            @Value(
                    "${gateway.admin.observability.kafka.topic:"
                            + "egon.gateway.call.v1}"
            ) String topic,
            @Value(
                    "${gateway.admin.observability.kafka.group-id:"
                            + "gateway-admin-call-events-v1}"
            ) String groupId,
            @Value(
                    "${gateway.admin.observability.kafka.retry-backoff:"
                            + "PT0.25S}"
            ) Duration retryBackoff,
            @Value(
                    "${gateway.admin.observability.kafka."
                            + "max-record-attempts:5}"
            ) int maxRecordAttempts) {
        return new GatewayKafkaCallEventConsumer(
                handler,
                new GatewayKafkaConsumerMetrics(
                        meterRegistry,
                        Clock.systemUTC()
                ),
                new GatewayKafkaCallEventConsumer.Settings(
                        bootstrapServers,
                        topic,
                        groupId,
                        Duration.ofMillis(500),
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(5),
                        retryBackoff,
                        maxRecordAttempts,
                        java.util.Map.of()
                )
        );
    }
}
