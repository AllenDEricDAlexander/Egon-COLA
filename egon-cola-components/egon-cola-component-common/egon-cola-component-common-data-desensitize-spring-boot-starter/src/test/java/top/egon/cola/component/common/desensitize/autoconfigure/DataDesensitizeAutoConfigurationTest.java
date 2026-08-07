package top.egon.cola.component.common.desensitize.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.common.desensitize.annotation.Sensitive;
import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.jackson.SensitiveJacksonModule;
import top.egon.cola.component.common.desensitize.logback.SensitiveLogbackRegistryBridge;
import top.egon.cola.component.common.desensitize.logback.SensitiveLogs;
import top.egon.cola.component.common.desensitize.metadata.SensitiveMetadataResolver;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategy;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class DataDesensitizeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(
                    AutoConfigurations.of(
                            DataDesensitizeAutoConfiguration.class,
                            JacksonAutoConfiguration.class
                    )
            );

    @Test
    void registersSharedInfrastructureAndInstallsJacksonModule() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SensitiveMetadataResolver.class);
            assertThat(context).hasSingleBean(SensitiveStrategyRegistry.class);
            assertThat(context).hasSingleBean(SensitiveJacksonModule.class);
            assertThat(context).hasSingleBean(SensitiveLogbackRegistryBridge.class);
            assertThat(context).hasSingleBean(ObjectMapper.class);

            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            String json = objectMapper.writeValueAsString(
                    new ResponseDto("13812345678")
            );
            assertThat(json).contains("138****5678");
            assertThat(json).doesNotContain("13812345678");
        });
    }

    @Test
    void customStrategyBeanOverridesOnlyItsSensitiveType() {
        contextRunner.withBean(SensitiveStrategy.class, () -> new SensitiveStrategy() {
            @Override
            public SensitiveType type() {
                return SensitiveType.MOBILE;
            }

            @Override
            public String mask(String value) {
                return "custom-mobile";
            }
        }).run(context -> {
            SensitiveStrategyRegistry registry = context.getBean(
                    SensitiveStrategyRegistry.class
            );
            assertThat(registry.mask(SensitiveType.MOBILE, "13812345678"))
                    .isEqualTo("custom-mobile");
            assertThat(registry.mask(SensitiveType.NAME, "张三"))
                    .isEqualTo("张*");
            assertThat(SensitiveLogs.of("13812345678", SensitiveType.MOBILE))
                    .isEqualTo("custom-mobile");
        });
    }

    @Test
    void backsOffForApplicationRegistry() {
        SensitiveStrategyRegistry applicationRegistry =
                SensitiveStrategyRegistry.defaults();

        contextRunner.withBean(
                SensitiveStrategyRegistry.class,
                () -> applicationRegistry
        ).run(context -> assertThat(context.getBean(
                SensitiveStrategyRegistry.class
        )).isSameAs(applicationRegistry));
    }

    private static class ResponseDto {

        @Sensitive(type = SensitiveType.MOBILE)
        private final String mobile;

        ResponseDto(String mobile) {
            this.mobile = mobile;
        }

        public String getMobile() {
            return mobile;
        }
    }
}
