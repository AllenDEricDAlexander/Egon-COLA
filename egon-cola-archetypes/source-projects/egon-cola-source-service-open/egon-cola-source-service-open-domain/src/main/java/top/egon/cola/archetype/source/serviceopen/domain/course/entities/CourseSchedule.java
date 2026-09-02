package top.egon.cola.archetype.source.serviceopen.domain.course.entities;

import top.egon.cola.archetype.source.serviceopen.domain.course.enums.CourseScheduleStatus;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseId;
import java.time.Instant;

public final class CourseSchedule {

    private final Long id;
    private final CourseId courseId;
    private final Long classId;
    private final Instant startsAt;
    private final Instant endsAt;
    private final CourseScheduleStatus status;

    public CourseSchedule(
            Long id,
            CourseId courseId,
            Long classId,
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

    public Long getId() { return id; }
    public CourseId getCourseId() { return courseId; }
    public Long getClassId() { return classId; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public CourseScheduleStatus getStatus() { return status; }
}
