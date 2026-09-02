package top.egon.cola.archetype.source.web.application.teaching.command;

public record AssignUserToClassCommand(
        String requestId,
        Long gradeId,
        Long schoolClassId,
        Long userId) {
}
