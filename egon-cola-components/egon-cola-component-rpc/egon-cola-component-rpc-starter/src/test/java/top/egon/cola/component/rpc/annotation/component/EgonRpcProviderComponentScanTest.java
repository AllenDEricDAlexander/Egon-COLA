package top.egon.cola.component.rpc.annotation.component;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

import static org.assertj.core.api.Assertions.assertThat;

class EgonRpcProviderComponentScanTest {

    @Test
    void shouldRegisterAnnotatedProviderThroughComponentScanning() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.scan(getClass().getPackageName());
            context.refresh();

            assertThat(context.getBeansOfType(ComponentScannedRpcProvider.class))
                    .hasSize(1);
        }
    }
}

@EgonRpcProvider
class ComponentScannedRpcProvider {
}
