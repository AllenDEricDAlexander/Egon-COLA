#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam;

import ${package}.application.exam.command.RecordScoreCommand;
import ${package}.application.exam.converter.ExamApplicationConverter;
import ${package}.application.exam.manage.impl.ScoreManageImpl;
import ${package}.application.exam.validators.ExamApplicationValidator;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.exam.event.ExamEventPublisher;
import ${package}.domain.exam.service.ExamDomainService;
import ${package}.domain.exam.service.ScoreDomainService;
import ${package}.domain.exam.vos.ExamId;
import ${package}.domain.course.vos.CourseId;
import java.time.Instant;
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
        ExamDomainService exams = mock(ExamDomainService.class);
        ScoreDomainService scores = mock(ScoreDomainService.class);
        Exam exam = TestEvaluationModels.publishedExam();
        ExamPaper paper = TestEvaluationModels.publishedPaper();
        when(exams.findById(new ExamId(4001L))).thenReturn(Optional.of(exam));
        when(exams.findPaperByExamId(new ExamId(4001L))).thenReturn(Optional.of(paper));
        when(scores.existsByExamIdAndStudentId(new ExamId(4001L), 6001L)).thenReturn(false);
        when(scores.recordScore(any(), any(), any(), any(Integer.class), any(Boolean.class)))
                .thenReturn(TestEvaluationModels.recordedScore());
        when(scores.save((Score) any())).thenAnswer(invocation -> invocation.getArgument(0));
        ExamEventPublisher events = mock(ExamEventPublisher.class);
        ScoreManageImpl manage = new ScoreManageImpl(
                exams, scores, events, new ExamApplicationConverter(), new ExamApplicationValidator());

        var result = manage.record(new RecordScoreCommand(4001L, 6001L, 90));

        assertEquals(7001L, result.id());
        assertEquals(90, result.points());
        verify(scores).save((Score) any());
        verify(events).scoreRecorded(any());
    }
}
