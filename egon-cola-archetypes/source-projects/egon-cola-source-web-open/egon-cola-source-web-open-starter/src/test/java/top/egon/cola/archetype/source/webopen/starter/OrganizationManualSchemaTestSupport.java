package top.egon.cola.archetype.source.webopen.starter;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** Existing flow-test entry point now enforces the isolated fixture's ownership instead of opening JDBC connections. */
final class OrganizationManualSchemaTestSupport {
    private OrganizationManualSchemaTestSupport() { }

    public static final class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            if (!Boolean.FALSE.equals(context.getEnvironment().getProperty(
                    "egon.cola.component.mybatis-plus.ddl.enabled", Boolean.class))) {
                throw new IllegalStateException("Flow tests must inherit PersistenceTestSupport and disable production DDL");
            }
        }
    }
}
