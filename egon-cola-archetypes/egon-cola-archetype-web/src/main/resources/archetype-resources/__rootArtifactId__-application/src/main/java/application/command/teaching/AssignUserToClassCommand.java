package ${package}.application.command.teaching;

public record AssignUserToClassCommand(String requestId, String userId, String schoolClassId) {
}
