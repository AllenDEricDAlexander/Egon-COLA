package top.egon.cola.archetype.source.lightopen.domain.teaching.aggregates;

import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;

import java.util.Objects;

public record CourseAggregate(Course course) {
    public CourseAggregate {
        Objects.requireNonNull(course, "course must not be null");
    }
}
