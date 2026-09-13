package top.egon.cola.archetype.source.lightopen.infrastructure.teaching.service.impl;

import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.lightopen.domain.teaching.service.CourseDomainService;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.converter.CoursePOConverter;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.CourseRepository;
import top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.po.CoursePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.util.Optional;

/** Business rules and orchestration for the course domain service. */
@Slf4j
@Validated
@Service("courseDomainService")
@RequiredArgsConstructor
public class CourseDomainServiceImpl
        implements CourseDomainService {

    @Qualifier("courseRepository")
    private final CourseRepository courseRepository;
    @Qualifier("coursePOConverterImpl")
    private final CoursePOConverter converter;
    @Qualifier("snowflakeIdGenerator")
    private final LongIdGenerator idGenerator;

    @Override
    public Course createCourse(CourseCode code, String name) {
        return new Course(idGenerator.nextLongId(), code, name, CourseStatus.ACTIVE);
    }

    @Override
    @Transactional
    public Course save(Course course) {
        CoursePO po = converter.toTarget(course);
        CoursePO current = po.getId() == null ? null : courseRepository.getById(po.getId());
        if (current != null) { converter.updateMetadata(po, current); }
        boolean written = current == null ? courseRepository.save(po) : courseRepository.updateById(po);
        if (!written) { throw new org.springframework.dao.OptimisticLockingFailureException("VERSIONED_WRITE_CONFLICT"); }

        return converter.toSource(po);
    }

    @Override
    public Optional<Course> findById(Long courseId) {
        return Optional.ofNullable(courseRepository.getById(courseId)).map(converter::toSource);
    }

    @Override
    public Optional<Course> findByCode(CourseCode courseCode) {
        return courseRepository.selectByCourseCode(courseCode.value()).stream()
                .findFirst().map(converter::toSource);
    }

}
