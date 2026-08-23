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
    @Id private String id;
    @Column(name = "exam_id", nullable = false) private String examId;
    @Column(name = "course_id", nullable = false) private String courseId;
    @Column(name = "student_id", nullable = false) private String studentId;
    @Column(nullable = false) private int points;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public String getId() { return id; } public String getExamId() { return examId; }
    public String getCourseId() { return courseId; } public String getStudentId() { return studentId; }
    public int getPoints() { return points; } public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void update(
            String courseId, String studentId, int points, String status, Instant updatedAt) {
        this.courseId = courseId;
        this.studentId = studentId;
        this.points = points;
        this.status = status;
        this.updatedAt = updatedAt;
    }
}
