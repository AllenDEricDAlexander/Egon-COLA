#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course.service.impl;

import ${package}.domain.course.entities.Course;
import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.enums.CourseScheduleStatus;
import ${package}.domain.course.service.CourseDomainService;
import ${package}.domain.course.validators.CourseDomainValidator;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.course.vos.CourseId;
import java.time.Instant;
import java.util.List;

public final class CourseDomainServiceImpl implements CourseDomainService {

    private final CourseDomainValidator validator = new CourseDomainValidator();

    @Override
    public Course createCourse(String id, CourseCode code, String name, int credit) {
        return Course.create(id, code, name, credit);
    }

    @Override
    public CourseSchedule scheduleCourse(
            String id,
            Course course,
            String classId,
            Instant startsAt,
            Instant endsAt,
            List<CourseSchedule> overlaps) {
        validator.validateSchedule(course, classId, startsAt, endsAt, overlaps);
        return new CourseSchedule(
                id,
                new CourseId(course.getId()),
                classId,
                startsAt,
                endsAt,
                CourseScheduleStatus.SCHEDULED);
    }
}
