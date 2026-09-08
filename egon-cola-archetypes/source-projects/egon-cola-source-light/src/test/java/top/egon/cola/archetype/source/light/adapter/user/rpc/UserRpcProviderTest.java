package top.egon.cola.archetype.source.light.adapter.user.rpc;

import top.egon.cola.archetype.source.light.facade.rpc.LightRpcConverter;
import top.egon.cola.archetype.source.light.facade.rpc.proto.*;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import org.mapstruct.factory.Mappers;
import jakarta.validation.Validation;

import top.egon.cola.archetype.source.light.facade.user.UserFacade;
import top.egon.cola.archetype.source.light.facade.user.dto.CreateUserDTO;
import top.egon.cola.archetype.source.light.facade.user.dto.UserDetailDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRpcProviderTest {
    private final LightRpcConverter converter = Mappers.getMapper(LightRpcConverter.class);
    private final jakarta.validation.ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(validators.getValidator());

    @org.junit.jupiter.api.AfterEach
    void closeValidationFactory() {
        validators.close();
    }

    @Test
    void delegates_to_facade_contract() {
        UserFacade delegate = mock(UserFacade.class);
        CreateUserDTO request = new CreateUserDTO("ext-1", "Mario", "mario@example.com", "operator-1", "request-1");
        UserDetailDTO response = new UserDetailDTO(1001L, "Mario", "mario@example.com", "ACTIVE");
        when(delegate.createUser(request)).thenReturn(response);

        UserRpcProvider provider = new UserRpcProvider(delegate, converter, validation);
        var result = provider.createUser(converter.toTarget(request));
        assertThat(result.getSuccess()).isTrue();
        assertThat(converter.toSource(result.getData())).isEqualTo(response);
        verify(delegate).createUser(request);
    }
}
