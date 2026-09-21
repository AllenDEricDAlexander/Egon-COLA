package top.egon.cola.archetype.source.serviceopen.infrastructure.exam.service.impl;

import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Score;
import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ExamPaperStatus;
import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ExamStatus;
import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ScoreStatus;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ScoreValue;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.mq.message.ExamPublishedMessage;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.mq.message.ScoreRecordedMessage;
import top.egon.cola.archetype.source.serviceopen.infrastructure.mq.MqRouteEnum;
import top.egon.cola.archetype.source.serviceopen.infrastructure.support.RecordingMqMessageService;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ExamEventServiceImplTest {

    private final RecordingMqMessageService messages = new RecordingMqMessageService();
    private final ExamEventServiceImpl service = new ExamEventServiceImpl(messages);

    private final Exam exam = new Exam(
            new ExamId(4001L), new CourseId(1001L), "Midterm",
            Instant.EPOCH, Instant.EPOCH.plusSeconds(60), ExamStatus.PUBLISHED);
    private final ExamPaper paper = new ExamPaper(
            5001L, exam.getId(), "Midterm paper", 100, ExamPaperStatus.PUBLISHED);

    @Test
    void shouldPublishExamPublishedMessageOnTheDeclaredRoute() {
        service.examPublished(exam, paper);

        assertThat(messages.publications()).singleElement().satisfies(publication -> {
            assertThat(publication.route()).isEqualTo(MqRouteEnum.EXAM_PUBLISHED);
            assertThat(((ExamPublishedMessage) publication.payload()).examId()).isEqualTo(4001L);
        });
    }

    @Test
    void shouldPublishScoreRecordedMessageOnTheDeclaredRoute() {
        service.scoreRecorded(new Score(
                7001L, exam.getId(), new CourseId(1001L), 6001L,
                new ScoreValue(90), ScoreStatus.RECORDED));

        assertThat(messages.publications()).singleElement().satisfies(publication -> {
            assertThat(publication.route()).isEqualTo(MqRouteEnum.SCORE_RECORDED);
            assertThat(((ScoreRecordedMessage) publication.payload()).scoreId()).isEqualTo(7001L);
        });
    }
}
