#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course.service;

import ${package}.domain.course.entities.Course;
import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.course.vos.CourseId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import ${package}.domain.common.Page;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

public interface CourseDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {

    Course createCourse(CourseCode code, String name, int credit);

    Course save(Course course);

    Optional<Course> findById(CourseId courseId);

    Optional<Course> findByCode(CourseCode courseCode);

    Page<Course> findPage(int currentPage, int pageSize);

    boolean existsByCode(CourseCode courseCode);

    CourseSchedule scheduleCourse(
            Course course,
            Long classId,
            Instant startsAt,
            Instant endsAt,
            List<CourseSchedule> overlaps);

    CourseSchedule saveSchedule(CourseSchedule schedule);

    List<CourseSchedule> findOverlapping(
            CourseId courseId, Long classId, Instant startsAt, Instant endsAt);
}
