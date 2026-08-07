package top.egon.cola.component.ddc.admin.service.config;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcConfigValueGuardTest {

    @Test
    void measuresUtf8BytesAndNeverEchoesTheRejectedValue() {
        DdcConfigValueGuard guard = new DdcConfigValueGuard(4);

        assertThatCode(() -> guard.check(null)).doesNotThrowAnyException();
        assertThatCode(() -> guard.check("你a")).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.check("你ab"))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("4")
                .hasMessageNotContaining("你ab");
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> new DdcConfigValueGuard(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }
}
