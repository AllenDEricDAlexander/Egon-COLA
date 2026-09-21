package top.egon.cola.archetype.source.service.infrastructure.exam.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.service.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.service.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.service.domain.exam.entities.Score;
import top.egon.cola.archetype.source.service.domain.exam.service.ExamEventService;
import top.egon.cola.archetype.source.service.infrastructure.exam.mq.message.ExamPublishedMessage;
import top.egon.cola.archetype.source.service.infrastructure.exam.mq.message.ScoreRecordedMessage;
import top.egon.cola.archetype.source.service.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.service.infrastructure.mq.MqRouteEnum;

import java.time.Instant;

/** Resolves the declared exam routes and hands every message to the single MQ boundary. */
@Validated
@Service("examEventService")
@RequiredArgsConstructor
@Slf4j
public class ExamEventServiceImpl implements ExamEventService {
    @Qualifier("mqMessageService")
    private final MqMessageService mqMessageService;

    @Override
    public void examPublished(Exam exam, ExamPaper paper) {
        mqMessageService.publish(MqRouteEnum.EXAM_PUBLISHED, new ExamPublishedMessage(
                exam.getId().value(), exam.getCourseId().value(), paper.getId(), Instant.now()));
    }

    @Override
    public void scoreRecorded(Score score) {
        mqMessageService.publish(MqRouteEnum.SCORE_RECORDED, new ScoreRecordedMessage(
                score.getId(), score.getExamId().value(), score.getCourseId().value(),
                score.getStudentId(), score.getPoints().value()));
    }
}
