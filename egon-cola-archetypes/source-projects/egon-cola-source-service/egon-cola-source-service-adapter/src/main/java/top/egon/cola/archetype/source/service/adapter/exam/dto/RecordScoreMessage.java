package top.egon.cola.archetype.source.service.adapter.exam.dto;
import java.time.Instant;
public record RecordScoreMessage(
        String messageId, Long examId, Long studentId, int points, Instant occurredAt) { }
