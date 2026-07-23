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
@Table(name = "exam")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExamPo {
    @Id private String id;
    @Column(name = "course_id", nullable = false) private String courseId;
    @Column(nullable = false) private String title;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public String getId() { return id; } public String getCourseId() { return courseId; }
    public String getTitle() { return title; } public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; } public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
