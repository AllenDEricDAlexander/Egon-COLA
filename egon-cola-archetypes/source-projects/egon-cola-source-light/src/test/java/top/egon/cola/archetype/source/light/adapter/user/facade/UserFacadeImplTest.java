package top.egon.cola.archetype.source.light.adapter.user.facade;

import top.egon.cola.archetype.source.light.adapter.user.facade.impl.UserFacadeImpl;
import top.egon.cola.archetype.source.light.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserUseCaseException;
import top.egon.cola.archetype.source.light.application.user.result.UserResult;
import top.egon.cola.archetype.source.light.facade.user.dto.CreateUserDTO;
import top.egon.cola.archetype.source.light.facade.user.exceptions.UserFacadeException;
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
        when(userManage.create(any())).thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));
        assertThat(facade.createUser(new CreateUserDTO("ext-1", "Mario", "mario@example.com", "operator-1", "request-1")).id())
                .isEqualTo(1001L);
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
