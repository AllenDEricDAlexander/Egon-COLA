package top.egon.cola.component.ruleengine.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import top.egon.cola.component.ruleengine.async.DefaultRuleAsyncExecutor;
import top.egon.cola.component.ruleengine.async.RuleAsyncExecutor;
import top.egon.cola.component.ruleengine.engine.DefaultRuleChainExecutor;
import top.egon.cola.component.ruleengine.engine.DefaultRuleEngine;
import top.egon.cola.component.ruleengine.engine.DefaultRuleTreeExecutor;
import top.egon.cola.component.ruleengine.engine.RuleChainExecutor;
import top.egon.cola.component.ruleengine.engine.RuleEngine;
import top.egon.cola.component.ruleengine.engine.RuleTreeExecutor;
import top.egon.cola.component.ruleengine.listener.LoggingRuleExecutionListener;
import top.egon.cola.component.ruleengine.listener.RuleExecutionListener;
import top.egon.cola.component.ruleengine.listener.RuleExecutionListenerComposite;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(RuleEngineProperties.class)
@ConditionalOnProperty(prefix = "egon.cola.component.rule-engine", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class RuleEngineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RuleChainExecutor ruleChainExecutor(RuleEngineProperties properties,
                                               RuleExecutionListenerComposite listeners) {
        return new DefaultRuleChainExecutor(properties.isTraceEnabled(), properties.isThrowException(), listeners);
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleTreeExecutor ruleTreeExecutor(RuleEngineProperties properties,
                                             RuleExecutionListenerComposite listeners) {
        return new DefaultRuleTreeExecutor(properties.isTraceEnabled(), properties.isThrowException(), listeners);
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleEngine ruleEngine(RuleChainExecutor chainExecutor, RuleTreeExecutor treeExecutor) {
        return new DefaultRuleEngine(chainExecutor, treeExecutor);
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public RuleAsyncExecutor ruleAsyncExecutor(RuleEngineProperties properties) {
        return new DefaultRuleAsyncExecutor(properties.getAsyncCorePoolSize(), properties.getAsyncMaxPoolSize());
    }

    @Bean
    @ConditionalOnMissingBean(LoggingRuleExecutionListener.class)
    public LoggingRuleExecutionListener loggingRuleExecutionListener() {
        return new LoggingRuleExecutionListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleExecutionListenerComposite ruleExecutionListenerComposite(List<RuleExecutionListener> listeners,
                                                                        RuleEngineProperties properties) {
        List<RuleExecutionListener> ordered = new ArrayList<>(listeners);
        ordered.removeIf(RuleExecutionListenerComposite.class::isInstance);
        ordered.sort(AnnotationAwareOrderComparator.INSTANCE);
        return new RuleExecutionListenerComposite(ordered, properties.isListenerErrorIgnore());
    }
}
