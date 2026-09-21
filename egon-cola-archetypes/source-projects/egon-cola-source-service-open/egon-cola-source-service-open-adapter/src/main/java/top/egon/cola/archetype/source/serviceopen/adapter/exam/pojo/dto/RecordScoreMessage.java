package top.egon.cola.archetype.source.serviceopen.adapter.exam.pojo.dto;

import java.time.Instant;
import top.egon.cola.component.common.core.pojo.BasePojo;

/** Score-record command consumed from the {@code SCORE_COMMAND} route. */
public record RecordScoreMessage(
        String messageId, Long examId, Long studentId, int points, Instant occurredAt)
        implements BasePojo { }
