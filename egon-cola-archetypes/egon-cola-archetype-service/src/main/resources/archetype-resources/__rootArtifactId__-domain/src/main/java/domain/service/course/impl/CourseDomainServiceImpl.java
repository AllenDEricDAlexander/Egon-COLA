#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.service.course.impl;

import ${package}.domain.entities.course.Course;
import ${package}.domain.entities.course.CourseSchedule;
import ${package}.domain.enums.course.CourseScheduleStatus;
import ${package}.domain.service.course.CourseDomainService;
import ${package}.domain.validators.course.CourseDomainValidator;
import ${package}.domain.vos.course.CourseCode;
import ${package}.domain.vos.course.CourseId;
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
