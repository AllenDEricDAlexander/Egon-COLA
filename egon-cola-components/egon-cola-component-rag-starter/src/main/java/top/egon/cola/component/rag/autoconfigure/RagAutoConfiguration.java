package top.egon.cola.component.rag.autoconfigure;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import top.egon.cola.component.rag.exception.RagConfigurationException;

import java.time.Clock;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wires the flat RAG starter only when the host explicitly enables it.
 *
 * <p>Every bean is created here; the component never creates an {@code EmbeddingModel} or a
 * {@code VectorStore} of its own and never holds a provider endpoint or key.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "egon.cola.component.rag", name = "enabled",
        havingValue = "true", matchIfMissing = false)
public class RagAutoConfiguration {

    @Bean(name = "ragClock")
    @ConditionalOnMissingBean(name = "ragClock")
    public Clock ragClock() {
        return Clock.systemUTC();
    }

    @Bean(name = "ragProperties")
    @ConditionalOnMissingBean(name = "ragProperties")
    public RagProperties ragProperties(Environment environment, Validator validator) {
        RagProperties properties = Binder.get(environment).bindOrCreate(
                "egon.cola.component.rag",
                Bindable.of(RagProperties.class),
                new NoUnboundElementsBindHandler(BindHandler.DEFAULT));
        Set<ConstraintViolation<RagProperties>> violations = validator.validate(properties);
        if (!violations.isEmpty()) {
            throw new RagConfigurationException("invalid rag configuration: " + violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        return properties;
    }
}
