package top.egon.cola.component.tianshu.admin.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcAdminTransportSecurityValidatorTest {

    @Test
    void rejectsUnconfiguredTransportMode() {
        assertThatThrownBy(() ->
                DdcAdminTransportSecurityValidator.validate(
                        new MockEnvironment(),
                        "egon.cola.component.tianshu.admin.transport-security"
                )
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(".mode is required");
    }
}
