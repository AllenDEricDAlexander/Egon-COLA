package top.egon.cola.archetype.source.light.adapter.handler;

import top.egon.cola.archetype.source.light.common.exception.UserUseCaseException;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.ResultRecord;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    @Test
    void exposes_application_code_without_internal_details() {
        ResultRecord<Void> response = new GlobalExceptionHandler().handleUserFailure(
                new UserUseCaseException("USER_EXISTS", "User exists", new IllegalStateException("secret")));
        assertThat(response.code()).isEqualTo(600000);
        assertThat(response.status()).isEqualTo("USER_EXISTS");
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("User exists");
        assertThat(response.toString()).doesNotContain("secret");
    }
}
