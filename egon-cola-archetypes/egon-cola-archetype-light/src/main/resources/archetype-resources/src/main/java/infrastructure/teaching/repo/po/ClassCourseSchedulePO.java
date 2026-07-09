package ${package}.infrastructure.teaching.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_course_schedules")
public class ClassCourseSchedulePO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "school_class_id", nullable = false)
    private String schoolClassId;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;
    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClassCourseSchedulePO() {
    }

    public ClassCourseSchedulePO(
            String schoolClassId,
            String courseId,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            Instant createdAt) {
        this.schoolClassId = schoolClassId;
        this.courseId = courseId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getSchoolClassId() { return schoolClassId; }
    public String getCourseId() { return courseId; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public Instant getCreatedAt() { return createdAt; }
}
