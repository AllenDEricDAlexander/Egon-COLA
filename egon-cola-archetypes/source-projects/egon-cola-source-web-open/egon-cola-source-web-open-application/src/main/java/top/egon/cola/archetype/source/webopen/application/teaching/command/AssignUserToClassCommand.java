package top.egon.cola.archetype.source.webopen.application.teaching.command;

public record AssignUserToClassCommand(
        String requestId,
        Long gradeId,
        Long schoolClassId,
        Long userId) {
}
