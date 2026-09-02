package top.egon.cola.archetype.source.serviceopen.infrastructure.exam.repo;

import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Score;
import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ScoreStatus;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ScoreValue;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.repo.converter.ScoreConverter;
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
