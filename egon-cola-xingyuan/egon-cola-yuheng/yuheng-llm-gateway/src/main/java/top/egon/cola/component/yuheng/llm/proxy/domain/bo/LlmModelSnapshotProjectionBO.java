package top.egon.cola.component.yuheng.llm.proxy.domain.bo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;

/**
 * 中文说明：{@code LlmModelSnapshotProjectionBO} 是 repository 完成 JSONB 校验/解码后交给 MapStruct 的内部投影来源，
 * 不含持久化 PO、技术 id、租户或审计字段。
 * English summary: Internal typed projection input passed from the repository to MapStruct after JSONB validation/decoding;
 * it contains no persistence PO, technical id, tenant or audit fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class LlmModelSnapshotProjectionBO {

    private String modelKey;

    private String name;

    private Instant createdAt;

    private LlmModelKindEnum kind;

    private Boolean enabled;

    private List<LlmProtocolEnum> protocols;

    private Integer dimensions;

    private String embeddingSpaceId;

    private List<String> allowedSubjects;

    private Long revision;

    private List<LlmModelSnapshotBO.RouteBO> routes;

    public List<LlmProtocolEnum> getProtocols() {
        return protocols == null ? null : Collections.unmodifiableList(new ArrayList<>(protocols));
    }

    public LlmModelSnapshotProjectionBO setProtocols(List<LlmProtocolEnum> protocols) {
        this.protocols = protocols == null ? null : new ArrayList<>(protocols);
        return this;
    }

    public List<String> getAllowedSubjects() {
        return allowedSubjects == null ? null : Collections.unmodifiableList(new ArrayList<>(allowedSubjects));
    }

    public LlmModelSnapshotProjectionBO setAllowedSubjects(List<String> allowedSubjects) {
        this.allowedSubjects = allowedSubjects == null ? null : new ArrayList<>(allowedSubjects);
        return this;
    }

    public List<LlmModelSnapshotBO.RouteBO> getRoutes() {
        return routes == null ? null : Collections.unmodifiableList(new ArrayList<>(routes));
    }

    public LlmModelSnapshotProjectionBO setRoutes(List<LlmModelSnapshotBO.RouteBO> routes) {
        this.routes = routes == null ? null : new ArrayList<>(routes);
        return this;
    }
}
