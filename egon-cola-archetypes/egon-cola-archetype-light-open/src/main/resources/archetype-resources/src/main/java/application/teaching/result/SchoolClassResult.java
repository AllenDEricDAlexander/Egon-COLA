package ${package}.application.teaching.result;

public record SchoolClassResult(
        Long id,
        String name,
        String semester,
        String status,
        int scheduleCount) {
}
