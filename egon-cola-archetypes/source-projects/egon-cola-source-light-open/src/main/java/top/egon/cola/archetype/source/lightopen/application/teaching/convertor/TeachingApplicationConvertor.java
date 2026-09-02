package top.egon.cola.archetype.source.lightopen.application.teaching.convertor;

import top.egon.cola.archetype.source.lightopen.application.teaching.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.CourseResult;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.SchoolClassResult;
import top.egon.cola.archetype.source.lightopen.domain.teaching.aggregates.SchoolClassAggregate;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSchedule;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSnapshot;
import org.springframework.stereotype.Component;

@Component
public class TeachingApplicationConvertor {
    public CourseResult toResult(Course course) {
        return new CourseResult(course.id(), course.code().value(), course.name(), course.status().name());
    }

    public CourseResult toResult(CourseSnapshot course) {
        return new CourseResult(course.id(), course.code().value(), course.name(), course.status().name());
    }

    public CourseSnapshot toSnapshot(Course course) {
        return CourseSnapshot.from(course);
    }

    public SchoolClassResult toResult(SchoolClass schoolClass) {
        return new SchoolClassResult(
                schoolClass.id().value(), schoolClass.name(), schoolClass.semester().value(),
                schoolClass.status().name(), 0);
    }

    public SchoolClassResult toResult(SchoolClassAggregate aggregate) {
        SchoolClass schoolClass = aggregate.schoolClass();
        return new SchoolClassResult(
                schoolClass.id().value(), schoolClass.name(), schoolClass.semester().value(),
                schoolClass.status().name(), aggregate.schedules().size());
    }

    public CourseSchedule toSchedule(ScheduleCourseCommand command, Course course) {
        return new CourseSchedule(course.code(), command.startsAt(), command.endsAt());
    }
}
