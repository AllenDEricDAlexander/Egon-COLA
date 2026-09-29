package top.egon.cola.component.tianshu.admin.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiContentStrategy;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class DdcAiTransactionConfiguration {

    @Bean("ddcAiReadTransactionTemplate")
    public TransactionTemplate ddcAiReadTransactionTemplate(
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {
        // API-003 takes a root FOR SHARE lock; PostgreSQL rejects that lock in a JDBC READ ONLY transaction.
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setTimeout(8);
        return template;
    }

    @Bean("ddcAiCommandTransactionTemplate")
    public TransactionTemplate ddcAiCommandTransactionTemplate(
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setTimeout(8);
        return template;
    }

    @Bean("ddcAiContentStrategyRegistry")
    public Map<DdcAiKindEnum, DdcAiContentStrategy> ddcAiContentStrategyRegistry(
            @Qualifier("ddcPromptContentStrategy") DdcAiContentStrategy promptStrategy,
            @Qualifier("ddcSkillContentStrategy") DdcAiContentStrategy skillStrategy) {
        EnumMap<DdcAiKindEnum, DdcAiContentStrategy> strategies = new EnumMap<>(DdcAiKindEnum.class);
        register(strategies, promptStrategy);
        register(strategies, skillStrategy);
        if (!strategies.keySet().equals(Set.of(DdcAiKindEnum.PROMPT, DdcAiKindEnum.SKILL))) {
            throw new IllegalStateException("AI_CONTENT_STRATEGY_REGISTRATION_INCOMPLETE");
        }
        return Collections.unmodifiableMap(strategies);
    }

    private void register(Map<DdcAiKindEnum, DdcAiContentStrategy> strategies, DdcAiContentStrategy strategy) {
        if (strategies.putIfAbsent(strategy.kind(), strategy) != null) {
            throw new IllegalStateException("AI_CONTENT_STRATEGY_DUPLICATE:" + strategy.kind());
        }
    }
}
