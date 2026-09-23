package top.egon.cola.component.yuheng.admin.wiki.domain.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：{@code WikiGraphVO} 是原 API-028 的关联与来源视图，节点与边都限定在当前主体可见的已发布页面；
 * 越权标题一律不出现。
 * English summary: {@code WikiGraphVO} is the API-028 relation view restricted to published pages the caller may
 * see, so unauthorized titles never surface.
 *
 * 用法 / Usage: 节点最多 100、边最多 200，达到上限时 truncated=true 由 UI 提示缩小范围；
 * 链接关系来自 revision links，不落独立 graph_edges 表。
 * 图元素合同只在本载体内部声明为 {@link WikiGraphNodeVO}/{@link WikiGraphEdgeVO}，避免新增未批准顶层类型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class WikiGraphVO {

    /** 授权发布页面图节点，最多 100 / at most 100 authorized published page nodes. */
    @NotNull
    @Size(max = 100)
    @Valid
    private List<WikiGraphNodeVO> nodes;

    /** 可见节点之间一跳链接，最多 200 / at most 200 one-hop links between visible nodes. */
    @NotNull
    @Size(max = 200)
    @Valid
    private List<WikiGraphEdgeVO> edges;

    /** 达到图输出上限时 true / true when the graph output hit its limits. */
    @NotNull
    private Boolean truncated;

    /**
     * 中文说明：返回 nodes 的不可变快照副本，未设置时输出 {@code []}。
     * English summary: Returns an unmodifiable copy of the nodes, or an empty list.
     */
    public List<WikiGraphNodeVO> getNodes() {
        return nodes == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(nodes));
    }

    /**
     * 中文说明：写入时复制 nodes。
     * English summary: Defensively copies the incoming nodes.
     */
    public WikiGraphVO setNodes(List<WikiGraphNodeVO> nodes) {
        this.nodes = nodes == null ? null : new ArrayList<>(nodes);
        return this;
    }

    /**
     * 中文说明：返回 edges 的不可变快照副本，未设置时输出 {@code []}。
     * English summary: Returns an unmodifiable copy of the edges, or an empty list.
     */
    public List<WikiGraphEdgeVO> getEdges() {
        return edges == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(edges));
    }

    /**
     * 中文说明：写入时复制 edges。
     * English summary: Defensively copies the incoming edges.
     */
    public WikiGraphVO setEdges(List<WikiGraphEdgeVO> edges) {
        this.edges = edges == null ? null : new ArrayList<>(edges);
        return this;
    }

    /**
     * 中文说明：{@code WikiGraphNodeVO} 是图节点投影，只含原 §9 合同列出的 id 与 title 两个字段。
     * English summary: {@code WikiGraphNodeVO} is the graph node projection holding exactly the id and title of §9.
     *
     * 用法 / Usage: 作为 {@code WikiGraphVO} 的限定嵌套类型输出，不新增未批准的顶层载体。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Accessors(chain = true)
    public static class WikiGraphNodeVO {

        /** 页面稳定 ID / page id. */
        @NotBlank
        @Pattern(regexp = "^[1-9][0-9]{0,19}$")
        private String id;

        /** 页面标题 1–128 / page title. */
        @NotBlank
        @Size(max = 128)
        private String title;
    }

    /**
     * 中文说明：{@code WikiGraphEdgeVO} 是一条可见节点之间的一跳链接，端点沿用 §9 的 links 页面 ID 语义。
     * English summary: {@code WikiGraphEdgeVO} is one hop between two visible nodes using the §9 link page ids.
     *
     * 用法 / Usage: 两端必须都在 nodes 内；未解析链接不伪造目标，因此不会生成悬空边。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Accessors(chain = true)
    public static class WikiGraphEdgeVO {

        /** 起点页面稳定 ID / source page id. */
        @NotBlank
        @Pattern(regexp = "^[1-9][0-9]{0,19}$")
        private String source;

        /** 终点页面稳定 ID / target page id. */
        @NotBlank
        @Pattern(regexp = "^[1-9][0-9]{0,19}$")
        private String target;
    }
}
