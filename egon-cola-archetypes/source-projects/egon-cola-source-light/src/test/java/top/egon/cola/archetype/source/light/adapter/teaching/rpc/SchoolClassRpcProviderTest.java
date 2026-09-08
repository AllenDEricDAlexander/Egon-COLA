package top.egon.cola.archetype.source.light.adapter.teaching.rpc;

import top.egon.cola.archetype.source.light.facade.rpc.LightRpcConverter;
import top.egon.cola.archetype.source.light.facade.rpc.proto.*;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import org.mapstruct.factory.Mappers;
import jakarta.validation.Validation;

import top.egon.cola.archetype.source.light.facade.teaching.SchoolClassFacade;
import top.egon.cola.archetype.source.light.facade.teaching.dto.SchoolClassDetailDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchoolClassRpcProviderTest {
    private final LightRpcConverter converter = Mappers.getMapper(LightRpcConverter.class);
    private final jakarta.validation.ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(validators.getValidator());

    @org.junit.jupiter.api.AfterEach
    void closeValidationFactory() {
        validators.close();
    }

    @Test
    void delegates_school_class_query_to_facade() {
        SchoolClassFacade facade = mock(SchoolClassFacade.class);
        SchoolClassDetailDTO expected = new SchoolClassDetailDTO(
                1003L, "Class One", "2026-FALL", "ACTIVE", 2);
        when(facade.getSchoolClass(1003L)).thenReturn(expected);

        var result = new SchoolClassRpcProvider(facade, converter, validation).getSchoolClass(GetSchoolClassRpcRequest.newBuilder().setSchoolClassId(1003L).build());
        assertThat(result.getSuccess()).isTrue();
        assertThat(converter.toSource(result.getData())).isEqualTo(expected);
    }
}
