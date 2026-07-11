#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam;

import ${package}.application.command.exam.RecordScoreCommand;
import ${package}.application.converter.exam.ExamApplicationConverter;
import ${package}.application.manage.exam.impl.ScoreManageImpl;
import ${package}.application.validators.exam.ExamApplicationValidator;
import ${package}.domain.entities.exam.Exam;
import ${package}.domain.entities.exam.ExamPaper;
import ${package}.domain.entities.exam.Score;
import ${package}.domain.event.exam.ExamEventPublisher;
import ${package}.domain.repos.exam.ExamPaperRepository;
import ${package}.domain.repos.exam.ExamRepository;
import ${package}.domain.repos.exam.ScoreRepository;
import ${package}.domain.service.exam.impl.ScoreDomainServiceImpl;
import ${package}.domain.vos.exam.ExamId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScoreManageTest {

    @Test
    void shouldPersistAndPublishRecordedScore() {
        ExamRepository examRepository = mock(ExamRepository.class);
        ExamPaperRepository paperRepository = mock(ExamPaperRepository.class);
        ScoreRepository scoreRepository = mock(ScoreRepository.class);
        ExamEventPublisher eventPublisher = mock(ExamEventPublisher.class);
        Exam exam = TestEvaluationModels.publishedExam();
        ExamPaper paper = TestEvaluationModels.publishedPaper();
        when(examRepository.findById(new ExamId("exam-1"))).thenReturn(Optional.of(exam));
        when(paperRepository.findByExamId(new ExamId("exam-1"))).thenReturn(Optional.of(paper));
        when(scoreRepository.existsByExamIdAndStudentId(new ExamId("exam-1"), "student-1"))
                .thenReturn(false);
        when(scoreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ScoreManageImpl manage = new ScoreManageImpl(
                provider(examRepository), provider(paperRepository), provider(scoreRepository),
                provider(eventPublisher), new ScoreDomainServiceImpl(),
                new ExamApplicationConverter(), new ExamApplicationValidator());

        var result = manage.record(new RecordScoreCommand("exam-1", "student-1", 90));

        assertEquals(90, result.points());
        verify(scoreRepository).save(any(Score.class));
        verify(eventPublisher).scoreRecorded(any(Score.class));
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(value);
        return provider;
    }
}
