package top.egon.cola.archetype.source.light.application.teaching.command;

public record CreateSchoolClassCommand(
        String name,
        String semester,
        String operatorId,
        String idempotencyKey) {
}
