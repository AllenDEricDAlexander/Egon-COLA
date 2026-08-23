package ${package}.infrastructure.teaching.repo;

import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.domain.teaching.repos.SchoolClassRepository;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.teaching.vos.Semester;
import ${package}.infrastructure.teaching.repo.converter.CoursePOConverter;
import ${package}.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import ${package}.infrastructure.teaching.repo.impl.CourseRepositoryImpl;
import ${package}.infrastructure.teaching.repo.impl.SchoolClassRepositoryImpl;
import ${package}.infrastructure.teaching.repo.mapper.ClassCourseScheduleMapper;
import ${package}.infrastructure.teaching.repo.mapper.CourseMapper;
import ${package}.infrastructure.teaching.repo.mapper.SchoolClassMapper;
import ${package}.infrastructure.teaching.repo.po.ClassCourseSchedulePO;
import ${package}.infrastructure.teaching.repo.po.CoursePO;
import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolClassRepositoryImplTest {
    private static final long SCHOOL_CLASS_ID = 1003L;
    private static final long COURSE_ID = 1002L;
    private static final long SCHEDULE_ID = 1005L;

    @Mock CourseMapper courseMapper;
    @Mock SchoolClassMapper schoolClassMapper;
    @Mock ClassCourseScheduleMapper scheduleMapper;
    @Mock CoursePOConverter courseConverter;
    @Mock SchoolClassPOConverter schoolClassConverter;
    @Mock LongIdGenerator idGenerator;

    @InjectMocks CourseRepositoryImpl courseRepositoryImpl;
    @InjectMocks SchoolClassRepositoryImpl schoolClassRepositoryImpl;

    @Test
    void reconstructs_scheduled_class_aggregate_from_mapper_rows() {
        Course course = course();
        SchoolClass schoolClass = schoolClass();
        CoursePO coursePO = coursePO();
        SchoolClassPO schoolClassPO = schoolClassPO();
        ClassCourseSchedulePO schedulePO = schedulePO();
        when(schoolClassMapper.selectById(SCHOOL_CLASS_ID)).thenReturn(schoolClassPO);
        when(schoolClassConverter.toDomain(schoolClassPO)).thenReturn(schoolClass);
        when(scheduleMapper.findBySchoolClassIdOrderByStartsAt(SCHOOL_CLASS_ID))
                .thenReturn(List.of(schedulePO));
        when(courseMapper.selectById(COURSE_ID)).thenReturn(coursePO);
        when(courseConverter.toDomain(coursePO)).thenReturn(course);

        SchoolClassRepository repository = schoolClassRepositoryImpl;

        SchoolClassAggregate restored = repository
                .findAggregateById(new SchoolClassId(SCHOOL_CLASS_ID))
                .orElseThrow();

        assertEquals(1, restored.schedules().size());
        assertEquals(new CourseCode("math"), restored.schedules().getFirst().courseCode());
        verify(scheduleMapper).findBySchoolClassIdOrderByStartsAt(SCHOOL_CLASS_ID);
    }

    @Test
    void saves_class_and_schedule_with_explicit_long_id() {
        Course course = course();
        SchoolClass schoolClass = schoolClass();
        when(schoolClassMapper.selectById(SCHOOL_CLASS_ID)).thenReturn(null);
        when(schoolClassConverter.toPO(schoolClass)).thenReturn(schoolClassPO());
        when(schoolClassMapper.insert(any(SchoolClassPO.class))).thenReturn(1);
        when(schoolClassConverter.toDomain(any(SchoolClassPO.class))).thenReturn(schoolClass);
        when(courseMapper.findByCourseCode("math")).thenReturn(coursePO());
        when(courseConverter.toDomain(any(CoursePO.class))).thenReturn(course);
        when(idGenerator.nextLongId()).thenReturn(SCHEDULE_ID);
        when(scheduleMapper.insertSchedule(any(ClassCourseSchedulePO.class))).thenReturn(1);

        SchoolClassAggregate aggregate = new SchoolClassAggregate(schoolClass);
        aggregate.schedule(course, schedule());

        schoolClassRepositoryImpl.saveAggregate(aggregate);

        verify(scheduleMapper).insertSchedule(any(ClassCourseSchedulePO.class));
    }

    @Test
    void propagates_duplicate_schedule_constraint() {
        Course course = course();
        SchoolClass schoolClass = schoolClass();
        when(schoolClassMapper.selectById(SCHOOL_CLASS_ID)).thenReturn(null);
        when(schoolClassConverter.toPO(schoolClass)).thenReturn(schoolClassPO());
        when(schoolClassMapper.insert(any(SchoolClassPO.class))).thenReturn(1);
        when(schoolClassConverter.toDomain(any(SchoolClassPO.class))).thenReturn(schoolClass);
        when(courseMapper.findByCourseCode("math")).thenReturn(coursePO());
        when(courseConverter.toDomain(any(CoursePO.class))).thenReturn(course);
        when(idGenerator.nextLongId()).thenReturn(SCHEDULE_ID);
        when(scheduleMapper.insertSchedule(any(ClassCourseSchedulePO.class)))
                .thenThrow(new DuplicateKeyException("uk_class_course_start_0"));

        SchoolClassAggregate aggregate = new SchoolClassAggregate(schoolClass);
        aggregate.schedule(course, schedule());

        assertThrows(DuplicateKeyException.class,
                () -> schoolClassRepositoryImpl.saveAggregate(aggregate));
    }

    @Test
    void finds_course_by_business_code_through_mapper() {
        Course course = course();
        CoursePO coursePO = coursePO();
        when(courseMapper.findByCourseCode("math")).thenReturn(coursePO);
        when(courseConverter.toDomain(coursePO)).thenReturn(course);

        assertThat(courseRepositoryImpl.findByCode(new CourseCode("math")))
                .contains(course);
    }

    private SchoolClass schoolClass() {
        return new SchoolClass(
                new SchoolClassId(SCHOOL_CLASS_ID), "Class One", new Semester("2026-FALL"),
                SchoolClassStatus.ACTIVE);
    }

    private Course course() {
        return new Course(COURSE_ID, new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE);
    }

    private CoursePO coursePO() {
        return new CoursePO(COURSE_ID, "math", "Mathematics", "ACTIVE", Instant.now());
    }

    private SchoolClassPO schoolClassPO() {
        return new SchoolClassPO(
                SCHOOL_CLASS_ID, "Class One", "2026-FALL", "ACTIVE", Instant.now());
    }

    private ClassCourseSchedulePO schedulePO() {
        return new ClassCourseSchedulePO(
                SCHEDULE_ID, SCHOOL_CLASS_ID, COURSE_ID,
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 1, 10, 0), Instant.now());
    }

    private CourseSchedule schedule() {
        return new CourseSchedule(
                new CourseCode("math"),
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 1, 10, 0));
    }
}
