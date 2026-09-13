package top.egon.cola.archetype.source.service.domain.course.service;

import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import top.egon.cola.archetype.source.service.domain.common.Page;

public interface CourseDomainService {

    Course createCourse( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull CourseCode code, String name, int credit);

    Course save( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Course course);

    Optional<Course> findById( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull CourseId courseId);

    Optional<Course> findByCode( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull CourseCode courseCode);

    Page<Course> findPage(int currentPage, int pageSize);

    boolean existsByCode( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull CourseCode courseCode);

    CourseSchedule scheduleCourse( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Course course,
            Long classId,
            Instant startsAt,
            Instant endsAt,
            List<CourseSchedule> overlaps);

    CourseSchedule saveSchedule( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull CourseSchedule schedule);

    List<CourseSchedule> findOverlapping( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull CourseId courseId, Long classId, Instant startsAt, Instant endsAt);
}
