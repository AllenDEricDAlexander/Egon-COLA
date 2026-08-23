#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo;

import ${package}.domain.common.Page;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.enums.CourseStatus;
import ${package}.domain.course.vos.CourseCode;
import ${package}.infrastructure.course.repo.converter.CourseConverter;
import ${package}.infrastructure.course.repo.converter.CourseDomainMapper;
import ${package}.infrastructure.course.repo.converter.CoursePoMapper;
import ${package}.infrastructure.course.repo.impl.CourseRepositoryImpl;
import ${package}.infrastructure.course.repo.mapper.CourseMapper;
import ${package}.infrastructure.course.repo.po.CoursePo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseRepositoryTest {

    @Test
    void shouldInsertCourseThroughMapperAndRoundTripDomain() {
        CoursePoMapper poMapper = mock(CoursePoMapper.class);
        CourseDomainMapper domainMapper = mock(CourseDomainMapper.class);
        CourseMapper mapper = mock(CourseMapper.class);
        Course course = Course.create(1001L, new CourseCode("MATH-101"), "Math", 3);
        CoursePo mappedPo = new CoursePo(
                1001L, "MATH-101", "Math", 3, "ACTIVE",
                LocalDateTime.MIN, LocalDateTime.MAX);
        when(poMapper.convert(course)).thenReturn(mappedPo);
        when(domainMapper.convert(any(CoursePo.class))).thenReturn(course);
        when(mapper.selectById(1001L)).thenReturn(null);
        when(mapper.insert(any(CoursePo.class))).thenReturn(1);
        CourseRepositoryImpl repository = new CourseRepositoryImpl(
                mapper,
                new CourseConverter(poMapper, domainMapper),
                new EvaluationPersistenceValidator());

        Course result = repository.save(course);

        assertThat(result.getCode()).isEqualTo(new CourseCode("MATH-101"));
        verify(mapper).insert(any(CoursePo.class));
    }

    @Test
    void shouldPageCoursesWithStableMetadataFromMapper() {
        CourseMapper mapper = mock(CourseMapper.class);
        CourseDomainMapper domainMapper = mock(CourseDomainMapper.class);
        CoursePoMapper poMapper = mock(CoursePoMapper.class);
        CoursePo row = new CoursePo(
                1001L, "MATH-101", "Math", 3, "ACTIVE",
                LocalDateTime.MIN, LocalDateTime.MAX);
        when(mapper.selectPage(0L, 2)).thenReturn(List.of(row));
        when(mapper.countAll()).thenReturn(3L);
        when(domainMapper.convert(row)).thenReturn(Course.create(
                1001L, new CourseCode("MATH-101"), "Math", 3));
        CourseRepositoryImpl repository = new CourseRepositoryImpl(
                mapper,
                new CourseConverter(poMapper, domainMapper),
                new EvaluationPersistenceValidator());

        Page<Course> page = repository.findPage(1, 2);

        assertThat(page.records()).hasSize(1);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.pageSize()).isEqualTo(2);
        assertThat(page.totalCount()).isEqualTo(3L);
        verify(mapper).selectPage(0L, 2);
    }

    @Test
    void shouldKeepCourseStatusWhenConvertingPersistenceRow() {
        CoursePoMapper poMapper = mock(CoursePoMapper.class);
        CourseDomainMapper domainMapper = mock(CourseDomainMapper.class);
        CoursePo row = new CoursePo(
                1001L, "MATH-101", "Math", 3, "ACTIVE",
                LocalDateTime.MIN, LocalDateTime.MAX);
        when(domainMapper.convert(row)).thenReturn(Course.create(
                1001L, new CourseCode("MATH-101"), "Math", 3));

        Course result = new CourseConverter(poMapper, domainMapper).toDomain(row);

        assertThat(result.getStatus()).isEqualTo(CourseStatus.ACTIVE);
    }
}
