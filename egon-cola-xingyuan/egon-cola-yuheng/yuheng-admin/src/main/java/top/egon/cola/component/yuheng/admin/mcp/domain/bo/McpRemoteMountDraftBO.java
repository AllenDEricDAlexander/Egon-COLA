package top.egon.cola.component.yuheng.admin.mcp.domain.bo;

import java.util.Map;
import java.util.Objects;
import top.egon.cola.component.yuheng.admin.mcp.repository.jdbc.McpJdbcJson;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：{@code McpRemoteMountDraftBO} 是业务载体，保留原持久化载体的业务字段与投影语义，不继承 {@code EgonModel}，也不承载持久化列注解。
 * English summary: {@code McpRemoteMountDraftBO} is the business carrier holding the fields and projection semantics of the legacy persistence carrier; it neither extends {@code EgonModel} nor carries persistence column annotations.
 *
 * 用法 / Usage: 仅在应用与领域层之间传递业务事实；持久边界由 RecordPO 与 Converter 负责。/ Use it between application and domain layers only; the persistence boundary stays on the RecordPO and its converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class McpRemoteMountDraftBO {

    private String id;
    private String gatewayGroupId;
    private String serverId;
    private String providerId;
    private String namespace;
    private String capabilityFingerprint;
    private Map<String, Object> content;
    private boolean enabled;
    private long revision;

    /**
     * 中文说明：按原持久化载体的紧凑构造器契约建立远程挂载草稿载体；Lombok 规范构造器不承担这些不变式，故读写边界必须经此工厂。
     * English summary: Builds a remote mount draft carrier enforcing the invariants of the legacy persistence carrier's compact constructor; Lombok's canonical constructor cannot, so read/write boundaries must go through this factory.
     *
     * 用法 / Usage: 由 Jdbc 载入与服务写入构造草稿时调用；/ Call it when the Jdbc load path or a service writes a draft.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param providerId 参数 提供方Id；parameter provider id。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param capabilityFingerprint 参数 capabilityFingerprint；parameter capability fingerprint。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param revision 参数 revision；parameter revision。
     * @return 返回已完成必填校验与防御性复制的草稿载体；returns the validated, defensively copied draft carrier.
     */
    public static McpRemoteMountDraftBO normalized(
            String id,
            String gatewayGroupId,
            String serverId,
            String providerId,
            String namespace,
            String capabilityFingerprint,
            Map<String, Object> content,
            boolean enabled,
            long revision
    ) {
        String checkedId = McpJdbcJson.required(id, "id");
        String checkedGatewayGroupId =
                McpJdbcJson.required(gatewayGroupId, "gatewayGroupId");
        String checkedServerId = McpJdbcJson.required(serverId, "serverId");
        String checkedProviderId =
                McpJdbcJson.required(providerId, "providerId");
        String checkedNamespace =
                McpJdbcJson.required(namespace, "namespace");
        String checkedCapabilityFingerprint = McpJdbcJson.required(
                capabilityFingerprint,
                "capabilityFingerprint"
        );
        Map<String, Object> checkedContent =
                Map.copyOf(Objects.requireNonNull(content, "content"));
        return new McpRemoteMountDraftBO(
                checkedId,
                checkedGatewayGroupId,
                checkedServerId,
                checkedProviderId,
                checkedNamespace,
                checkedCapabilityFingerprint,
                checkedContent,
                enabled,
                revision
        );
    }
}
