package top.egon.cola.component.common.mybatis.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class EgonColaIdentifierGeneratorTest {

    private static final long MACHINE_ID = 0L;

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(MACHINE_ID, Duration.ofMillis(5));
    }

    @Test
    void takesEveryIdFromTheProcessWideStaticGenerator() {
        EgonColaIdentifierGenerator generator = new EgonColaIdentifierGenerator();

        long first = generator.nextId(new TestBusinessModel());
        long second = generator.nextId(new TestBusinessModel());

        assertThat(first).isPositive();
        assertThat(second).isGreaterThan(first);
        assertThat(second >>> 12 & 1023L).isEqualTo(MACHINE_ID);
        assertThat(generator.assignId(null)).isTrue();
        assertThat(generator.assignId(123L)).isFalse();
    }

    @Test
    void rejectsNonEgonEntitiesAndNeverFallsBackToAnInventedId() {
        EgonColaIdentifierGenerator generator = new EgonColaIdentifierGenerator();

        assertThatThrownBy(() -> generator.nextId(new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("EGON_MODEL_REQUIRED");
        assertThat(generator.nextId(new TestBusinessModel())).isPositive();
    }
}
