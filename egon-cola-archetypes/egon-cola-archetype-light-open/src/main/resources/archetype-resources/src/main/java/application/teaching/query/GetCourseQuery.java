package ${package}.application.teaching.query;

public record GetCourseQuery(long courseId) {
    public GetCourseQuery {
        if (courseId <= 0) {
            throw new IllegalArgumentException("courseId must be positive");
        }
    }
}
