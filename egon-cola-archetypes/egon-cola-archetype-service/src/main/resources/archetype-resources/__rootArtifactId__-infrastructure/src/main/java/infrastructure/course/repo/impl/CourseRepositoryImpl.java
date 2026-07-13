#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import ${package}.domain.common.Page;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.repos.CourseRepository;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.course.vos.CourseId;
import ${package}.infrastructure.course.repo.converter.CourseConverter;
import ${package}.infrastructure.course.repo.jpa.CourseJpaRepository;
import ${package}.infrastructure.course.repo.po.CoursePo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository("courseRepositoryImpl")
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {

    @Qualifier("courseJpaRepository")
    private final CourseJpaRepository courseJpaRepository;

    @Qualifier("courseConverter")
    private final CourseConverter courseConverter;

    private final EvaluationPersistenceValidator persistenceValidator;

    @Override
    public Course save(Course course) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = courseJpaRepository.findById(course.getId())
                .map(CoursePo::getCreatedAt)
                .orElse(now);
        CoursePo coursePo = courseConverter.toPo(course, createdAt, now);
        try {
            return courseConverter.toDomain(courseJpaRepository.saveAndFlush(coursePo));
        } catch (DataIntegrityViolationException exception) {
            throw persistenceValidator.translate("save course", exception);
        }
    }

    @Override
    public Optional<Course> findById(CourseId courseId) {
        return courseJpaRepository.findById(courseId.value()).map(courseConverter::toDomain);
    }

    @Override
    public Optional<Course> findByCode(CourseCode courseCode) {
        return courseJpaRepository.findByCode(courseCode.value()).map(courseConverter::toDomain);
    }

    @Override
    public Page<Course> findPage(int currentPage, int pageSize) {
        Pageable pageable = PageRequest.of(
                Math.max(currentPage, 1) - 1,
                pageSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        org.springframework.data.domain.Page<CoursePo> page = courseJpaRepository.findAll(pageable);
        return Page.of(
                page.getContent().stream()
                        .map(courseConverter::toDomain)
                        .toList(),
                currentPage,
                page.getTotalPages(),
                pageSize,
                page.getTotalElements());
    }

    @Override
    public boolean existsByCode(CourseCode courseCode) {
        return courseJpaRepository.existsByCode(courseCode.value());
    }

}
