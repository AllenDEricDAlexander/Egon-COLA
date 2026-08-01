package top.egon.cola.component.gateway.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.client.DdcClientTransportSecurity;
import top.egon.cola.component.ddc.management.client.DdcManagementClientProperties;
import top.egon.cola.component.ddc.management.client.HttpDdcManagementClient;
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

import java.time.Clock;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(GatewayAdminProperties.class)
public class GatewayAdminConfiguration {

    @Value("${gateway.admin.ddc.tls.enabled:false}")
    private boolean ddcTlsEnabled;

    @Value("${gateway.admin.ddc.tls.development-plaintext:false}")
    private boolean ddcDevelopmentPlaintext = true;

    @Value("${gateway.admin.ddc.tls.certificate-chain-path:}")
    private String ddcCertificateChainPath = "";

    @Value("${gateway.admin.ddc.tls.private-key-path:}")
    private String ddcPrivateKeyPath = "";

    @Value("${gateway.admin.ddc.tls.trust-certificate-collection-path:}")
    private String ddcTrustCertificateCollectionPath = "";

    @Bean
    @ConditionalOnProperty(
            name = "gateway.admin.ddc.enabled",
            havingValue = "true"
    )
    DdcManagementClient ddcManagementClient(
            @Value("${gateway.admin.ddc.endpoint}") String endpoint,
            @Value("${gateway.admin.ddc.access-key}") String accessKey,
            @Value("${gateway.admin.ddc.secret-key}") String secretKey,
            @Value("${gateway.admin.ddc.connect-timeout:PT3S}")
            Duration connectTimeout,
            @Value("${gateway.admin.ddc.read-timeout:PT10S}")
            Duration readTimeout) {
        return new HttpDdcManagementClient(
                new DdcManagementClientProperties(
                        endpoint,
                        accessKey,
                        secretKey,
                        connectTimeout,
                        readTimeout,
                        new DdcClientTransportSecurity(
                                ddcTlsEnabled,
                                ddcDevelopmentPlaintext,
                                ddcCertificateChainPath,
                                ddcPrivateKeyPath,
                                ddcTrustCertificateCollectionPath
                        )
                )
        );
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
