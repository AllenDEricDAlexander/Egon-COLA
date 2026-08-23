#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.identity;

import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.vos.ExamId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceLongIdentityContractTest {

    @Test
    void technical_ids_are_positive_longs() {
        assertEquals(42L, new CourseId(42L).value());
        assertEquals(43L, new ExamId(43L).value());
    }

    @Test
    void non_positive_ids_are_rejected_before_persistence() {
        assertThrows(RuntimeException.class, () -> new CourseId(0L));
        assertThrows(RuntimeException.class, () -> new ExamId(-1L));
    }
}
