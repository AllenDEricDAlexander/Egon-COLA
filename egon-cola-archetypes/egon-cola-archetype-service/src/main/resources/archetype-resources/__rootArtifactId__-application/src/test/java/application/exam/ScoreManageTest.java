#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam;

import ${package}.application.exam.command.RecordScoreCommand;
import ${package}.application.exam.converter.ExamApplicationConverter;
import ${package}.application.exam.manage.impl.ScoreManageImpl;
import ${package}.application.exam.query.GetScoreQuery;
import ${package}.application.exam.validators.ExamApplicationValidator;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.enums.ScoreStatus;
import ${package}.domain.exam.event.ExamEventPublisher;
import ${package}.domain.exam.repos.ExamPaperRepository;
import ${package}.domain.exam.repos.ExamRepository;
import ${package}.domain.exam.repos.ScoreRepository;
import ${package}.domain.exam.service.impl.ScoreDomainServiceImpl;
import ${package}.domain.exam.vos.ExamId;
import ${package}.domain.exam.vos.ScoreValue;
import ${package}.domain.course.vos.CourseId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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
                examRepository, paperRepository, scoreRepository, eventPublisher,
                new ScoreDomainServiceImpl(),
                new ExamApplicationConverter(), new ExamApplicationValidator());

        var result = manage.record(new RecordScoreCommand("exam-1", "student-1", 90));

        assertEquals(90, result.points());
        verify(scoreRepository).save(any(Score.class));
        verify(eventPublisher).scoreRecorded(any(Score.class));
    }

    @Test
    void shouldGetScoreWithinExamShard() {
        ExamRepository examRepository = mock(ExamRepository.class);
        ExamPaperRepository paperRepository = mock(ExamPaperRepository.class);
        ScoreRepository scoreRepository = mock(ScoreRepository.class);
        ExamEventPublisher eventPublisher = mock(ExamEventPublisher.class);
        Score score = new Score(
            "score-1", new ExamId("exam-1"), new CourseId("course-1"),
            "student-1", new ScoreValue(90), ScoreStatus.RECORDED);
        when(scoreRepository.findByExamIdAndId(new ExamId("exam-1"), "score-1"))
            .thenReturn(Optional.of(score));
        ScoreManageImpl manage = new ScoreManageImpl(
            examRepository, paperRepository, scoreRepository, eventPublisher,
            new ScoreDomainServiceImpl(),
            new ExamApplicationConverter(), new ExamApplicationValidator());

        var result = manage.get(new GetScoreQuery("exam-1", "score-1"));

        assertEquals("score-1", result.id());
        verify(scoreRepository).findByExamIdAndId(new ExamId("exam-1"), "score-1");
    }
}
