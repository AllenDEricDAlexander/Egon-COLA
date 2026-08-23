package ${package}.adapter.handler;

import ${package}.application.user.manage.UserUseCaseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    @Test
    void exposes_application_code_without_internal_details() {
        ApiResponse<Void> response = new GlobalExceptionHandler().handleUserFailure(
                new UserUseCaseException("USER_EXISTS", "User exists", new IllegalStateException("secret")));
        assertThat(response.code()).isEqualTo("USER_EXISTS");
        assertThat(response.message()).isEqualTo("User exists");
        assertThat(response.toString()).doesNotContain("secret");
    }
}
