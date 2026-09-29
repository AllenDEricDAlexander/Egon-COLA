package top.egon.cola.component.tianshu.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.tianshu.api.client.DdcPromptClient;
import top.egon.cola.component.tianshu.api.client.DdcSkillClient;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcAiResourceProperties;

/** Binds the opt-in AI client switches and fails when an enabled Port has no adapter. */
@AutoConfiguration(after = ValidationAutoConfiguration.class)
@EnableConfigurationProperties(DdcAiResourceProperties.class)
public class DdcAiResourceAutoConfiguration {

    @Bean("ddcAiPromptClientRequirement")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.tianshu.prompt",
            name = "enabled",
            havingValue = "true")
    @ConditionalOnMissingBean(DdcPromptClient.class)
    public DdcPromptClient ddcAiPromptClientRequirement() {
        throw new IllegalStateException(
                "egon.cola.component.tianshu.prompt.enabled requires a DdcPromptClient adapter");
    }

    @Bean("ddcAiSkillClientRequirement")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.tianshu.skill",
            name = "enabled",
            havingValue = "true")
    @ConditionalOnMissingBean(DdcSkillClient.class)
    public DdcSkillClient ddcAiSkillClientRequirement() {
        throw new IllegalStateException(
                "egon.cola.component.tianshu.skill.enabled requires a DdcSkillClient adapter");
    }
}
