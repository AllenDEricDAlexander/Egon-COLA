#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.entities.exam;

import ${package}.domain.enums.exam.ScoreStatus;
import ${package}.domain.vos.course.CourseId;
import ${package}.domain.vos.exam.ExamId;
import ${package}.domain.vos.exam.ScoreValue;

public final class Score {

    private final String id;
    private final ExamId examId;
    private final CourseId courseId;
    private final String studentId;
    private final ScoreValue points;
    private ScoreStatus status;

    public Score(
            String id,
            ExamId examId,
            CourseId courseId,
            String studentId,
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
    public String getId() { return id; }
    public ExamId getExamId() { return examId; }
    public CourseId getCourseId() { return courseId; }
    public String getStudentId() { return studentId; }
    public ScoreValue getPoints() { return points; }
    public ScoreStatus getStatus() { return status; }
}
