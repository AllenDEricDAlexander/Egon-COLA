#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo;

import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.enums.ScoreStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.domain.exam.vos.ScoreValue;
import ${package}.infrastructure.exam.repo.converter.ScoreConverter;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreRepositoryTest {
    @Test
    void shouldRoundTripScore() {
        Score score = new Score(7001L, new ExamId(4001L), new CourseId(1001L),
                6001L, new ScoreValue(90), ScoreStatus.RECORDED);
        ScoreConverter converter = Mappers.getMapper(ScoreConverter.class);
        var target = converter.toTarget(score);
        target.setId(score.getId());
        Score restored = converter.toSource(target);
        assertEquals(7001L, restored.getId());
        assertEquals(6001L, restored.getStudentId());
    }
}
