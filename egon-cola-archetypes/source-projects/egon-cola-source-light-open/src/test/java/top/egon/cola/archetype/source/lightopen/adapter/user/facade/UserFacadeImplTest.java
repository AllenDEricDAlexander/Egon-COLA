package top.egon.cola.archetype.source.lightopen.adapter.user.facade;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.lightopen.adapter.user.facade.impl.UserFacadeImpl;
import top.egon.cola.archetype.source.lightopen.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.lightopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.lightopen.common.exception.UserFacadeException;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.CreateUserDTO;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserFacadeImplTest {
    private final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final UserManage userManage = mock(UserManage.class);
    private final RoleManage roleManage = mock(RoleManage.class);
    private final UserFacadeImpl facade =
            new UserFacadeImpl(userManage, roleManage, new ValidationUtils(validators.getValidator()));

    @AfterEach
    void closeValidationFactory() {
        validators.close();
    }

    @Test
    void converts_application_result() {
        when(userManage.create(any())).thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));
        assertThat(facade.createUser(new CreateUserDTO("ext-1", "Mario", "mario@example.com", "operator-1", "request-1")).id())
                .isEqualTo(1001L);
    }

    @Test
    void rejects_an_out_of_contract_carrier_before_the_use_case_runs() {
        assertThatThrownBy(() -> facade.createUser(new CreateUserDTO("ext-1", " ", "mario@example.com", "operator-1", "request-1")))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        org.mockito.Mockito.verifyNoInteractions(userManage);
    }

    @Test
    void maps_use_case_error_to_facade_contract_without_cause() {
        when(userManage.create(any())).thenThrow(new UserUseCaseException("USER_EXISTS", "User already exists", new IllegalStateException("internal")));
        assertThatThrownBy(() -> facade.createUser(new CreateUserDTO("ext-1", "Mario", "mario@example.com", "operator-1", "request-1")))
                .isInstanceOfSatisfying(UserFacadeException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo("USER_EXISTS");
                    assertThat(error.getCause()).isNull();
                });
    }
}
