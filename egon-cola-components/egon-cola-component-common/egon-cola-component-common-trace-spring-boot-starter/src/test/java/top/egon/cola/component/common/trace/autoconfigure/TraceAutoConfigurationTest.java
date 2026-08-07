package top.egon.cola.component.common.trace.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TraceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(TraceAutoConfiguration.class));

    @Test
    void backsOffWhenDisabled() {
        contextRunner
                .withPropertyValues("egon.cola.component.trace.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(TraceRestClientCustomizer.class));
    }

    @Test
    void registersRestClientCustomizerByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TraceRestClientCustomizer.class);
            assertThat(context).doesNotHaveBean(TraceTaskDecorator.class);
        });
    }

    @Test
    void backsOffFromCustomRestClientCustomizer() {
        contextRunner
                .withBean(TraceRestClientCustomizer.class,
                        () -> new TraceRestClientCustomizer(new TraceProperties()))
                .run(context -> assertThat(context)
                        .hasSingleBean(TraceRestClientCustomizer.class));
    }

    @Test
    void servletFilterIsConditionalOnServletWebApplication() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TraceAutoConfiguration.class))
                .run(context -> assertThat(context)
                        .hasSingleBean(TraceServletFilter.class));
    }
}
