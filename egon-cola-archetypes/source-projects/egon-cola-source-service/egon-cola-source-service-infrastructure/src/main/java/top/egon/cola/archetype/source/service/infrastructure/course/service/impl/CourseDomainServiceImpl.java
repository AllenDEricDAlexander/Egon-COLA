package top.egon.cola.archetype.source.service.infrastructure.course.service.impl;

import top.egon.cola.archetype.source.service.domain.common.Page;
import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.service.domain.course.enums.CourseScheduleStatus;
import top.egon.cola.archetype.source.service.domain.course.service.CourseDomainService;
import top.egon.cola.archetype.source.service.domain.course.validators.CourseDomainValidator;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.service.infrastructure.course.repo.converter.CourseConverter;
import top.egon.cola.archetype.source.service.infrastructure.course.repo.converter.CourseScheduleConverter;
import top.egon.cola.archetype.source.service.infrastructure.course.repo.CourseRepository;
import top.egon.cola.archetype.source.service.infrastructure.course.repo.CourseScheduleRepository;
import top.egon.cola.archetype.source.service.infrastructure.course.repo.po.CoursePO;
import top.egon.cola.archetype.source.service.infrastructure.course.repo.po.CourseSchedulePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Validated
@Service("courseDomainService")
@RequiredArgsConstructor
public class CourseDomainServiceImpl
        implements CourseDomainService {

    @Qualifier("courseRepository")
    private final CourseRepository courseRepository;
    @Qualifier("courseScheduleRepository")
    private final CourseScheduleRepository courseScheduleRepository;
    @Qualifier("courseConverterImpl")
    private final CourseConverter courseConverter;
    @Qualifier("courseScheduleConverterImpl")
    private final CourseScheduleConverter courseScheduleConverter;

    private final CourseDomainValidator validator = new CourseDomainValidator();

    @Override
    public Course createCourse(CourseCode code, String name, int credit) {
        return Course.create(SnowflakeIdGenerator.nextLongId(), code, name, credit);
    }

    @Override
    @Transactional
    public Course save(Course course) {
        CoursePO po = courseConverter.toTarget(course);
        CoursePO current = po.getId() == null ? null : courseRepository.getById(po.getId());
        if (current != null) { courseConverter.updateMetadata(po, current); }
        boolean written = current == null ? courseRepository.save(po) : courseRepository.updateById(po);
        if (!written) { throw new org.springframework.dao.OptimisticLockingFailureException("VERSIONED_WRITE_CONFLICT"); }

        return courseConverter.toSource(po);
    }

    @Override
    public Optional<Course> findById(CourseId courseId) {
        return Optional.ofNullable(courseRepository.getById(courseId.value())).map(courseConverter::toSource);
    }

    @Override
    public Optional<Course> findByCode(CourseCode courseCode) {
        return Optional.ofNullable(courseRepository.selectByCode(courseCode.value()))
                .map(courseConverter::toSource);
    }

    @Override
    public Page<Course> findPage(int currentPage, int pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<CoursePO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        Math.max(currentPage, 1), pageSize);
        courseRepository.selectActivePage(page);
        return Page.of(
                page.getRecords().stream().map(courseConverter::toSource).toList(),
                currentPage, (int) page.getPages(), pageSize, page.getTotal());
    }

    @Override
    public boolean existsByCode(CourseCode courseCode) {
        return courseRepository.selectByCode(courseCode.value()) != null;
    }

    @Override
    public CourseSchedule scheduleCourse(
            Course course,
            Long classId,
            Instant startsAt,
            Instant endsAt,
            List<CourseSchedule> overlaps) {
        validator.validateSchedule(course, classId, startsAt, endsAt, overlaps);
        return new CourseSchedule(
                SnowflakeIdGenerator.nextLongId(), new CourseId(course.getId()), classId,
                startsAt, endsAt, CourseScheduleStatus.SCHEDULED);
    }

    @Override
    @Transactional
    public CourseSchedule saveSchedule(CourseSchedule schedule) {
        CourseSchedulePO po = courseScheduleConverter.toTarget(schedule);
        CourseSchedulePO current = po.getId() == null ? null : courseScheduleRepository.getById(po.getId());
        if (current != null) { courseScheduleConverter.updateMetadata(po, current); }
        boolean written = current == null ? courseScheduleRepository.save(po) : courseScheduleRepository.updateById(po);
        if (!written) { throw new org.springframework.dao.OptimisticLockingFailureException("VERSIONED_WRITE_CONFLICT"); }

        return courseScheduleConverter.toSource(po);
    }

    @Override
    public List<CourseSchedule> findOverlapping(
            CourseId courseId, Long classId, Instant startsAt, Instant endsAt) {
        return courseScheduleRepository.selectOverlapping(
                        courseId.value(), classId, startsAt, endsAt).stream()
                .map(courseScheduleConverter::toSource)
                .toList();
    }

}
