package top.egon.cola.archetype.source.service.infrastructure.exam.mq.message;

import top.egon.cola.component.common.core.pojo.BasePojo;

/** Score publication event published on the {@code SCORE_RECORDED} route. */
public record ScoreRecordedMessage(
        Long scoreId, Long examId, Long courseId, Long studentId, int points)
        implements BasePojo { }
