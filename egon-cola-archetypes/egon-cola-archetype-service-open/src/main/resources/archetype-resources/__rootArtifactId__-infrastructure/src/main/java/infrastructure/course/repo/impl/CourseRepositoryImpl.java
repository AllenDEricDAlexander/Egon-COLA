#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo.impl;

import ${package}.domain.common.Page;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.repos.CourseRepository;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.course.vos.CourseId;
import ${package}.infrastructure.course.repo.converter.CourseConverter;
import ${package}.infrastructure.course.repo.mapper.CourseMapper;
import ${package}.infrastructure.course.repo.po.CoursePo;
import ${package}.infrastructure.validators.EvaluationPersistenceValidator;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository("courseRepositoryImpl")
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {

    private final CourseMapper mapper;
    private final CourseConverter courseConverter;
    private final EvaluationPersistenceValidator persistenceValidator;

    @Override
    public Course save(Course course) {
        LocalDateTime now = LocalDateTime.now();
        CoursePo existing = mapper.selectById(course.getId());
        LocalDateTime createdAt = existing == null ? now : existing.getCreatedAt();
        CoursePo coursePo = courseConverter.toPo(course, createdAt, now);
        try {
            int affected = existing == null ? mapper.insert(coursePo) : mapper.updateById(coursePo);
            requireAffected(affected, "save course");
            return courseConverter.toDomain(coursePo);
        } catch (DataIntegrityViolationException exception) {
            throw persistenceValidator.translate("save course", exception);
        }
    }

    @Override
    public Optional<Course> findById(CourseId courseId) {
        return Optional.ofNullable(mapper.selectById(courseId.value()))
                .map(courseConverter::toDomain);
    }

    @Override
    public Optional<Course> findByCode(CourseCode courseCode) {
        return Optional.ofNullable(mapper.selectByCode(courseCode.value()))
                .map(courseConverter::toDomain);
    }

    @Override
    public Page<Course> findPage(int currentPage, int pageSize) {
        long offset = (long) (Math.max(currentPage, 1) - 1) * pageSize;
        var records = mapper.selectPage(offset, pageSize).stream()
                .map(courseConverter::toDomain)
                .toList();
        long totalCount = mapper.countAll();
        int totalPages = pageSize <= 0 ? 0 : (int) ((totalCount + pageSize - 1) / pageSize);
        return Page.of(records, currentPage, totalPages, pageSize, totalCount);
    }

    @Override
    public boolean existsByCode(CourseCode courseCode) {
        return mapper.selectByCode(courseCode.value()) != null;
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " affected " + affected + " rows");
        }
    }
}
