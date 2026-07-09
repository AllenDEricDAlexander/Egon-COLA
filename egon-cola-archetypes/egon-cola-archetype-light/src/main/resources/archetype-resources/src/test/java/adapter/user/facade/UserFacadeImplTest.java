package ${package}.adapter.user.facade;

import ${package}.adapter.user.facade.impl.UserFacadeImpl;
import ${package}.application.user.manage.RoleManage;
import ${package}.application.user.manage.UserManage;
import ${package}.application.user.manage.UserUseCaseException;
import ${package}.application.user.result.UserResult;
import ${package}.facade.user.dto.CreateUserDTO;
import ${package}.facade.user.exceptions.UserFacadeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserFacadeImplTest {
    private final UserManage userManage = mock(UserManage.class);
    private final RoleManage roleManage = mock(RoleManage.class);
    private final UserFacadeImpl facade = new UserFacadeImpl(userManage, roleManage);

    @Test
    void converts_application_result() {
        when(userManage.create(any())).thenReturn(new UserResult("user-1", "Mario", "mario@example.com", "ACTIVE"));
        assertThat(facade.createUser(new CreateUserDTO("ext-1", "Mario", "mario@example.com", "operator-1", "request-1")).id())
                .isEqualTo("user-1");
    }

    @Test
    void maps_use_case_error_to_facade_contract_without_cause() {
        when(userManage.create(any())).thenThrow(new UserUseCaseException("USER_EXISTS", "User already exists", new IllegalStateException("internal")));
        assertThatThrownBy(() -> facade.createUser(new CreateUserDTO("ext-1", "Mario", "mario@example.com", "operator-1", "request-1")))
                .isInstanceOfSatisfying(UserFacadeException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("USER_EXISTS");
                    assertThat(error.getCause()).isNull();
                });
    }
}
