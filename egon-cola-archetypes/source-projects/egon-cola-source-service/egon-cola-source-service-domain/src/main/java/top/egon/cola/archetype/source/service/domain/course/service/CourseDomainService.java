package top.egon.cola.archetype.source.service.domain.course.service;

import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import top.egon.cola.archetype.source.service.domain.common.Page;
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
