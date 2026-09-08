package top.egon.cola.archetype.source.light.adapter.user.rpc;

import top.egon.cola.archetype.source.light.facade.rpc.LightRpcConverter;
import top.egon.cola.archetype.source.light.facade.rpc.proto.*;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import org.mapstruct.factory.Mappers;
import jakarta.validation.Validation;

import top.egon.cola.archetype.source.light.facade.user.PermissionFacade;
import top.egon.cola.archetype.source.light.facade.user.dto.PermissionDetailDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionRpcProviderTest {
    private final LightRpcConverter converter = Mappers.getMapper(LightRpcConverter.class);
    private final jakarta.validation.ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(validators.getValidator());

    @org.junit.jupiter.api.AfterEach
    void closeValidationFactory() {
        validators.close();
    }

    @Test
    void delegates_permission_query_to_facade() {
        PermissionFacade facade = mock(PermissionFacade.class);
        List<PermissionDetailDTO> expected = List.of(
                new PermissionDetailDTO("course:read", "Read courses", List.of()));
        when(facade.getUserPermissions(1001L)).thenReturn(expected);

        var result = new PermissionRpcProvider(facade, converter, validation).getUserPermissions(GetUserPermissionsRpcRequest.newBuilder().setUserId(1001L).build());
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getDataList().stream().map(converter::toSource).toList()).isEqualTo(expected);
    }
}
