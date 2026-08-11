package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAuthBootstrapControllerConditionTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(DdcAuthBootstrapController.class);

    @Test
    void doesNotRequireRbacBootstrapWhenRbacIsDisabled() {
        contextRunner
                .withPropertyValues("egon.cola.platform.rbac3.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(
                            DdcAuthBootstrapController.class
                    );
                });
    }
}
