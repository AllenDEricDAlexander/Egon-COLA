package top.egon.cola.component.yuheng.admin.routing.domain.bo;

import java.time.Instant;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code GatewayDraftBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code GatewayDraftBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class GatewayDraftBO {

    private String gatewayGroupId;
    private long revision;
    private String basedOnReleaseId;
    private String status;
    private String changeSummary;
    private Instant updatedAt;
    private String updatedBy;

    public void assertEditable(long expectedRevision) {
            if (revision != expectedRevision) {
                throw new top.egon.cola.component.yuheng.admin.shared.domain.exception
                        .GatewayAdminRevisionConflictException(revision);
            }
            if (!"EDITABLE".equals(status)) {
                throw new IllegalStateException(
                        "YUHENG_ADMIN_DRAFT_NOT_EDITABLE"
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

    /**
         * 中文说明：创建 {@code GatewayDraftBO} 实例并保留原持久化载体的构造语义（业务 revision、审计时间与缺省状态）。
         * English summary: Creates a {@code GatewayDraftBO} while preserving the construction semantics of the legacy carrier (business revision, audit timestamps and default state).
         *
         * 用法 / Usage: 由服务层在建立新业务对象时调用；/ Call it from the service layer when a new business object is created.
         */
        public GatewayDraftBO(
                String gatewayGroupId,
                String actor,
                Instant now) {
            this.gatewayGroupId = gatewayGroupId;
            status = "EDITABLE";
            updatedBy = actor;
            updatedAt = now;
        }
}
