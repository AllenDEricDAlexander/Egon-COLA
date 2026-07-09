package ${package}.infrastructure.teaching.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "courses")
public class CoursePO {
    @Id
    private String id;
    @Column(name = "course_code", nullable = false, unique = true)
    private String courseCode;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CoursePO() {
    }

    public CoursePO(String id, String courseCode, String name, String status, Instant createdAt) {
        this.id = id;
        this.courseCode = courseCode;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getCourseCode() { return courseCode; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
