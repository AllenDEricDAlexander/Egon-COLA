package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcTestDependencyBoundaryTest {

    @Test
    void adminImplementationIsNotOnTheTestClasspath() {
        assertThatThrownBy(() -> Class.forName(
                "top.egon.cola.component.ddc.admin.service.config.DdcConfigService"
        )).isInstanceOf(ClassNotFoundException.class);
    }
}
