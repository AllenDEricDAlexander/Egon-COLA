package top.egon.cola.archetype.source.lightopen.application.teaching.command;

public record CreateCourseCommand(
        String code,
        String name,
        String operatorId,
        String idempotencyKey) {
}
