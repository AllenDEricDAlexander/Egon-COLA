package top.egon.cola.archetype.source.light.application.teaching.pojo.command;

public record CreateSchoolClassCommand(
        String name,
        String semester,
        String operatorId,
        String idempotencyKey) {
}
