package top.egon.cola.component.tianshu.admin.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.outbox.common.exception.OutboxValidationException;
import top.egon.cola.component.tianshu.admin.service.config.DdcYamlConfigValidator;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;

import java.time.Clock;
import java.util.regex.Pattern;

@Configuration(value = "ddcOutboxConfiguration", proxyBeanMethods = false)
public class DdcOutboxConfiguration {

    @Bean("ddcPublishTransactionTemplate")
    public TransactionTemplate ddcPublishTransactionTemplate(
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean({"ddcPublishClock", "egonColaMybatisPlusClock"})
    public Clock ddcPublishClock() {
        return Clock.systemUTC();
    }

    @Bean("ddcYamlConfigValidator")
    public DdcYamlConfigValidator ddcYamlConfigValidator(
            @Qualifier("egon.cola.component.tianshu.admin-top.egon.cola.component.tianshu.admin.config.DdcAdminProperties")
            DdcAdminProperties properties) {
        return new DdcYamlConfigValidator(properties.getMaxConfigBytes());
    }

    @Bean("ddcAiPublicationOutboxRoute")
    public DdcAiPublicationOutboxRouteDTO ddcAiPublicationOutboxRoute() {
        return new DdcAiPublicationOutboxRouteDTO("tianshu.ai-publication", "1");
    }

    /**
     * EVENT-103 的 Outbox 路由：固定通道、schema 与租户 AI Topic 目的地。
     * / Outbox route of EVENT-103: fixed channel, schema and tenant AI-topic destination.
     */
    public record DdcAiPublicationOutboxRouteDTO(String channel, String schemaVersion) {

        public static final Pattern TOPIC_PATTERN = Pattern.compile(
                "^tianshu:v4:tenant:[1-9][0-9]*:\\{[a-f0-9]{64}}:ai:topic$");

        public DdcAiPublicationOutboxRouteDTO {
            if (!"tianshu.ai-publication".equals(channel) || !"1".equals(schemaVersion)) {
                throw new IllegalArgumentException("Invalid Tianshu ai-publication Outbox route");
            }
        }

        public String eventType() {
            return "TIANSHU_AI_PUBLICATION";
        }

        public String destination(Long tenantId, String bizCode, String env, String appCode) {
            String destination = DdcRedisKeys.aiTopic(tenantId, bizCode, env, appCode);
            if (!TOPIC_PATTERN.matcher(destination).matches()) {
                throw new IllegalStateException("AI_TOPIC_DESTINATION_INVALID");
            }
            return destination;
        }
    }

    @Bean("ddcConfigDispatchOutboxRoute")
    public DdcConfigDispatchOutboxRouteDTO ddcConfigDispatchOutboxRoute() {
        return new DdcConfigDispatchOutboxRouteDTO("tianshu.config-dispatch", "1");
    }

    public record DdcConfigDispatchOutboxRouteDTO(String channel, String schemaVersion) {

        private static final Pattern TOPIC_PATTERN = Pattern.compile(
                "^tianshu:v4:tenant:[1-9][0-9]*:\\{[a-f0-9]{64}}:topic$");

        public DdcConfigDispatchOutboxRouteDTO {
            if (!"tianshu.config-dispatch".equals(channel) || !"1".equals(schemaVersion)) {
                throw new IllegalArgumentException("Invalid Tianshu config-dispatch Outbox route");
            }
        }

        public String eventType() {
            return "TIANSHU_CONFIG_DISPATCH";
        }

        public String destination(DdcConfigDispatchPreparedEvent event) {
            if (event == null || event.getMessage() == null) {
                throw new IllegalArgumentException("Prepared config event is required for destination");
            }
            return DdcRedisKeys.topic(
                    event.getTenantId(),
                    event.getMessage().getBizCode(),
                    event.getMessage().getEnv(),
                    event.getMessage().getAppCode());
        }

        public boolean matchesDestination(DdcConfigDispatchPreparedEvent event, String destination) {
            return destination != null
                    && TOPIC_PATTERN.matcher(destination).matches()
                    && destination.equals(destination(event));
        }

        public void validateDestination(String destination) {
            if (destination == null || !TOPIC_PATTERN.matcher(destination).matches()) {
                throw new OutboxValidationException("Invalid Tianshu config dispatch destination");
            }
        }
    }
}
