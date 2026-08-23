#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo;

import ${package}.domain.common.Page;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.enums.ScoreStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.domain.exam.vos.ScoreValue;
import ${package}.infrastructure.exam.repo.converter.ScoreConverter;
import ${package}.infrastructure.exam.repo.impl.ScoreRepositoryImpl;
import ${package}.infrastructure.exam.repo.mapper.ScoreMapper;
import ${package}.infrastructure.exam.repo.po.ScorePo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScoreRepositoryTest {

    @Test
    void shouldPersistScoreAndCheckDuplicateByExamAndStudent() {
        ScoreMapper mapper = mock(ScoreMapper.class);
        Score score = score();
        when(mapper.selectByExamIdAndId(1002L, 1004L)).thenReturn(null);
        when(mapper.insert(any(ScorePo.class))).thenReturn(1);
        when(mapper.countByExamIdAndStudentId(1002L, 2001L)).thenReturn(1L);
        ScoreRepositoryImpl repository = new ScoreRepositoryImpl(
                mapper, new ScoreConverter(), new EvaluationPersistenceValidator());

        Score result = repository.save(score);

        assertThat(result.getId()).isEqualTo(1004L);
        assertThat(repository.existsByExamIdAndStudentId(new ExamId(1002L), 2001L))
                .isTrue();
        verify(mapper).insert(any(ScorePo.class));
    }

    @Test
    void shouldPageScoresWithCreatedAtAndIdOrderContract() {
        ScoreMapper mapper = mock(ScoreMapper.class);
        ScorePo row = new ScorePo(
                1004L, 1002L, 1001L, 2001L, 92, "RECORDED",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1));
        when(mapper.selectPageByExamId(1002L, 0L, 2)).thenReturn(List.of(row));
        when(mapper.countByExamId(1002L)).thenReturn(3L);
        ScoreRepositoryImpl repository = new ScoreRepositoryImpl(
                mapper, new ScoreConverter(), new EvaluationPersistenceValidator());

        Page<Score> page = repository.findPageByExamId(new ExamId(1002L), 1, 2);

        assertThat(page.records()).extracting(Score::getId).containsExactly(1004L);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.totalCount()).isEqualTo(3L);
        verify(mapper).selectPageByExamId(1002L, 0L, 2);
    }

    private static Score score() {
        return new Score(
                1004L, new ExamId(1002L), new CourseId(1001L),
                2001L, new ScoreValue(90), ScoreStatus.RECORDED);
    }
}
