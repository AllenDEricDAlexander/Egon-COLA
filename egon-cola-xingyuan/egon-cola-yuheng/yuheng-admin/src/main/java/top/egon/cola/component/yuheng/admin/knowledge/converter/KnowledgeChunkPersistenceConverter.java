package top.egon.cola.component.yuheng.admin.knowledge.converter;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeChunkBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeChunkPO;

/**
 * 中文说明：{@code KnowledgeChunkPersistenceConverter} 是 {@code gateway_knowledge_chunk} 在持久边界唯一的 MapStruct
 * 转换器，负责 {@link KnowledgeChunkPO} 行模型与 {@link KnowledgeChunkBO} 业务载体之间的双向映射：主键、父知识库与
 * 父冻结版本三个 bigint 标识以十进制文本往返，{@code embedding} 向量数组在两个方向上都按 {@code clone} 复制而非共享
 * 引用（避免调用方在检索打分时改写已冻结的向量），{@code metadata} 保持 {@code JsonNode} 结构化形态直传，
 * 不再做第二层 JSON 往返；{@code createdAt}/{@code updatedAt} 只读地投影自 MP 审计列，租户、操作者、软删与技术版本列
 * 一律不由业务载体写入。向量的长度与有限性由 Spec 在摄取与检索两侧校验，本转换器不伪造也不裁剪。
 * English summary: {@code KnowledgeChunkPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_knowledge_chunk}, mapping {@link KnowledgeChunkPO} rows and {@link KnowledgeChunkBO} carriers both ways: the
 * primary key and the two parent bigint identifiers round-trip as decimal text, the {@code embedding} vector is copied by
 * {@code clone} in both directions so a caller scoring a search never mutates a frozen vector, {@code metadata} keeps its
 * structured {@code JsonNode} shape without a second JSON round trip, the audit instants are read-only projections of the MP
 * audit columns, and tenant, operator, soft-delete and technical version columns are never written from the business carrier.
 * Vector length and finiteness are asserted by the Spec on both the ingestion and the retrieval side; this converter neither
 * fabricates nor trims them.
 *
 * 用法 / Usage: 由 {@code MpKnowledgeRepository} 注入使用（bean 名 {@code knowledgeChunkPersistenceConverter}）；
 * {@code newRow} 产出只带业务列的待插入行，{@code stageChunks} 的整批暂存即按列表顺序调用它；
 * {@code applyBusiness} 在已加载活跃行上整行覆盖内容、元数据、hash 与向量，保留两个父级定位键、主键与受保护元数据，
 * 并且从不写入业务 {@code revision}。行模型禁止进入端口或服务签名，向量与原文也绝不写入日志或出站投影。
 * Injected by the guarded knowledge store under the bean name {@code knowledgeChunkPersistenceConverter}; {@code newRow} yields
 * a business-columns-only insert candidate and the batch staging calls it in list order, while {@code applyBusiness} replaces
 * the content, metadata, hash and vector of a loaded active row, keeping the two parent locators, the identifier and the
 * protected metadata, and never writing the business {@code revision}. A row model may never reach a port or a service
 * signature, and neither vectors nor content are ever logged or projected outbound.
 */
@Slf4j
@Component("knowledgeChunkPersistenceConverter")
public class KnowledgeChunkPersistenceConverter implements BaseConverter<
        KnowledgeChunkPO,
        KnowledgeChunkBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final KnowledgeChunkMapping MAPPING =
            Mappers.getMapper(KnowledgeChunkMapping.class);

    /**
     * 中文说明：执行 toBusiness 操作，把分块行模型投影为业务载体，向量数组按 {@code clone} 复制而非共享引用。
     * English summary: Executes the toBusiness operation, projecting a chunk row onto the business carrier and copying the vector
     * by {@code clone} instead of sharing a reference.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeChunkPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public KnowledgeChunkBO toBusiness(KnowledgeChunkPO row) {
        if (row == null) {
            log.debug("gateway_knowledge_chunk row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistence 操作，把业务载体渲染为 MP 行模型，向量再次按 {@code clone} 复制后交给 vector 类型处理器。
     * English summary: Executes the toPersistence operation, rendering a chunk carrier into the MyBatis-Plus row model and copying
     * the vector by {@code clone} again before handing it to the vector type handler.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeChunkPersistenceConverter.toPersistence(chunkBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public KnowledgeChunkPO toPersistence(KnowledgeChunkBO carrier) {
        if (carrier == null) {
            log.debug("gateway_knowledge_chunk business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影分块行；null 或空输入返回空列表而非 {@code null}，
     * 与分块数必须等于向量数的合同口径一致（空批次如实为空）。
     * English summary: Executes the toBusinessList operation, projecting every chunk row in order; null or empty input yields an
     * empty list and never {@code null}, matching the contract that the chunk count equals the vector count (an empty batch is
     * truthfully empty).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeChunkPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<KnowledgeChunkBO> toBusinessList(List<KnowledgeChunkPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染分块载体；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toPersistenceList operation, rendering every chunk carrier in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeChunkPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<KnowledgeChunkPO> toPersistenceList(List<KnowledgeChunkBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeChunkPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public KnowledgeChunkBO toTarget(KnowledgeChunkPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeChunkPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public KnowledgeChunkPO toSource(KnowledgeChunkBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」：
     * 分块序号、内容、元数据、hash 与向量取自可信载体，租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only: the chunk index, content, metadata, hash and vector come from the trusted carrier while the tenant, audit,
     * soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeChunkPersistenceConverter.newRow(chunkBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public KnowledgeChunkPO newRow(KnowledgeChunkBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载的活跃行上整行覆盖可写业务列（内容、元数据、hash、冻结的
     * 向量空间与向量都在其中，向量按 {@code clone} 复制），保留两个父级定位键、主键与受保护元数据；
     * 业务 {@code revision} 只由 CAS 语句推进，故此处同样忽略。
     * English summary: Executes the applyBusiness operation, overwrite-replacing the writable business columns of a loaded active
     * row (content, metadata, hash, the frozen embedding space and the vector included, with the vector copied by
     * {@code clone}) while keeping the two parent locators, the identifier and the protected metadata; the business
     * {@code revision} is advanced only by the CAS statement and is therefore ignored here too.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeChunkPersistenceConverter.applyBusiness(chunkBO, row)}。
     * 任一入参为 {@code null} 时如实不处理。/ Either argument being {@code null} is a no-op.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @param row 参数 已加载行模型；parameter the loaded row model.
     */
    public void applyBusiness(KnowledgeChunkBO carrier, KnowledgeChunkPO row) {
        if (carrier == null || row == null) {
            log.debug("gateway_knowledge_chunk carrier or row is absent; nothing applied");
            return;
        }
        MAPPING.applyBusiness(carrier, row);
    }
}

/**
 * 中文说明：{@code KnowledgeChunkMapping} 是 gateway_knowledge_chunk 的 MapStruct 结构映射契约，逐列声明读写形态：
 * 三个 bigint 标识在 {@code Long} 与十进制 {@code String} 之间换算，{@code embedding} 向量在两个方向上都由 {@code clone}
 * 复制以免调用方与持久行共享可变数组，{@code metadata} 以 {@code JsonNode} 直传（不做 JSON 往返、不引入第二套元数据
 * 载体）；{@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态，受保护技术列一律 {@code ignore}。
 * English summary: {@code KnowledgeChunkMapping} is the MapStruct structural contract for gateway_knowledge_chunk, stating every
 * column shape: the three bigint identifiers convert between {@code Long} and decimal {@code String}, the {@code embedding}
 * vector is copied by {@code clone} in both directions so a caller and the persisted row never share a mutable array, and
 * {@code metadata} crosses over as {@code JsonNode} without a JSON round trip or a second metadata carrier;
 * {@code unmappedTargetPolicy=ERROR} forces every new column to be declared while the protected technical columns stay
 * ignored.
 *
 * 用法 / Usage: 由 {@link KnowledgeChunkPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link KnowledgeChunkPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a
 * Spring bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface KnowledgeChunkMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code KnowledgeChunkBO} 的字段顺序投影 gateway_knowledge_chunk 行。
     * English summary: Executes the toBusiness operation, projecting a gateway_knowledge_chunk row onto {@code KnowledgeChunkBO}.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeChunkPersistenceConverter#toBusiness(KnowledgeChunkPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( text( source.getKbId() ) )")
    @Mapping(target = "revisionId", expression = "java( text( source.getRevisionId() ) )")
    @Mapping(target = "chunkIndex", source = "chunkIndex")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "metadata", source = "metadata")
    @Mapping(target = "contentHash", source = "contentHash")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "embedding", expression = "java( vector( source.getEmbedding() ) )")
    @Mapping(target = "revision", expression = "java( value( source.getRevision() ) )")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    KnowledgeChunkBO toBusiness(KnowledgeChunkPO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回 gateway_knowledge_chunk 行；主键与两个父级标识按十进制文本解析，
     * 向量再次复制，租户、审计、软删与版本列全部忽略。
     * English summary: Executes the toRow operation, writing the carrier back onto a gateway_knowledge_chunk row; the key and the
     * two parent identifiers parse from their decimal text, the vector is copied again and the tenant, audit, soft-delete and
     * version columns stay ignored.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeChunkPersistenceConverter#toPersistence(KnowledgeChunkBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( identifier( source.getKbId() ) )")
    @Mapping(target = "revisionId", expression = "java( identifier( source.getRevisionId() ) )")
    @Mapping(target = "chunkIndex", source = "chunkIndex")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "metadata", source = "metadata")
    @Mapping(target = "contentHash", source = "contentHash")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "embedding", expression = "java( vector( source.getEmbedding() ) )")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    KnowledgeChunkPO toRow(KnowledgeChunkBO source);

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载行上覆盖可写业务列；两个父级定位键、主键、只由 CAS 语句推进的
     * 业务 {@code revision} 与受保护元数据保持不变。
     * English summary: Executes the applyBusiness operation, replacing the writable business columns of a loaded row; the two
     * parent locators, the identifier, the business {@code revision} advanced only by the CAS statement and the protected
     * metadata stay untouched.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeChunkPersistenceConverter#applyBusiness(KnowledgeChunkBO, KnowledgeChunkPO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @param target 参数 已加载行模型；parameter the loaded row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "chunkIndex", source = "chunkIndex")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "metadata", source = "metadata")
    @Mapping(target = "contentHash", source = "contentHash")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "embedding", expression = "java( vector( source.getEmbedding() ) )")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kbId", ignore = true)
    @Mapping(target = "revisionId", ignore = true)
    @Mapping(target = "revision", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyBusiness(KnowledgeChunkBO source,
                       @MappingTarget KnowledgeChunkPO target);

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 标识按十进制文本投影，{@code null} 保持 {@code null}。
     * English summary: Executes the text operation, projecting an MP bigint identifier as decimal text; {@code null} stays
     * {@code null}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 标识；parameter identifier。
     * @return 返回 十进制文本；returns the decimal text.
     */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /**
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白视为未生成，交由 {@code ASSIGN_ID} 补位。
     * English summary: Executes the identifier operation, parsing a business decimal identifier into the MP bigint; a blank value
     * is treated as not yet generated so {@code ASSIGN_ID} can fill it, while a malformed value is rejected.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 业务标识；parameter business identifier。
     * @return 返回 主键；returns the identifier.
     */
    default Long identifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "gateway_knowledge_chunk identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 value 操作，把可空 bigint 业务 revision 投影为 long；列缺失即 0。
     * English summary: Executes the value operation, projecting the nullable bigint business revision onto the long carrier; an
     * absent column reads as zero.
     *
     * {@code @Named} 保证本方法只由 revision 的显式表达式调用，不被 MapStruct 当成通用 {@code Long} 换算自动挑中。
     * English summary: {@code @Named} keeps this method reachable only from the explicit revision expression so
     * MapStruct never picks it up as a generic {@code Long} converter.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 业务 revision；returns the business revision.
     */
    @Named("revisionValue")
    default long value(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 中文说明：执行 vector 操作，按 {@code clone} 在两个方向上复制向量数组，缺失保持缺失；这是防御性复制，
     * 不校验长度、不补齐维度、也不把任何分量写进日志，长度与有限性由 Spec 在摄取与检索两侧失败关闭。
     * English summary: Executes the vector operation, copying the vector by {@code clone} in both directions and keeping an absent
     * value absent; this is a defensive copy that neither validates the length, nor pads the dimensions, nor logs a single
     * component, because length and finiteness fail closed on the ingestion and retrieval sides per the Spec.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 向量；parameter the embedding vector.
     * @return 返回 复制后的向量或 {@code null}；returns the cloned vector or {@code null}.
     */
    default float[] vector(float[] value) {
        return value == null ? null : value.clone();
    }
}
