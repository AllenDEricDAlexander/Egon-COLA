#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "score")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ScorePo {
    @Id private Long id;
    @Column(name = "exam_id", nullable = false) private Long examId;
    @Column(name = "course_id", nullable = false) private Long courseId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(nullable = false) private int points;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public Long getId() { return id; } public Long getExamId() { return examId; }
    public Long getCourseId() { return courseId; } public Long getStudentId() { return studentId; }
    public int getPoints() { return points; } public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void update(
            Long courseId, Long studentId, int points, String status, Instant updatedAt) {
        this.courseId = courseId;
        this.studentId = studentId;
        this.points = points;
        this.status = status;
        this.updatedAt = updatedAt;
    }
}
