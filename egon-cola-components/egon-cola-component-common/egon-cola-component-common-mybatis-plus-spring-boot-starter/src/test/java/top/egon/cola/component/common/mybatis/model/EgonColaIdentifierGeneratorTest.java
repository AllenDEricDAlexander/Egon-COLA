package top.egon.cola.component.common.mybatis.model;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EgonColaIdentifierGeneratorTest {

    @Test
    void delegatesOnlyToTheExistingDistributedIdGenerator() {
        LongIdGenerator delegate = mock(LongIdGenerator.class);
        when(delegate.nextLongId()).thenReturn(9223372036854775000L);
        EgonColaIdentifierGenerator generator = new EgonColaIdentifierGenerator(delegate);
        assertThat(generator.nextId(new TestBusinessModel())).isEqualTo(9223372036854775000L);
        verify(delegate, times(1)).nextLongId();
        assertThat(generator.assignId(null)).isTrue();
        assertThat(generator.assignId(123L)).isFalse();
    }

    @Test
    void rejectsNonEgonEntitiesAndNeverFallsBackAfterGeneratorFailure() {
        LongIdGenerator delegate = mock(LongIdGenerator.class);
        EgonColaIdentifierGenerator generator = new EgonColaIdentifierGenerator(delegate);
        assertThatThrownBy(() -> generator.nextId(new Object())).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(delegate);
        when(delegate.nextLongId()).thenThrow(new IllegalStateException("CLOCK_MOVED_BACKWARDS"));
        assertThatThrownBy(() -> generator.nextId(new TestBusinessModel())).hasMessage("CLOCK_MOVED_BACKWARDS");
    }
}
