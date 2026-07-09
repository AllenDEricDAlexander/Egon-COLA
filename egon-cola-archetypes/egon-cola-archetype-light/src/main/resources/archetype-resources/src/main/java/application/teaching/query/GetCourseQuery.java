package ${package}.application.teaching.query;

public record GetCourseQuery(String courseId) {
    public GetCourseQuery {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("courseId must not be blank");
        }
    }
}
