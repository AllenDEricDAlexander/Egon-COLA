#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo;

import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ExamConverter;
import ${package}.infrastructure.exam.repo.impl.ExamRepositoryImpl;
import ${package}.infrastructure.exam.repo.mapper.ExamMapper;
import ${package}.infrastructure.exam.repo.po.ExamPo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamRepositoryTest {

    @Test
    void shouldInsertExamThroughMapper() {
        ExamMapper mapper = mock(ExamMapper.class);
        Exam exam = exam();
        when(mapper.selectById(1002L)).thenReturn(null);
        when(mapper.insert(any(ExamPo.class))).thenReturn(1);
        ExamRepositoryImpl repository = new ExamRepositoryImpl(
                mapper, new ExamConverter(), new EvaluationPersistenceValidator());

        Exam result = repository.save(exam);

        assertThat(result.getId()).isEqualTo(new ExamId(1002L));
        verify(mapper).insert(any(ExamPo.class));
    }

    private static Exam exam() {
        return new Exam(
                new ExamId(1002L), new CourseId(1001L), "Midterm",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), ExamStatus.DRAFT);
    }
}
