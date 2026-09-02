package top.egon.cola.archetype.source.light.infrastructure.teaching.service.impl;

import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.enums.CourseStatus;
import top.egon.cola.archetype.source.light.domain.teaching.service.CourseDomainService;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.converter.CoursePOConverter;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.dao.CourseDAO;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.po.CoursePO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.util.Optional;

/** MyBatis-Plus implementation of the course domain service. */
@Slf4j
@Service("courseDomainService")
@RequiredArgsConstructor
public class CourseDomainServiceImpl
        extends EgonColaServiceImpl<CourseDAO, CoursePO>
        implements CourseDomainService<CoursePO> {

    @Qualifier("courseDAO")
    private final CourseDAO courseDAO;
    @Qualifier("coursePOConverterImpl")
    private final CoursePOConverter converter;
    @Qualifier("snowflakeIdGenerator")
    private final LongIdGenerator idGenerator;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaModelValidationUtils")
    private final EgonColaModelValidationUtils modelValidationUtils;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaMdcTenantIdProvider")
    private final EgonColaTenantIdProvider tenantIdProvider;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public Course createCourse(CourseCode code, String name) {
        return new Course(idGenerator.nextLongId(), code, name, CourseStatus.ACTIVE);
    }

    @Override
    public Course save(Course course) {
        CoursePO po = converter.toTarget(course);
        po.setId(course.id());
        courseDAO.insert(po);
        return converter.toSource(po);
    }

    @Override
    public Optional<Course> findById(Long courseId) {
        return Optional.ofNullable(courseDAO.selectById(courseId)).map(converter::toSource);
    }

    @Override
    public Optional<Course> findByCode(CourseCode courseCode) {
        return courseDAO.selectByCourseCode(courseCode.value()).stream()
                .findFirst().map(converter::toSource);
    }
}
