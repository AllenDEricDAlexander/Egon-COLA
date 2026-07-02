package top.egon.fable.domain.entities.examing;

import top.egon.fable.common.constants.ErrorCodes;
import top.egon.fable.common.exception.BizException;
import top.egon.fable.domain.enums.ExamResultStatus;

public class ExamResult {

    private String id;

    private String courseId;

    private String studentId;

    private int score;

    private ExamResultStatus status;

    public static ExamResult record(String id, String courseId, String studentId, int score) {
        validate(courseId, studentId, score);
        ExamResult examResult = new ExamResult();
        examResult.setId(id);
        examResult.setCourseId(courseId);
        examResult.setStudentId(studentId);
        examResult.setScore(score);
        examResult.setStatus(ExamResultStatus.RECORDED);
        return examResult;
    }

    public static void validate(String courseId, String studentId, int score) {
        if (isBlank(courseId)) {
            throw new BizException("course id must not be blank");
        }
        if (isBlank(studentId)) {
            throw new BizException("student id must not be blank");
        }
        if (score < 0 || score > 100) {
            throw new BizException(ErrorCodes.INVALID_EXAM_SCORE, "invalid exam result");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public ExamResultStatus getStatus() {
        return status;
    }

    public void setStatus(ExamResultStatus status) {
        this.status = status;
    }
}
