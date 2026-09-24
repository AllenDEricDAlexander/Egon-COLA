package top.egon.cola.component.yuheng.admin.wiki.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiRevisionBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiSourceDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationPolicyEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiReviewStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.po.WikiRevisionPO;

/**
 * 中文说明：{@code WikiRevisionPersistenceConverter} 是 {@code gateway_wiki_revision} 在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link WikiRevisionPO} 行模型与 {@link WikiRevisionBO} 业务载体之间的双向映射：主键与所属页面、生成作业三个
 * bigint 标识以十进制文本往返，作业缺失仍为 {@code null}；{@code tags}/{@code links}/{@code sources} 三列在
 * {@code jsonb} 与冻结列表之间经 Jackson 换算，读侧解析失败按「存储列已被改写」如实抛出而绝不降级成空列表；
 * 三个封闭列（发布状态、评审状态、发布策略快照）只承认各自的 wire 词汇；{@code publicationVersion} 与
 * {@code revision} 把可空 bigint 投影为正 long（缺列即 DDL 初值 1）；{@code createdAt}/{@code updatedAt} 只读地投影自
 * MP 审计列，租户、操作者、软删与技术 {@code version} 一律不由业务载体写入。
 * English summary: {@code WikiRevisionPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_wiki_revision}, mapping {@link WikiRevisionPO} rows and {@link WikiRevisionBO} carriers both ways: the key and
 * the two parent bigint identifiers round-trip as decimal text with an absent job staying {@code null}; the three
 * {@code jsonb} columns convert between stored JSON and frozen lists through Jackson, a payload that no longer parses
 * surfacing as tampered storage instead of degrading into an empty list; the three closed columns (publication status,
 * review status and the policy snapshot) accept only their own wire vocabulary; {@code publicationVersion} and
 * {@code revision} project a nullable bigint as a positive long (an absent column reading as the DDL default of one); the
 * audit instants are read-only projections of the MP audit columns while tenant, operator, soft-delete and the technical
 * {@code version} are never written from the carrier.
 *
 * 用法 / Usage: 由 {@code MpWikiRepository} 注入使用（bean 名 {@code wikiRevisionPersistenceConverter}）。{@code newRow}
 * 产出只带内容列与冻结快照的待插入行——正文、标签、链接与来源在此一次落定，之后只能由 {@code applyLifecycle}
 * 覆盖状态与痕迹列；{@code applyLifecycle} 明确忽略全部内容列与两个定位键，因为修订内容不可变，状态迁移只推进
 * {@code publication_version}。行模型与原始 JSON 列不得越过持久边界出现在端口、服务签名或对外投影里。
 * Injected by {@code MpWikiRepository} as {@code wikiRevisionPersistenceConverter}. {@code newRow} renders a row carrying the
 * content columns and the frozen snapshot — title, body, tags, links and sources settle exactly once there, after which only
 * {@code applyLifecycle} may rewrite the status and trace columns, since revision content is immutable and a transition
 * advances {@code publication_version} alone; {@code applyLifecycle} deliberately ignores every content column and both parent
 * locators. Neither the row model nor its raw JSON columns may cross the persistence boundary into a port, a signature or an
 * outbound projection.
 */
@Slf4j
@Component("wikiRevisionPersistenceConverter")
public class WikiRevisionPersistenceConverter implements BaseConverter<
        WikiRevisionPO,
        WikiRevisionBO> {

    /** MapStruct 生成的同包结构映射器，本类只做空值守护与日志边界。 / The MapStruct generated same-package mapper; this class only adds null guarding and logging. */
    private static final WikiRevisionMapping MAPPING = Mappers.getMapper(WikiRevisionMapping.class);

    /**
     * 中文说明：把修订行模型投影为业务载体。
     * English summary: Projects a revision row onto the business carrier.
     * @param row 参数 行模型，可为 {@code null}；parameter the row model, nullable.
     * @return 返回 业务载体或 {@code null}；returns the carrier or {@code null}.
     */
    public WikiRevisionBO toBusiness(WikiRevisionPO row) {
        if (row == null) {
            log.debug("gateway_wiki_revision row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：把业务载体渲染为行模型。
     * English summary: Renders a business carrier into the MyBatis-Plus row model.
     * @param carrier 参数 业务载体，可为 {@code null}；parameter the carrier, nullable.
     * @return 返回 行模型或 {@code null}；returns the row model or {@code null}.
     */
    public WikiRevisionPO toPersistence(WikiRevisionBO carrier) {
        if (carrier == null) {
            log.debug("gateway_wiki_revision business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：按列表顺序投影修订行；空输入返回空列表而非 {@code null}。
     * English summary: Projects revision rows in list order; empty input yields an empty list, never {@code null}.
     * @param rows 参数 行模型列表；parameter the row models.
     * @return 返回 业务载体列表；returns the carriers.
     */
    public List<WikiRevisionBO> toBusinessList(List<WikiRevisionPO> rows) {
        return toTargetList(rows);
    }

    /** {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。 / The forward projection (row model to business carrier). */
    @Override
    public WikiRevisionBO toTarget(WikiRevisionPO source) {
        return toBusiness(source);
    }

    /** {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。 / The reverse rendering (business carrier to row model). */
    @Override
    public WikiRevisionPO toSource(WikiRevisionBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：{@code toPersistence} 的插入别名，语义为「只带内容与冻结快照的新行」：内容 hash、作者、策略快照与
     * 两个正 long 版本都在载体里已定，租户、审计、软删与版本列留空交由受守卫边界补齐。
     * English summary: An insert alias of {@code toPersistence} rendering a new row carrying content and the frozen snapshot only:
     * the digest, author, policy snapshot and the two positive versions already settled on the carrier, while tenant, audit,
     * soft-delete and version stay empty for the guarded boundary.
     * @param carrier 参数 业务载体；parameter the carrier.
     * @return 返回 待插入行模型；returns the row to insert.
     */
    public WikiRevisionPO newRow(WikiRevisionBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：在已加载的活跃行上只覆盖状态与痕迹列——发布状态、评审状态、状态版本、审核三元组、发布三元组、
     * 归档三元组、错误码与「曾发布过」标记；内容列、两个定位键、{@code revision} 与受保护元数据一律保留，
     * 这正是「内容不可变、生命周期可迁移」在持久边界的落点。
     * English summary: Overwrites only the status and trace columns of a loaded active row — publication and review status, the
     * status version, the review/publication/archive triples, the error code and the ever-published flag — keeping every content
     * column, both parent locators, {@code revision} and the protected metadata. That is where "content immutable, lifecycle
     * migratable" lands at the persistence boundary.
     * @param carrier 参数 目标状态已就位的载体；parameter the carrier holding the target status.
     * @param row 参数 已加载行模型；parameter the loaded row model.
     */
    public void applyLifecycle(WikiRevisionBO carrier, WikiRevisionPO row) {
        if (carrier == null || row == null) {
            log.debug("gateway_wiki_revision carrier or row is absent; nothing applied");
            return;
        }
        MAPPING.applyLifecycle(carrier, row);
    }
}

/**
 * 中文说明：{@code WikiRevisionMapping} 是 gateway_wiki_revision 的 MapStruct 结构映射契约，逐列声明读写形态：三个
 * bigint 标识在 {@code Long} 与十进制 {@code String} 之间换算，三个 {@code jsonb} 列在 {@code JsonNode} 与冻结列表之间
 * 经 Jackson 换算（解析失败按存储被改写抛出），三个封闭列经 {@code fromWire}/{@code wireValue} 失败关闭地往返，
 * 两个版本列把可空 bigint 读成正 long；{@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code WikiRevisionMapping} is the MapStruct structural contract for gateway_wiki_revision, stating every
 * column shape: the three bigint identifiers convert between {@code Long} and decimal {@code String}, the three {@code jsonb}
 * columns convert between {@code JsonNode} and frozen lists through Jackson (a payload that no longer parses meaning tampered
 * storage), the three closed columns round-trip fail-closed through {@code fromWire}/{@code wireValue}, and the two version
 * columns read a nullable bigint as a positive long; {@code unmappedTargetPolicy=ERROR} forces every new column to be declared here.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface WikiRevisionMapping {

    /** 修订内容在两个方向上都走同一份 Jackson 视图，无需注入容器里的 mapper 实例。 / The JSON columns round-trip through their own Jackson view, no container mapper needed. */
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 来源三元组的存储形状。 / the stored shape of a source triple. */
    TypeReference<List<WikiSourceDTO>> SOURCE_LIST = new TypeReference<>() {
    };

    /** 字符串数组列的存储形状。 / the stored shape of a string array column. */
    TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    /**
     * 中文说明：按 {@code WikiRevisionBO} 的字段顺序投影修订行。
     * English summary: Projects a revision row onto {@code WikiRevisionBO}.
     * @param source 参数 行模型；parameter the row model.
     * @return 返回 业务载体；returns the carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( text( source.getKbId() ) )")
    @Mapping(target = "pageId", expression = "java( text( source.getPageId() ) )")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "markdown", source = "markdown")
    @Mapping(target = "tags", expression = "java( strings( source.getTags() ) )")
    @Mapping(target = "links", expression = "java( strings( source.getLinks() ) )")
    @Mapping(target = "sources", expression = "java( sources( source.getSources() ) )")
    @Mapping(target = "contentHash", source = "contentHash")
    @Mapping(target = "authorActorId", source = "authorActorId")
    @Mapping(target = "generationJobId", expression = "java( text( source.getGenerationJobId() ) )")
    @Mapping(target = "publicationStatus", expression = "java( publication( source.getPublicationStatus() ) )")
    @Mapping(target = "reviewStatus", expression = "java( review( source.getReviewStatus() ) )")
    @Mapping(target = "publicationPolicySnapshot", expression = "java( policy( source.getPublicationPolicySnapshot() ) )")
    @Mapping(target = "publicationVersion", expression = "java( positive( source.getPublicationVersion() ) )")
    @Mapping(target = "reviewInstanceId", source = "reviewInstanceId")
    @Mapping(target = "reviewerActorId", source = "reviewerActorId")
    @Mapping(target = "reviewedAt", source = "reviewedAt")
    @Mapping(target = "reviewDecisionCode", source = "reviewDecisionCode")
    @Mapping(target = "publishedAt", source = "publishedAt")
    @Mapping(target = "publishedByActorId", source = "publishedByActorId")
    @Mapping(target = "archivedAt", source = "archivedAt")
    @Mapping(target = "archivedByActorId", source = "archivedByActorId")
    @Mapping(target = "publicationErrorCode", source = "publicationErrorCode")
    @Mapping(target = "everPublished", source = "everPublished")
    @Mapping(target = "revision", expression = "java( positive( source.getRevision() ) )")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    WikiRevisionBO toBusiness(WikiRevisionPO source);

    /**
     * 中文说明：把业务载体渲染为待插入行，只搬运业务列并把十进制文本换算回 bigint。
     * English summary: Renders the carrier into a row, transporting business columns and converting decimal text back to bigint.
     * @param source 参数 业务载体；parameter the carrier.
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( identifier( source.getKbId() ) )")
    @Mapping(target = "pageId", expression = "java( identifier( source.getPageId() ) )")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "markdown", source = "markdown")
    @Mapping(target = "tags", expression = "java( json( source.getTags() ) )")
    @Mapping(target = "links", expression = "java( json( source.getLinks() ) )")
    @Mapping(target = "sources", expression = "java( json( source.getSources() ) )")
    @Mapping(target = "contentHash", source = "contentHash")
    @Mapping(target = "authorActorId", source = "authorActorId")
    @Mapping(target = "generationJobId", expression = "java( identifier( source.getGenerationJobId() ) )")
    @Mapping(target = "publicationStatus", expression = "java( wire( source.getPublicationStatus() ) )")
    @Mapping(target = "reviewStatus", expression = "java( wire( source.getReviewStatus() ) )")
    @Mapping(target = "publicationPolicySnapshot", expression = "java( wire( source.getPublicationPolicySnapshot() ) )")
    @Mapping(target = "publicationVersion", source = "publicationVersion")
    @Mapping(target = "reviewInstanceId", source = "reviewInstanceId")
    @Mapping(target = "reviewerActorId", source = "reviewerActorId")
    @Mapping(target = "reviewedAt", source = "reviewedAt")
    @Mapping(target = "reviewDecisionCode", source = "reviewDecisionCode")
    @Mapping(target = "publishedAt", source = "publishedAt")
    @Mapping(target = "publishedByActorId", source = "publishedByActorId")
    @Mapping(target = "archivedAt", source = "archivedAt")
    @Mapping(target = "archivedByActorId", source = "archivedByActorId")
    @Mapping(target = "publicationErrorCode", source = "publicationErrorCode")
    @Mapping(target = "everPublished", source = "everPublished")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    WikiRevisionPO toRow(WikiRevisionBO source);

    /**
     * 中文说明：只覆盖状态与痕迹列，内容列与定位键、{@code revision} 及受保护元数据一律忽略——修订内容不可变。
     * English summary: Overwrites the status and trace columns only, ignoring every content column, both locators,
     * {@code revision} and the protected metadata — revision content is immutable.
     * @param source 参数 目标状态已就位的载体；parameter the carrier holding the target status.
     * @param target 参数 已加载行模型；parameter the loaded row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "publicationStatus", expression = "java( wire( source.getPublicationStatus() ) )")
    @Mapping(target = "reviewStatus", expression = "java( wire( source.getReviewStatus() ) )")
    @Mapping(target = "publicationVersion", source = "publicationVersion")
    @Mapping(target = "reviewInstanceId", source = "reviewInstanceId")
    @Mapping(target = "reviewerActorId", source = "reviewerActorId")
    @Mapping(target = "reviewedAt", source = "reviewedAt")
    @Mapping(target = "reviewDecisionCode", source = "reviewDecisionCode")
    @Mapping(target = "publishedAt", source = "publishedAt")
    @Mapping(target = "publishedByActorId", source = "publishedByActorId")
    @Mapping(target = "archivedAt", source = "archivedAt")
    @Mapping(target = "archivedByActorId", source = "archivedByActorId")
    @Mapping(target = "publicationErrorCode", source = "publicationErrorCode")
    @Mapping(target = "everPublished", source = "everPublished")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kbId", ignore = true)
    @Mapping(target = "pageId", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "markdown", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "links", ignore = true)
    @Mapping(target = "sources", ignore = true)
    @Mapping(target = "contentHash", ignore = true)
    @Mapping(target = "authorActorId", ignore = true)
    @Mapping(target = "generationJobId", ignore = true)
    @Mapping(target = "publicationPolicySnapshot", ignore = true)
    @Mapping(target = "revision", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyLifecycle(WikiRevisionBO source, @MappingTarget WikiRevisionPO target);

    /** 中文说明：把可空 bigint 换算为十进制文本，缺失仍为缺失。 English summary: Converts a nullable bigint into decimal text, an absent value staying absent. */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /** 中文说明：把十进制文本换算回 bigint，空白按 {@code null} 处理而非伪造 0。 English summary: Converts decimal text back to bigint, blank meaning {@code null} rather than a fabricated zero. */
    default Long identifier(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    /** 中文说明：把 {@code jsonb} 字符串数组读成冻结列表；形状不符按存储被改写抛出。 English summary: Reads a {@code jsonb} string array as a frozen list, a shape that no longer fits surfacing as tampered storage. */
    default List<String> strings(JsonNode value) {
        if (value == null || value.isNull()) {
            return List.of();
        }
        try {
            return List.copyOf(OBJECT_MAPPER.convertValue(value, STRING_LIST));
        } catch (IllegalArgumentException malformed) {
            throw new IllegalStateException("stored gateway_wiki_revision string array is invalid", malformed);
        }
    }

    /** 中文说明：把 {@code jsonb} 来源数组读成来源三元组列表；形状不符按存储被改写抛出。 English summary: Reads the {@code jsonb} source array as source triples, a shape that no longer fits surfacing as tampered storage. */
    default List<WikiSourceDTO> sources(JsonNode value) {
        if (value == null || value.isNull()) {
            return List.of();
        }
        try {
            return List.copyOf(OBJECT_MAPPER.convertValue(value, SOURCE_LIST));
        } catch (IllegalArgumentException malformed) {
            throw new IllegalStateException("stored gateway_wiki_revision sources are invalid", malformed);
        }
    }

    /** 中文说明：把列表渲染成 {@code jsonb} 节点，空列表保留为 {@code []} 而不是 SQL {@code NULL}，与 DDL 的 NOT NULL 一致。 English summary: Renders a list as a {@code jsonb} node, an empty list staying {@code []} rather than SQL {@code NULL}, as the NOT NULL column requires. */
    default JsonNode json(Object value) {
        return value == null ? OBJECT_MAPPER.nullNode() : OBJECT_MAPPER.valueToTree(value);
    }

    /** 中文说明：把可空 bigint 版本列投影为正 long，缺列按 DDL 初值 1。 English summary: Projects a nullable version column as a positive long, an absent column reading as the DDL default of one. */
    default long positive(Long value) {
        return value == null ? 1L : value;
    }

    /** 中文说明：读侧按封闭词汇换算发布状态，未知值一律拒绝。 English summary: Reads the publication status through its closed vocabulary, rejecting any unknown value. */
    default WikiPublicationStatusEnum publication(String value) {
        return value == null ? null : WikiPublicationStatusEnum.fromWire(value);
    }

    /** 中文说明：读侧按封闭词汇换算评审状态，未知值一律拒绝。 English summary: Reads the review status through its closed vocabulary, rejecting any unknown value. */
    default WikiReviewStatusEnum review(String value) {
        return value == null ? null : WikiReviewStatusEnum.fromWire(value);
    }

    /** 中文说明：读侧按封闭词汇换算策略快照，未知值一律拒绝。 English summary: Reads the policy snapshot through its closed vocabulary, rejecting any unknown value. */
    default WikiPublicationPolicyEnum policy(String value) {
        return value == null ? null : WikiPublicationPolicyEnum.fromWire(value);
    }

    /** 中文说明：写侧统一按 {@code wireValue} 落库，禁止 ordinal 参与持久化。 English summary: Writes through {@code wireValue} only, never an ordinal. */
    default String wire(Enum<?> value) {
        if (value instanceof WikiPublicationStatusEnum status) {
            return status.wireValue();
        }
        if (value instanceof WikiReviewStatusEnum review) {
            return review.wireValue();
        }
        if (value instanceof WikiPublicationPolicyEnum policy) {
            return policy.wireValue();
        }
        return value == null ? null : value.name();
    }
}
