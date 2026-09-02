package top.egon.cola.archetype.source.service.application.exam.command;

public record RecordScoreCommand(Long examId, Long studentId, int points) {
}
