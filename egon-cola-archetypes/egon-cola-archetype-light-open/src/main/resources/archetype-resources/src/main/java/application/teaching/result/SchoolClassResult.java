package ${package}.application.teaching.result;

public record SchoolClassResult(
        long id,
        String name,
        String semester,
        String status,
        int scheduleCount) {
}
