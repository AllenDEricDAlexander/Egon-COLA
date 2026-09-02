package top.egon.cola.archetype.source.serviceopen.infrastructure.exam.mq.message;
public record ScoreRecordedMessage(
        Long scoreId, Long examId, Long courseId, Long studentId, int points) { }
