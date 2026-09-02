package top.egon.cola.archetype.source.service.infrastructure.exam.mq.message;
public record ScoreRecordedMessage(
        Long scoreId, Long examId, Long courseId, Long studentId, int points) { }
