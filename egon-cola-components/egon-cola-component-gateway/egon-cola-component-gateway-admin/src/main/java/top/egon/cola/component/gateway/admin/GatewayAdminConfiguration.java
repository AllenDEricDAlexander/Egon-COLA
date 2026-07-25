package top.egon.cola.component.gateway.admin;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.gateway.admin.rule.GatewayDdcRulePublisher;
import top.egon.cola.component.gateway.admin.application.credential.GatewaySecretProtector;
import top.egon.cola.component.gateway.admin.infrastructure.security.AesGcmGatewaySecretProtector;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class GatewayAdminConfiguration {

    @Bean
    @ConditionalOnBean(DdcManagementClient.class)
    GatewayDdcRulePublisher gatewayDdcRulePublisher(
            DdcManagementClient client) {
        return new GatewayDdcRulePublisher(client, Duration.ofSeconds(10));
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
}
