#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.entities;

import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.vos.ExamId;
import java.time.Instant;

public final class Exam {

    private final ExamId id;
    private final CourseId courseId;
    private final String title;
    private final Instant startsAt;
    private final Instant endsAt;
    private ExamStatus status;

    public Exam(
            ExamId id,
            CourseId courseId,
            String title,
            Instant startsAt,
            Instant endsAt,
            ExamStatus status) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
    }

    public void publish() { status = ExamStatus.PUBLISHED; }
    public ExamId getId() { return id; }
    public CourseId getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public ExamStatus getStatus() { return status; }
}
