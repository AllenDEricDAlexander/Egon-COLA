#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo;

import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ExamPaperConverter;
import ${package}.infrastructure.exam.repo.impl.ExamPaperRepositoryImpl;
import ${package}.infrastructure.exam.repo.mapper.ExamPaperMapper;
import ${package}.infrastructure.exam.repo.po.ExamPaperPo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamPaperRepositoryTest {

    @Test
    void shouldPersistAssignedPaperIdThroughMapper() {
        ExamPaperMapper mapper = mock(ExamPaperMapper.class);
        ExamPaper paper = paper();
        when(mapper.selectByExamIdAndId(1002L, 1003L)).thenReturn(null);
        when(mapper.insert(any(ExamPaperPo.class))).thenReturn(1);
        ExamPaperRepositoryImpl repository = new ExamPaperRepositoryImpl(
                mapper, new ExamPaperConverter(),
                new EvaluationPersistenceValidator());

        ExamPaper result = repository.save(paper);

        assertThat(result.getId()).isEqualTo(1003L);
        verify(mapper).insert(any(ExamPaperPo.class));
    }

    @Test
    void shouldUseExamIdForPointLookup() {
        ExamPaperMapper mapper = mock(ExamPaperMapper.class);
        when(mapper.selectByExamId(1002L)).thenReturn(null);
        ExamPaperRepositoryImpl repository = new ExamPaperRepositoryImpl(
                mapper, new ExamPaperConverter(),
                new EvaluationPersistenceValidator());

        assertThat(repository.findByExamId(new ExamId(1002L))).isEmpty();
        verify(mapper).selectByExamId(1002L);
    }

    private static ExamPaper paper() {
        return new ExamPaper(
                1003L, new ExamId(1002L), "Paper", 100, ExamPaperStatus.DRAFT);
    }
}
