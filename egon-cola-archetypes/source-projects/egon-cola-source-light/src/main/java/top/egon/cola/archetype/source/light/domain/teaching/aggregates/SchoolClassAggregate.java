package top.egon.cola.archetype.source.light.domain.teaching.aggregates;

import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.light.domain.teaching.validators.TeachingDomainValidator;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SchoolClassAggregate {
    private final SchoolClass schoolClass;
    private final List<CourseSchedule> schedules = new ArrayList<>();

    public SchoolClassAggregate(SchoolClass schoolClass) {
        this.schoolClass = Objects.requireNonNull(schoolClass);
    }

    public SchoolClass schoolClass() {
        return schoolClass;
    }

    public List<CourseSchedule> schedules() {
        return List.copyOf(schedules);
    }

    public void schedule(Course course, CourseSchedule schedule) {
        TeachingDomainValidator.requireSchedulable(schoolClass, course, schedule);
        TeachingDomainValidator.requireNoOverlap(schedules, schedule);
        schedules.add(schedule);
    }
}
