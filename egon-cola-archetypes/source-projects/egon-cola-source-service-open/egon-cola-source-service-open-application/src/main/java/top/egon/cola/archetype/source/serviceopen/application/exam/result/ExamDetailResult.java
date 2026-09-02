package top.egon.cola.archetype.source.serviceopen.application.exam.result;

import java.time.Instant;

public record ExamDetailResult(
        Long id,
        Long courseId,
        String title,
        Instant startsAt,
        Instant endsAt,
        String status) {
}
