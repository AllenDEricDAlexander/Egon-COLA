package ${package}.application.teaching.command;

public record AssignUserToClassCommand(String requestId, String userId, String schoolClassId) {
}
