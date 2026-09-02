package top.egon.cola.archetype.source.serviceopen.infrastructure.course.service.impl;

import top.egon.cola.archetype.source.serviceopen.domain.common.Page;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.Course;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.serviceopen.domain.course.enums.CourseScheduleStatus;
import top.egon.cola.archetype.source.serviceopen.domain.course.service.CourseDomainService;
import top.egon.cola.archetype.source.serviceopen.domain.course.validators.CourseDomainValidator;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.converter.CourseConverter;
import top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.converter.CourseScheduleConverter;
import top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.dao.CourseDAO;
import top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.dao.CourseScheduleDAO;
import top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.po.CoursePO;
import top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.po.CourseSchedulePO;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service("courseDomainService")
@RequiredArgsConstructor
public class CourseDomainServiceImpl
        extends EgonColaServiceImpl<CourseDAO, CoursePO>
        implements CourseDomainService<CoursePO> {

    @Qualifier("courseDAO")
    private final CourseDAO courseDAO;
    @Qualifier("courseScheduleDAO")
    private final CourseScheduleDAO courseScheduleDAO;
    @Qualifier("courseConverterImpl")
    private final CourseConverter courseConverter;
    @Qualifier("courseScheduleConverterImpl")
    private final CourseScheduleConverter courseScheduleConverter;
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

    private final CourseDomainValidator validator = new CourseDomainValidator();

    @Override
    public Course createCourse(CourseCode code, String name, int credit) {
        return Course.create(idGenerator.nextLongId(), code, name, credit);
    }

    @Override
    public Course save(Course course) {
        CoursePO po = courseConverter.toTarget(course);
        if (po.getId() == null) {
            po.setId(course.getId());
        }
        super.save(po);
        return courseConverter.toSource(po);
    }

    @Override
    public Optional<Course> findById(CourseId courseId) {
        return Optional.ofNullable(super.getById(courseId.value())).map(courseConverter::toSource);
    }

    @Override
    public Optional<Course> findByCode(CourseCode courseCode) {
        return Optional.ofNullable(courseDAO.selectByCode(courseCode.value()))
                .map(courseConverter::toSource);
    }

    @Override
    public Page<Course> findPage(int currentPage, int pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<CoursePO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        Math.max(currentPage, 1), pageSize);
        page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("create_time"));
        page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.asc("id"));
        super.page(page);
        return Page.of(
                page.getRecords().stream().map(courseConverter::toSource).toList(),
                currentPage, (int) page.getPages(), pageSize, page.getTotal());
    }

    @Override
    public boolean existsByCode(CourseCode courseCode) {
        return courseDAO.selectByCode(courseCode.value()) != null;
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
                idGenerator.nextLongId(), new CourseId(course.getId()), classId,
                startsAt, endsAt, CourseScheduleStatus.SCHEDULED);
    }

    @Override
    public CourseSchedule saveSchedule(CourseSchedule schedule) {
        CourseSchedulePO po = courseScheduleConverter.toTarget(schedule);
        if (po.getId() == null) {
            po.setId(schedule.getId());
        }
        insertCompanion(courseScheduleDAO, po);
        return courseScheduleConverter.toSource(po);
    }

    @Override
    public List<CourseSchedule> findOverlapping(
            CourseId courseId, Long classId, Instant startsAt, Instant endsAt) {
        return courseScheduleDAO.selectOverlapping(
                        courseId.value(), classId, startsAt, endsAt).stream()
                .map(courseScheduleConverter::toSource)
                .toList();
    }

    private <M extends top.egon.cola.component.common.mybatis.model.EgonModel<M>> void insertCompanion(
            top.egon.cola.component.common.mybatis.extension.EgonColaMapper<M> dao, M model) {
        if (tenantIdProvider.currentTenantId() == null) {
            throw new IllegalStateException("TENANT_CONTEXT_MISSING");
        }
        modelValidationUtils.validateBusiness(
                model,
                top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups.Operation.INSERT);
        dao.insert(model);
    }
}
