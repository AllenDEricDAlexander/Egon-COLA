package top.egon.fable.infrastructure.repo.course.impl;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import top.egon.fable.common.constants.ErrorCodes;
import top.egon.fable.common.exception.BizException;
import top.egon.fable.domain.common.Page;
import top.egon.fable.domain.entities.course.Course;
import top.egon.fable.domain.repos.course.CourseRepository;
import top.egon.fable.infrastructure.repo.course.converter.CourseConverter;
import top.egon.fable.infrastructure.repo.course.jpa.CourseJpaRepository;
import top.egon.fable.infrastructure.repo.course.po.CoursePo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository("courseRepositoryImpl")
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {

    @Qualifier("courseJpaRepository")
    private final CourseJpaRepository courseJpaRepository;

    @Qualifier("courseConverter")
    private final CourseConverter courseConverter;

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
            if (isCourseNameUniqueConstraintViolation(exception)) {
                throw new BizException(ErrorCodes.COURSE_NAME_DUPLICATED, "course name already exists");
            }
            throw exception;
        }
    }

    @Override
    public Optional<Course> findById(String courseId) {
        return courseJpaRepository.findById(courseId).map(courseConverter::toDomain);
    }

    @Override
    public Page<Course> findPage(int currentPage, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(currentPage, 1) - 1, pageSize);
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
    public boolean existsByName(String name) {
        return courseJpaRepository.existsByName(name);
    }

    private boolean isCourseNameUniqueConstraintViolation(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("uk_course_name")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
