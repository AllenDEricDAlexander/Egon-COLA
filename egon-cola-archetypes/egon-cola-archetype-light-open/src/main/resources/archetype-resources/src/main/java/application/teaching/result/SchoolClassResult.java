package ${package}.application.teaching.result;

public record SchoolClassResult(
        String id,
        String name,
        String semester,
        String status,
        int scheduleCount) {
}
