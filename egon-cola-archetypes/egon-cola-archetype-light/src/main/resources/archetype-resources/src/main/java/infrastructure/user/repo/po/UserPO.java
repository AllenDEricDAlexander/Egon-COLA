package ${package}.infrastructure.user.repo.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class UserPO {
    @Id
    private String id;
    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserPO() {
    }

    public UserPO(String id, String externalId, String name, String email, String status, Instant createdAt) {
        this.id = id;
        this.externalId = externalId;
        this.name = name;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
