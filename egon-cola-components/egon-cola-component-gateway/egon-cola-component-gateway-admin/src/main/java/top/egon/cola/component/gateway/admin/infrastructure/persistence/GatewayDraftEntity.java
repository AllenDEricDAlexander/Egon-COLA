package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "gateway_draft")
public class GatewayDraftEntity {

    @Id
    @Column(name = "gateway_group_id")
    private String gatewayGroupId;

    @Version
    @Column(nullable = false)
    private long revision;

    @Column(name = "based_on_release_id")
    private String basedOnReleaseId;

    @Column(nullable = false)
    private String status;

    @Column(name = "change_summary")
    private String changeSummary;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    protected GatewayDraftEntity() {
    }

    public GatewayDraftEntity(
            String gatewayGroupId,
            String actor,
            Instant now) {
        this.gatewayGroupId = gatewayGroupId;
        status = "EDITABLE";
        updatedBy = actor;
        updatedAt = now;
    }

    public void assertEditable(long expectedRevision) {
        if (revision != expectedRevision) {
            throw new top.egon.cola.component.gateway.admin.domain
                    .GatewayAdminRevisionConflictException(revision);
        }
        if (!"EDITABLE".equals(status)) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_DRAFT_NOT_EDITABLE"
            );
        }
    }

    public void touch(String reason, String actor, Instant now) {
        changeSummary = reason;
        updatedBy = actor;
        updatedAt = now;
    }

    public void changeStatus(String status, String actor, Instant now) {
        this.status = status;
        updatedBy = actor;
        updatedAt = now;
    }

    public void baseOn(String releaseId, String actor, Instant now) {
        basedOnReleaseId = releaseId;
        touch("published " + releaseId, actor, now);
    }

    public String getGatewayGroupId() {
        return gatewayGroupId;
    }

    public long getRevision() {
        return revision;
    }

    public String getBasedOnReleaseId() {
        return basedOnReleaseId;
    }

    public String getStatus() {
        return status;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
