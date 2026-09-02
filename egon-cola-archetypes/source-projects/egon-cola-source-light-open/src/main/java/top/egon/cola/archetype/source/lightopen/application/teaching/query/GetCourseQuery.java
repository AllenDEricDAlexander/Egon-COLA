package top.egon.cola.archetype.source.lightopen.application.teaching.query;

public record GetCourseQuery(Long courseId) {
    public GetCourseQuery {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("courseId must be positive");
        }
    }

}
