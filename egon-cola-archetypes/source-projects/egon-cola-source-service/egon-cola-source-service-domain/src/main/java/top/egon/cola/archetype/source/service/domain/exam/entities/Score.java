package top.egon.cola.archetype.source.service.domain.exam.entities;

import top.egon.cola.archetype.source.service.domain.exam.enums.ScoreStatus;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.service.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.service.domain.exam.vos.ScoreValue;

public final class Score {

    private final Long id;
    private final ExamId examId;
    private final CourseId courseId;
    private final Long studentId;
    private final ScoreValue points;
    private ScoreStatus status;

    public Score(
            Long id,
            ExamId examId,
            CourseId courseId,
            Long studentId,
            ScoreValue points,
            ScoreStatus status) {
        this.id = id;
        this.examId = examId;
        this.courseId = courseId;
        this.studentId = studentId;
        this.points = points;
        this.status = status;
    }

    public void cancel() { status = ScoreStatus.CANCELLED; }
    public Long getId() { return id; }
    public ExamId getExamId() { return examId; }
    public CourseId getCourseId() { return courseId; }
    public Long getStudentId() { return studentId; }
    public ScoreValue getPoints() { return points; }
    public ScoreStatus getStatus() { return status; }
}
