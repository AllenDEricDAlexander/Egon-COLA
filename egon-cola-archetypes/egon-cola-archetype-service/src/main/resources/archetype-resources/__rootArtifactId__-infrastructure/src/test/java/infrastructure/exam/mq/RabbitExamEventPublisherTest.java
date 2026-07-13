#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.mq;

import ${package}.domain.course.entities.Course;
import ${package}.domain.exam.service.impl.ExamDomainServiceImpl;
import ${package}.domain.exam.service.impl.ScoreDomainServiceImpl;
import ${package}.domain.course.vos.CourseCode;
import ${package}.infrastructure.exam.mq.RabbitExamEventPublisher;
import ${package}.infrastructure.exam.mq.message.ScoreRecordedMessage;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitExamEventPublisherTest {

    @Test
    void shouldPublishScoreRecordedMessage() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitExamEventPublisher publisher = new RabbitExamEventPublisher(
                template, "evaluation.events", "exam.published", "score.recorded");
        Course course = Course.create("course-1", new CourseCode("MATH-101"), "Math", 3);
        var examService = new ExamDomainServiceImpl();
        var exam = examService.createExam(
                "exam-1", course, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        var paper = examService.attachPaper("paper-1", exam, "Paper", 100);
        examService.publishExam(exam, paper);
        var score = new ScoreDomainServiceImpl().recordScore(
                "score-1", exam, paper, "student-1", 90, false);

        publisher.scoreRecorded(score);

        verify(template).convertAndSend(
                eq("evaluation.events"), eq("score.recorded"),
                argThat((Object message) ->
                        ((ScoreRecordedMessage) message).scoreId().equals("score-1")));
    }
}
