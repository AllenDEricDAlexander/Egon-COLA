package top.egon.cola.component.common.desensitize.autoconfigure;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.common.desensitize.jackson.SensitiveJacksonModule;
import top.egon.cola.component.common.desensitize.logback.SensitiveLogbackRegistryBridge;
import top.egon.cola.component.common.desensitize.metadata.SensitiveMetadataResolver;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategy;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;

import java.util.List;

@AutoConfiguration(before = JacksonAutoConfiguration.class)
public class DataDesensitizeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SensitiveMetadataResolver sensitiveMetadataResolver() {
        return new SensitiveMetadataResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    SensitiveStrategyRegistry sensitiveStrategyRegistry(
            ObjectProvider<SensitiveStrategy> strategyProvider) {
        List<SensitiveStrategy> overrides = strategyProvider.orderedStream().toList();
        return SensitiveStrategyRegistry.defaults().withOverrides(overrides);
    }

    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean
    SensitiveJacksonModule sensitiveJacksonModule(
            SensitiveStrategyRegistry strategyRegistry,
            SensitiveMetadataResolver metadataResolver) {
        return new SensitiveJacksonModule(strategyRegistry, metadataResolver);
    }

    @Bean
    @ConditionalOnClass(LoggerContext.class)
    @ConditionalOnMissingBean
    SensitiveLogbackRegistryBridge sensitiveLogbackRegistryBridge(
            SensitiveStrategyRegistry strategyRegistry) {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        LoggerContext loggerContext = loggerFactory instanceof LoggerContext context
                ? context
                : null;
        return new SensitiveLogbackRegistryBridge(loggerContext, strategyRegistry);
    }
}
