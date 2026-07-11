#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "course_schedule")
public class CourseSchedulePo {
    @Id private String id;
    @Column(name = "course_id", nullable = false) private String courseId;
    @Column(name = "class_id", nullable = false) private String classId;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CourseSchedulePo() { }

    public CourseSchedulePo(
            String id, String courseId, String classId, Instant startsAt, Instant endsAt,
            String status, Instant createdAt, Instant updatedAt) {
        this.id = id; this.courseId = courseId; this.classId = classId;
        this.startsAt = startsAt; this.endsAt = endsAt; this.status = status;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public String getClassId() { return classId; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public String getStatus() { return status; }
}
