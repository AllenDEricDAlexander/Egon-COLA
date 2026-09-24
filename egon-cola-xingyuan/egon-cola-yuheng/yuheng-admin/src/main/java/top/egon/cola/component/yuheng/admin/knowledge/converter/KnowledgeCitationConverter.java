package top.egon.cola.component.yuheng.admin.knowledge.converter;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeChunkPO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeCitationVO;

/**
 * 中文说明：{@code KnowledgeCitationConverter} 是 API-022 引用条目的唯一出站投影器，把
 * {@code gateway_knowledge_chunk} 的 {@link KnowledgeChunkPO} 行模型单向投影为 {@link KnowledgeCitationVO}：
 * 只带出四个可追溯业务事实——冻结资料版本标识 {@code documentRevisionId}、稳定分块标识 {@code chunkId}
 * （两者按十进制文本投影，绝不使用查询列表下标）、冻结来源 hash {@code sourceHash}（用于 stale 检查）与
 * 授权证据节选 {@code excerpt}（按声明上限做代理对安全的截断）。
 * 本投影刻意不含 {@code rawBytes}、{@code embedding} 向量、chunk 内部 {@code metadata}、租户、审计、软删与技术版本列，
 * 也不含任何密钥或任务租约列；这些字段要么不存在于源行、要么在 {@link KnowledgeCitationVO} 上被显式
 * {@code ignore}，因此反向不可重建源行，本类实现 {@code BaseForwardConverter} 而不提供 {@code toSource}，
 * 也不用 {@code UnsupportedOperationException} 占位。
 * English summary: {@code KnowledgeCitationConverter} is the only outbound projector of an API-022 citation, projecting a
 * {@link KnowledgeChunkPO} row of {@code gateway_knowledge_chunk} one way onto {@link KnowledgeCitationVO}: it carries exactly
 * four traceable business facts, the frozen document-revision identifier and the stable chunk identifier (both as decimal text
 * and never as a result-list index), the frozen source hash used for staleness checks, and the authorized excerpt truncated in a
 * surrogate-pair-safe way to its declared bound. The projection deliberately excludes {@code rawBytes}, the
 * {@code embedding} vector, the chunk's internal {@code metadata}, the tenant, audit, soft-delete and technical version columns,
 * and any secret or job-lease column; those either do not exist on the source row or are explicitly {@code ignore}d on
 * {@link KnowledgeCitationVO}, so the source row cannot be rebuilt, which is why this class implements
 * {@code BaseForwardConverter} with no {@code toSource} and no {@code UnsupportedOperationException} placeholder.
 *
 * 用法 / Usage: 由 {@code KnowledgeRetrievalService}（Step 13 实现）在装配答案引用时调用（bean 名
 * {@code knowledgeCitationConverter}），单条走 {@code toTarget}、整批走继承来的 {@code toTargetList}，
 * 传入 {@code null} 如实得到 {@code null} 而不伪造引用。本投影只完成「分块行 -> 引用」这一段：
 * {@code documentId} 与 {@code fileName} 需要联结 {@code gateway_knowledge_document}/冻结版本行、
 * {@code pageId} 只在 WIKI 来源下由 wiki 页面血缘给出、{@code score} 由检索打分产出，
 * 三者在投影之后由调用方补齐，因此投影结果在补齐前不满足 VO 自身的声明约束。
 * Injected under the bean name {@code knowledgeCitationConverter} in Step 13's authorized retrieval service when assembling
 * answer citations, one row at a time through {@code toTarget} or a whole batch through the inherited {@code toTargetList},
 * with {@code null} yielding {@code null} instead of a fabricated citation. This projector covers only the chunk-row to
 * citation segment: {@code documentId} and {@code fileName} need a join onto the document and the frozen revision,
 * {@code pageId} comes from the wiki page lineage for WIKI sources only, and {@code score} is produced by retrieval ranking, so
 * the caller fills those three afterwards and the projection does not satisfy the VO's own declared constraints until it does.
 */
@Slf4j
@Component("knowledgeCitationConverter")
public class KnowledgeCitationConverter implements BaseForwardConverter<
        KnowledgeChunkPO,
        KnowledgeCitationVO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final KnowledgeCitationProjection MAPPING =
            Mappers.getMapper(KnowledgeCitationProjection.class);

    /**
     * 中文说明：执行 toTarget 操作，把分块行模型单向投影为授权引用条目，只带出四个可追溯业务事实；
     * 传入 {@code null} 时如实返回 {@code null} 并留下调试日志，不伪造引用。
     * English summary: Executes the toTarget operation, projecting a chunk row one way onto the authorized citation carrying only
     * the four traceable business facts; {@code null} input yields {@code null} with a debug log instead of a fabricated
     * citation.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeCitationConverter.toTarget(chunkRow)}。
     * 整批投影用继承的 {@code toTargetList(List)}。/ Use the inherited {@code toTargetList(List)} for a whole batch.
     * @param source 参数 分块行模型；parameter the chunk row model.
     * @return 返回 引用投影；returns the citation projection.
     */
    @Override
    public KnowledgeCitationVO toTarget(KnowledgeChunkPO source) {
        if (source == null) {
            log.debug("gateway_knowledge_chunk row is absent; no citation projection rendered");
            return null;
        }
        return MAPPING.toView(source);
    }
}

/**
 * 中文说明：{@code KnowledgeCitationProjection} 是分块行到引用条目的 MapStruct 结构映射契约，
 * {@code unmappedTargetPolicy=ERROR} 强制 {@link KnowledgeCitationVO} 的每个字段都显式表态：
 * 四个字段投影自分块行的可追溯业务列，另外四个字段在此如实 {@code ignore} 并说明归属——
 * {@code documentId}/{@code fileName} 不在分块行上、需要检索侧联结资料与冻结版本，
 * {@code pageId} 只在 WIKI 来源下由 wiki 页面血缘给出、资料来源保持 {@code null}，
 * {@code score} 是检索排序打分而非存储列。源行的 {@code rawBytes}（在版本行上）、{@code embedding} 向量、
 * {@code metadata}、租户/审计/软删/版本与任务租约列都不在本投影的目标类型中，因命名巧合被带出的可能性由
 * 逐字段 {@code @Mapping} 与 ERROR 策略共同封堵。
 * English summary: {@code KnowledgeCitationProjection} is the MapStruct structural contract from the chunk row to the citation,
 * where {@code unmappedTargetPolicy=ERROR} forces every field of {@link KnowledgeCitationVO} to state itself: four fields
 * project from the traceable business columns of the chunk row and the other four are {@code ignore}d truthfully with their
 * ownership recorded—{@code documentId} and {@code fileName} are not on the chunk row and need the retrieval side to join the
 * document and the frozen revision, {@code pageId} comes from the wiki page lineage for WIKI sources only and stays
 * {@code null} for document-only citations, and {@code score} is a retrieval ranking value rather than a stored column. The
 * source row's {@code rawBytes} (which lives on the revision row), the {@code embedding} vector, {@code metadata}, the tenant,
 * audit, soft-delete and version columns and the job lease columns are absent from the target type altogether, and the risk of
 * them leaking through a naming coincidence is closed by the per-field {@code @Mapping} declarations plus the ERROR policy.
 *
 * 用法 / Usage: 由 {@link KnowledgeCitationConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link KnowledgeCitationConverter} through {@code Mappers}; neither published to callers nor registered as a Spring
 * bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface KnowledgeCitationProjection {

    /**
     * 中文说明：{@link KnowledgeCitationVO#getExcerpt()} 声明的节选上限，投影时按该上限做代理对安全截断。
     * English summary: The excerpt bound declared by {@link KnowledgeCitationVO#getExcerpt()}, enforced during projection with a
     * surrogate-pair-safe cut.
     */
    int EXCERPT_MAX_LENGTH = 1_000;

    /**
     * 中文说明：执行 toView 操作，按 {@link KnowledgeCitationVO} 的字段顺序投影 gateway_knowledge_chunk 行；
     * 只投影可追溯业务事实，四个需要联结或打分的字段显式忽略。
     * English summary: Executes the toView operation, projecting a gateway_knowledge_chunk row onto
     * {@link KnowledgeCitationVO}; only traceable business facts are projected and the four fields needing a join or a ranking
     * score are explicitly ignored.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeCitationConverter#toTarget(KnowledgeChunkPO)} 调用。
     * @param source 参数 分块行模型；parameter the chunk row model.
     * @return 返回 引用投影；returns the citation projection.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "documentRevisionId", expression = "java( text( source.getRevisionId() ) )")
    @Mapping(target = "chunkId", expression = "java( text( source.getId() ) )")
    @Mapping(target = "sourceHash", source = "contentHash")
    // 资料标识与显示名不在 gateway_knowledge_chunk 上：由检索侧联结 gateway_knowledge_document 与冻结版本行补齐。
    @Mapping(target = "documentId", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    // pageId 只在 WIKI 来源下由 wiki 页面血缘给出，资料引用合同要求保持 null，绝不从 chunk metadata 猜测。
    @Mapping(target = "pageId", ignore = true)
    // score 是检索排序打分而非存储列；向量、rawBytes、metadata、租户/审计/版本与租约列一律不在出站形态里。
    @Mapping(target = "score", ignore = true)
    @Mapping(target = "excerpt", expression = "java( excerpt( source.getContent() ) )")
    KnowledgeCitationVO toView(KnowledgeChunkPO source);

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 标识按十进制文本投影，{@code null} 保持 {@code null}
     * 而由引用条目的声明约束如实拒绝，绝不伪造 0 或列表下标。
     * English summary: Executes the text operation, projecting an MP bigint identifier as decimal text and keeping {@code null}
     * {@code null} so the citation's declared constraints reject it truthfully instead of fabricating a zero or a list index.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 标识；parameter identifier。
     * @return 返回 十进制文本；returns the decimal text.
     */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /**
     * 中文说明：执行 excerpt 操作，把分块正文裁剪为不超过 {@code EXCERPT_MAX_LENGTH} 的授权节选，
     * 结尾若落在代理对高半区则回退一格，避免产出孤立码元；缺失如实保持缺失，不追加省略号也不伪造文本。
     * English summary: Executes the excerpt operation, cutting the chunk content down to at most
     * {@code EXCERPT_MAX_LENGTH} of authorized excerpt and stepping back one char when the cut would land on a high surrogate so
     * no orphan code unit is emitted; an absent value stays absent and neither an ellipsis nor fabricated text is added.
     *
     * {@code @Named} 保证本方法只由 excerpt 的显式表达式调用，不被 MapStruct 当成通用 {@code String} 换算挑中，
     * 否则 {@code sourceHash} 这类同型字符串列会被悄悄截断。
     * English summary: {@code @Named} keeps this method reachable only from the explicit excerpt expression so MapStruct never
     * picks it up as a generic {@code String} converter, which would silently truncate a same-shaped column such as
     * {@code sourceHash}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param content 参数 分块正文；parameter the chunk content.
     * @return 返回 授权节选或 {@code null}；returns the authorized excerpt or {@code null}.
     */
    @Named("boundedExcerpt")
    default String excerpt(String content) {
        if (content == null || content.length() <= EXCERPT_MAX_LENGTH) {
            return content;
        }
        String head = content.substring(0, EXCERPT_MAX_LENGTH);
        if (Character.isHighSurrogate(head.charAt(head.length() - 1))) {
            return head.substring(0, head.length() - 1);
        }
        return head;
    }
}
