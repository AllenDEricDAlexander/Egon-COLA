package ${package}.application.teaching.convertor;

import ${package}.application.teaching.command.ScheduleCourseCommand;
import ${package}.application.teaching.result.CourseResult;
import ${package}.application.teaching.result.SchoolClassResult;
import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.CourseSnapshot;
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
