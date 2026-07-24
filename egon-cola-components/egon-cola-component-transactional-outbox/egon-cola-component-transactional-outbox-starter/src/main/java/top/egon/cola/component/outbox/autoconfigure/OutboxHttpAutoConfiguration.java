package top.egon.cola.component.outbox.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.delivery.http.DefaultHttpDeliveryClassifier;
import top.egon.cola.component.outbox.delivery.http.HttpCredentialProvider;
import top.egon.cola.component.outbox.delivery.http.HttpDeliveryClassifier;
import top.egon.cola.component.outbox.delivery.http.HttpDeliveryHandler;
import top.egon.cola.component.outbox.delivery.http.HttpDestinationResolver;
import top.egon.cola.component.outbox.delivery.http.PropertiesHttpDestinationResolver;

import java.time.Clock;

@AutoConfiguration(after = TransactionalOutboxAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@ConditionalOnBean(TransactionalOutbox.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.transactional-outbox.http",
        name = "enabled",
        havingValue = "true"
)
public class OutboxHttpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    HttpDestinationResolver httpDestinationResolver(
            TransactionalOutboxProperties properties
    ) {
        return new PropertiesHttpDestinationResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    HttpCredentialProvider httpCredentialProvider() {
        return HttpCredentialProvider.none();
    }

    @Bean
    @ConditionalOnMissingBean
    HttpDeliveryClassifier httpDeliveryClassifier() {
        return new DefaultHttpDeliveryClassifier();
    }

    @Bean
    @ConditionalOnMissingBean
    HttpDeliveryHandler httpDeliveryHandler(
            HttpDestinationResolver destinationResolver,
            HttpCredentialProvider credentialProvider,
            HttpDeliveryClassifier classifier
    ) {
        return new HttpDeliveryHandler(
                destinationResolver,
                credentialProvider,
                classifier,
                Clock.systemUTC()
        );
    }
}
