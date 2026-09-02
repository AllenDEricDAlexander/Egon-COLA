package top.egon.cola.archetype.source.light.adapter.teaching.rpc;

import top.egon.cola.archetype.source.light.facade.teaching.SchoolClassFacade;
import top.egon.cola.archetype.source.light.facade.teaching.dto.SchoolClassDetailDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchoolClassRpcProviderTest {
    @Test
    void delegates_school_class_query_to_facade() {
        SchoolClassFacade facade = mock(SchoolClassFacade.class);
        SchoolClassDetailDTO expected = new SchoolClassDetailDTO(
                1003L, "Class One", "2026-FALL", "ACTIVE", 2);
        when(facade.getSchoolClass(1003L)).thenReturn(expected);

        assertThat(new SchoolClassRpcProvider(facade).getSchoolClass(1003L))
                .isSameAs(expected);
    }
}
