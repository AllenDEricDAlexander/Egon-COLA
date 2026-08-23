package ${package}.application.teaching.command;

public record AssignUserToClassCommand(
        String requestId,
        String gradeId,
        String schoolClassId,
        String userId) {
}
