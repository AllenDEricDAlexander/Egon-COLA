package top.egon.cola.archetype.source.lightopen.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Retained as the light-open test configuration anchor; the fixed-value ID generator seam is gone
 * because generation now comes from the process-wide static Snowflake engine.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestLongIdGeneratorConfiguration {
}
