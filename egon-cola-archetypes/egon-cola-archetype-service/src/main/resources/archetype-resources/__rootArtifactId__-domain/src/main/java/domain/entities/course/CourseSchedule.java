#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.entities.course;

import ${package}.domain.enums.course.CourseScheduleStatus;
import ${package}.domain.vos.course.CourseId;
import java.time.Instant;

public final class CourseSchedule {

    private final String id;
    private final CourseId courseId;
    private final String classId;
    private final Instant startsAt;
    private final Instant endsAt;
    private final CourseScheduleStatus status;

    public CourseSchedule(
            String id,
            CourseId courseId,
            String classId,
            Instant startsAt,
            Instant endsAt,
            CourseScheduleStatus status) {
        this.id = id;
        this.courseId = courseId;
        this.classId = classId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
    }

    public boolean overlaps(Instant candidateStart, Instant candidateEnd) {
        return startsAt.isBefore(candidateEnd) && candidateStart.isBefore(endsAt);
    }

    public String getId() { return id; }
    public CourseId getCourseId() { return courseId; }
    public String getClassId() { return classId; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public CourseScheduleStatus getStatus() { return status; }
}
