package top.egon.cola.component.yuheng.admin.observability.domain.bo;

import java.time.Instant;
import java.util.Map;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code GatewayAuditLogBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code GatewayAuditLogBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class GatewayAuditLogBO {

    private String id;
    private String actorId;
    private String actorType;
    private String source;
    private String requestId;
    private String traceId;
    private String resourceType;
    private String resourceId;
    private String action;
    private Map<String, Object> beforeSummary;
    private Map<String, Object> afterSummary;
    private Long draftRevision;
    private String releaseId;
    private boolean successful;
    private String errorCode;
    private Instant occurredAt;

    public static Map<String, Object> sanitized(Map<String, Object> source) {
            if (source == null || source.isEmpty()) {
                return Map.of();
            }
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            source.forEach((key, value) -> {
                String lower = key.toLowerCase(java.util.Locale.ROOT);
                if (!lower.contains("secret")
                        && !lower.contains("token")
                        && !lower.contains("authorization")
                        && !lower.contains("cookie")) {
                    result.put(key, value);
                }
            });
            return Map.copyOf(result);
        }

    }
