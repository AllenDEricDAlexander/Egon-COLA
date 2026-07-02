package ${package}.infrastructure.repo.student.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_course_assignments")
public class StudentCoursePo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_id")
    private String studentId;
    @Column(name = "course_id")
    private String courseId;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected StudentCoursePo() {
    }

    public StudentCoursePo(String studentId, String courseId, LocalDateTime createdAt) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.createdAt = createdAt;
    }

    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
