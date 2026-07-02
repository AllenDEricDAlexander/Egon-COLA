package top.egon.fable.domain.service.examing;

import top.egon.fable.common.constants.ErrorCodes;
import top.egon.fable.common.exception.NotFoundException;
import top.egon.fable.domain.entities.examing.ExamResult;
import top.egon.fable.domain.repos.course.CourseRepository;

public class ExamDomainService {

    private final CourseRepository courseRepository;

    public ExamDomainService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public void record(String courseId, String studentId, int score) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
        ExamResult.validate(courseId, studentId, score);
    }
}
