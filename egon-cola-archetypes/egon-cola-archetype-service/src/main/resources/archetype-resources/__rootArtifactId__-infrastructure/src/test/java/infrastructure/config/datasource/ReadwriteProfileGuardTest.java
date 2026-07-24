package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ReadwriteProfileGuardTest {

    @Test
    void shouldAcceptReadwriteTogetherWithSharding() {
        assertThatCode(() -> new ReadwriteProfileGuard(
                        new MockEnvironment().withProperty(
                                "spring.profiles.active",
                                "test,sharding,readwrite")))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectReadwriteWithoutSharding() {
        assertThatThrownBy(() -> new ReadwriteProfileGuard(
                        new MockEnvironment().withProperty(
                                "spring.profiles.active",
                                "test,readwrite")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sharding");
    }
}
