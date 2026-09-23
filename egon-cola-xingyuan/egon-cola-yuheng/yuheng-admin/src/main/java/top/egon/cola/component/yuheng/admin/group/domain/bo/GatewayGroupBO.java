package top.egon.cola.component.yuheng.admin.group.domain.bo;

import java.time.Instant;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code GatewayGroupBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code GatewayGroupBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class GatewayGroupBO {

    private String id;
    private String gatewayGroupCode;
    private String displayName;
    private String env;
    private String namespace;
    private String description;
    private boolean enabled;
    private long revision;
    private boolean deleted;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    public void update(
                String displayName,
                String description,
                String actor,
                Instant now) {
            this.displayName = required(displayName, "displayName");
            this.description = description;
            updatedBy = required(actor, "actor");
            updatedAt = now;
        }

    public void setEnabled(boolean enabled, String actor, Instant now) {
            this.enabled = enabled;
            updatedBy = required(actor, "actor");
            updatedAt = now;
        }

    private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }

    /**
         * 中文说明：创建 {@code GatewayGroupBO} 实例并保留原持久化载体的构造语义（业务 revision、审计时间与缺省状态）。
         * English summary: Creates a {@code GatewayGroupBO} while preserving the construction semantics of the legacy carrier (business revision, audit timestamps and default state).
         *
         * 用法 / Usage: 由服务层在建立新业务对象时调用；/ Call it from the service layer when a new business object is created.
         */
        public GatewayGroupBO(
                String id,
                String gatewayGroupCode,
                String displayName,
                String env,
                String namespace,
                String description,
                String actor,
                Instant now) {
            this.id = required(id, "id");
            this.gatewayGroupCode = required(
                    gatewayGroupCode,
                    "gatewayGroupCode"
            );
            this.displayName = required(displayName, "displayName");
            this.env = required(env, "env");
            this.namespace = required(namespace, "namespace");
            this.description = description;
            enabled = true;
            createdAt = now;
            updatedAt = now;
            createdBy = required(actor, "actor");
            updatedBy = actor;
        }
}
