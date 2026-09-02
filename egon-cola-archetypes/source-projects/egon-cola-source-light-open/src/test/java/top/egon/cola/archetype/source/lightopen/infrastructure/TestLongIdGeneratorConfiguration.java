package top.egon.cola.archetype.source.lightopen.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

@TestConfiguration(proxyBeanMethods = false)
public class TestLongIdGeneratorConfiguration {
    @Bean
    LongIdGenerator testLongIdGenerator() {
        return () -> 2001L;
    }
}
