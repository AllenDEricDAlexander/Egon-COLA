package top.egon.cola.archetype.source.service.infrastructure.exam.mq.message;
import java.time.Instant;
public record ExamPublishedMessage(
        Long examId, Long courseId, Long paperId, Instant publishedAt) { }
