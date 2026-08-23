package ${package}.infrastructure.teaching.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "class_course_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_class_course_start",
                columnNames = {"school_class_id", "course_id", "starts_at"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassCourseSchedulePO {
    @Id
    @Column(length = 36)
    private Long id;
    @Column(name = "school_class_id", nullable = false, length = 36)
    private Long schoolClassId;
    @Column(name = "course_id", nullable = false, length = 36)
    private Long courseId;
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;
    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ClassCourseSchedulePO(
            Long id,
            Long schoolClassId,
            Long courseId,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            Instant createdAt) {
        this.id = id;
        this.schoolClassId = schoolClassId;
        this.courseId = courseId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getSchoolClassId() { return schoolClassId; }
    public Long getCourseId() { return courseId; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public Instant getCreatedAt() { return createdAt; }
}
