package top.egon.cola.component.ddc.configuration.binding;

import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import top.egon.cola.component.ddc.annotation.DdcValue;
import top.egon.cola.component.ddc.configuration.binding.DdcBeanPostProcessor;
import top.egon.cola.component.ddc.configuration.binding.DdcFieldBinding;
import top.egon.cola.component.ddc.configuration.binding.DdcValueBindingRegistry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcFieldBindingServiceTest {

    @Test
    void usesSpringValueSemanticsForInitialInjectionAndRefresh() {
        Map<String, Object> values = new LinkedHashMap<>(Map.of(
                "order.rate-limit.permits-per-second", "120",
                "defaults.retry.max-attempts", "4",
                "order.rate-limit.timeout", "2s",
                "startup-only", "7"
        ));
        AnnotationConfigApplicationContext context = context(
                values,
                DdcFieldBindingService.class,
                SampleBean.class
        );
        SampleBean bean = context.getBean(SampleBean.class);
        DdcFieldBindingService service = context.getBean(
                DdcFieldBindingService.class
        );

        assertThat(bean.permitsPerSecond).isEqualTo(120);
        assertThat(bean.maxAttempts).isEqualTo(4);
        assertThat(bean.burstCapacity).isEqualTo(240);
        assertThat(bean.timeout).isEqualTo(Duration.ofSeconds(2));
        assertThat(bean.startupOnly).isEqualTo(7);

        values.put("order.rate-limit.permits-per-second", "150");
        values.put("order.retry.max-attempts", "6");
        values.put("order.rate-limit.timeout", "3s");
        values.put("startup-only", "9");
        DdcFieldBindingService.RefreshResult result = service.refresh();

        assertThat(bean.permitsPerSecond).isEqualTo(150);
        assertThat(bean.maxAttempts).isEqualTo(6);
        assertThat(bean.burstCapacity).isEqualTo(300);
        assertThat(bean.timeout).isEqualTo(Duration.ofSeconds(3));
        assertThat(bean.startupOnly).isEqualTo(7);
        assertThat(result.refreshedExpressions()).containsExactlyInAnyOrder(
                "${order.rate-limit.permits-per-second:100}",
                "${order.retry.max-attempts:${defaults.retry.max-attempts:3}}",
                "#{${order.rate-limit.permits-per-second:100} * 2}",
                "${order.rate-limit.timeout:1s}"
        );

        service.rollback(result);
        assertThat(bean.permitsPerSecond).isEqualTo(120);
        assertThat(bean.maxAttempts).isEqualTo(4);
        assertThat(bean.burstCapacity).isEqualTo(240);
        assertThat(bean.timeout).isEqualTo(Duration.ofSeconds(2));

        DdcValueBindingRegistry registry = context.getBean(
                DdcValueBindingRegistry.class
        );
        assertThat(registry.bindings()).hasSize(4);
        context.close();
        assertThat(registry.bindings()).isEmpty();
    }

    @Test
    void resolvesAllCandidatesBeforeWritingAnyField() {
        Map<String, Object> values = new LinkedHashMap<>(Map.of(
                "first", "1",
                "second", "2"
        ));
        AnnotationConfigApplicationContext context = context(
                values,
                FailingSecondWriteService.class,
                PairBean.class
        );
        PairBean bean = context.getBean(PairBean.class);
        FailingSecondWriteService service = context.getBean(
                FailingSecondWriteService.class
        );

        values.put("first", "5");
        values.put("second", "6");
        service.arm();

        assertThatThrownBy(service::refresh)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("second write failed");
        assertThat(bean.first).isEqualTo(1);
        assertThat(bean.second).isEqualTo(2);
        context.close();
    }

    @Test
    void missingPlaceholderWithoutDefaultFailsBeanCreation() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        configureBeanFactory(context);
        context.registerBean(MissingBean.class);

        assertThatThrownBy(context::refresh)
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unresolved.required.value");
        context.close();
    }

    private AnnotationConfigApplicationContext context(
            Map<String, Object> values,
            Class<? extends DdcFieldBindingService> serviceType,
            Class<?> beanType) {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();
        configureBeanFactory(context);
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test", values)
        );
        context.registerBean(DdcValueBindingRegistry.class);
        if (serviceType == FailingSecondWriteService.class) {
            context.registerBean(
                    FailingSecondWriteService.class,
                    () -> new FailingSecondWriteService(
                        context.getBean(DdcValueBindingRegistry.class),
                        context.getBeanFactory()
                )
            );
        } else {
            context.registerBean(
                    DdcFieldBindingService.class,
                    () -> new DdcFieldBindingService(
                        context.getBean(DdcValueBindingRegistry.class),
                        context.getBeanFactory()
                )
            );
        }
        context.registerBean(DdcBeanPostProcessor.class);
        context.registerBean(beanType);
        context.refresh();
        return context;
    }

    private void configureBeanFactory(
            AnnotationConfigApplicationContext context) {
        context.getBeanFactory().setConversionService(
                ApplicationConversionService.getSharedInstance()
        );
        context.getBeanFactory().addEmbeddedValueResolver(
                context.getEnvironment()::resolveRequiredPlaceholders
        );
    }

    static class SampleBean {

        @DdcValue("${order.rate-limit.permits-per-second:100}")
        private volatile int permitsPerSecond;

        @DdcValue("${order.retry.max-attempts:${defaults.retry.max-attempts:3}}")
        private volatile int maxAttempts;

        @DdcValue("#{${order.rate-limit.permits-per-second:100} * 2}")
        private volatile int burstCapacity;

        @DdcValue("${order.rate-limit.timeout:1s}")
        private volatile Duration timeout;

        @DdcValue(value = "${startup-only:5}", refreshable = false)
        private volatile int startupOnly;
    }

    static class PairBean {

        @DdcValue("${first:1}")
        private volatile int first;

        @DdcValue("${second:2}")
        private volatile int second;
    }

    static class MissingBean {

        @DdcValue("${unresolved.required.value}")
        private String value;
    }

    private static final class FailingSecondWriteService
            extends DdcFieldBindingService {

        private int writes;

        private boolean armed;

        private FailingSecondWriteService(
                DdcValueBindingRegistry registry,
                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
            super(registry, beanFactory);
        }

        private void arm() {
            writes = 0;
            armed = true;
        }

        @Override
        protected void write(DdcFieldBinding binding, Object value) {
            if (armed && writes++ == 1) {
                throw new IllegalStateException("second write failed");
            }
            super.write(binding, value);
        }
    }
}
