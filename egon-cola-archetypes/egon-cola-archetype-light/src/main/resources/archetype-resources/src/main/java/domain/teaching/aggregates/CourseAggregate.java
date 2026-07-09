package ${package}.domain.teaching.aggregates;

import ${package}.domain.teaching.entities.Course;

import java.util.Objects;

public record CourseAggregate(Course course) {
    public CourseAggregate {
        Objects.requireNonNull(course, "course must not be null");
    }
}
