package top.egon.cola.archetype.source.serviceopen.application.exam.command;

import java.time.Instant;

public record CreateExamCommand(
        Long courseId, String title, Instant startsAt, Instant endsAt) {
}
