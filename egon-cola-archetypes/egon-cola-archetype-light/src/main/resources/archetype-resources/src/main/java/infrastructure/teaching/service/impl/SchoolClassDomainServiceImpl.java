package ${package}.infrastructure.teaching.service.impl;

import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.teaching.vos.Semester;
import ${package}.infrastructure.teaching.repo.converter.CoursePOConverter;
import ${package}.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import ${package}.infrastructure.teaching.repo.dao.ClassCourseScheduleDAO;
import ${package}.infrastructure.teaching.repo.dao.CourseDAO;
import ${package}.infrastructure.teaching.repo.dao.SchoolClassDAO;
import ${package}.infrastructure.teaching.repo.po.ClassCourseSchedulePO;
import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.util.Optional;

/** MyBatis-Plus implementation of the school-class domain service. */
@Slf4j
@Service("schoolClassDomainService")
@RequiredArgsConstructor
public class SchoolClassDomainServiceImpl
        extends EgonColaServiceImpl<SchoolClassDAO, SchoolClassPO>
        implements SchoolClassDomainService<SchoolClassPO> {

    @Qualifier("schoolClassDAO")
    private final SchoolClassDAO schoolClassDAO;
    @Qualifier("courseDAO")
    private final CourseDAO courseDAO;
    @Qualifier("classCourseScheduleDAO")
    private final ClassCourseScheduleDAO scheduleDAO;
    @Qualifier("schoolClassPOConverterImpl")
    private final SchoolClassPOConverter schoolClassConverter;
    @Qualifier("coursePOConverterImpl")
    private final CoursePOConverter courseConverter;
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
    public SchoolClass createSchoolClass(String name, Semester semester) {
        return new SchoolClass(new SchoolClassId(idGenerator.nextLongId()), name, semester,
                ${package}.domain.teaching.enums.SchoolClassStatus.ACTIVE);
    }

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        SchoolClassPO po = schoolClassConverter.toTarget(schoolClass);
        po.setId(schoolClass.id().value());
        schoolClassDAO.insert(po);
        return schoolClassConverter.toSource(po);
    }

    @Override
    public Optional<SchoolClassAggregate> findAggregateById(SchoolClassId schoolClassId) {
        SchoolClassPO po = schoolClassDAO.selectById(schoolClassId.value());
        if (po == null) {
            return Optional.empty();
        }
        SchoolClassAggregate aggregate = new SchoolClassAggregate(
                schoolClassConverter.toSource(po));
        scheduleDAO.selectBySchoolClassIdOrderByStartsAt(schoolClassId.value()).forEach(schedule -> {
            Course course = Optional.ofNullable(courseDAO.selectById(schedule.getCourseId()))
                    .map(courseConverter::toSource)
                    .orElseThrow(() -> new IllegalStateException("scheduled course not found"));
            aggregate.schedule(course, new CourseSchedule(
                    course.code(), schedule.getStartsAt(), schedule.getEndsAt()));
        });
        return Optional.of(aggregate);
    }

    @Override
    @Transactional
    public void saveAggregate(SchoolClassAggregate aggregate) {
        save(aggregate.schoolClass());
        aggregate.schedules().forEach(schedule -> {
            Course course = courseDAO.selectByCourseCode(schedule.courseCode().value()).stream()
                    .findFirst().map(courseConverter::toSource)
                    .orElseThrow(() -> new IllegalStateException("scheduled course not found"));
            scheduleDAO.insert(ClassCourseSchedulePO.builder()
                    .schoolClassId(aggregate.schoolClass().id().value())
                    .courseId(course.id())
                    .startsAt(schedule.startsAt())
                    .endsAt(schedule.endsAt())
                    .build());
        });
    }

    @Override
    public SchoolClassAggregate schedule(
            SchoolClassAggregate schoolClass, Course course, CourseSchedule schedule) {
        schoolClass.schedule(course, schedule);
        return schoolClass;
    }
}
