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
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentRevisionBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeRevisionStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeDocumentRevisionPO;

/**
 * 中文说明：{@code KnowledgeDocumentRevisionPersistenceConverter} 是 {@code gateway_knowledge_revision} 在持久边界唯一的
 * MapStruct 转换器，负责 {@link KnowledgeDocumentRevisionPO} 行模型与 {@link KnowledgeDocumentRevisionBO} 业务载体之间的
 * 双向映射：主键与父知识库、父资料三个 bigint 标识以十进制文本往返，{@code raw_bytes} 字节数组在两个方向上都按深度
 * 复制（{@code clone}）而非共享引用，使调用方持有的原始上传字节与载体内部状态互不影响；{@code chunking_config} 保持
 * {@code JsonNode} 结构化形态直传，不再做第二层 JSON 往返；{@code status} 封闭列在 {@code String} 与
 * {@link KnowledgeRevisionStatusEnum} 之间按 {@code fromWire}/{@code wireValue} 失败关闭地往返；
 * {@code createdAt}/{@code updatedAt} 只读地投影自 MP 审计列，租户、操作者、软删与技术版本列一律不由业务载体写入。
 * English summary: {@code KnowledgeDocumentRevisionPersistenceConverter} is the only MapStruct converter at the persistence
 * boundary of {@code gateway_knowledge_revision}, mapping {@link KnowledgeDocumentRevisionPO} rows and
 * {@link KnowledgeDocumentRevisionBO} carriers both ways: the primary key and the two parent bigint identifiers round-trip as
 * decimal text, the {@code raw_bytes} array is deep-copied by {@code clone} in both directions so a caller's uploaded bytes and
 * the carrier's internal state never share a reference, {@code chunking_config} keeps its structured {@code JsonNode} shape
 * without a second JSON round trip, the closed {@code status} column round-trips between {@code String} and
 * {@link KnowledgeRevisionStatusEnum} fail-closed through {@code fromWire}/{@code wireValue}, the audit instants are read-only
 * projections of the MP audit columns, and tenant, operator, soft-delete and technical version columns are never written from
 * the business carrier.
 *
 * 用法 / Usage: 由 {@code MpKnowledgeRepository} 注入使用（bean 名 {@code knowledgeDocumentRevisionPersistenceConverter}），
 * 上传事务与摄取任务都经由它读写冻结版本；{@code newRow} 产出只带业务列的待插入行（原始字节在此一次写入，
 * 之后不再由任何 API 暴露），{@code applyBusiness} 在已加载活跃行上整行覆盖解析结果与状态列，保留两个父级定位键、
 * 主键与受保护元数据，并且从不写入业务 {@code revision}——该列只能由仓储的乐观锁 CAS 语句推进。
 * 行模型与原始字节都禁止进入端口、服务签名或任何出站投影。
 * Injected by the guarded knowledge store under the bean name {@code knowledgeDocumentRevisionPersistenceConverter} and used by
 * both the upload transaction and the ingestion job; {@code newRow} yields a business-columns-only insert candidate (the raw
 * bytes are written once here and never exposed by any API afterwards), while {@code applyBusiness} replaces the extraction and
 * status columns of a loaded active row, keeping the two parent locators, the identifier and the protected metadata, and never
 * writing the business {@code revision} that only the repository CAS statement advances. Neither the row model nor the raw
 * bytes may reach a port, a service signature or any outbound projection.
 */
@Slf4j
@Component("knowledgeDocumentRevisionPersistenceConverter")
public class KnowledgeDocumentRevisionPersistenceConverter implements BaseConverter<
        KnowledgeDocumentRevisionPO,
        KnowledgeDocumentRevisionBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final KnowledgeDocumentRevisionMapping MAPPING =
            Mappers.getMapper(KnowledgeDocumentRevisionMapping.class);

    /**
     * 中文说明：执行 toBusiness 操作，把冻结版本行投影为业务载体，原始字节按 {@code clone} 复制而非共享引用。
     * English summary: Executes the toBusiness operation, projecting a frozen revision row onto the business carrier and copying
     * the raw bytes by {@code clone} instead of sharing a reference.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentRevisionPersistenceConverter.toBusiness(row)}。
     * 传入 {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public KnowledgeDocumentRevisionBO toBusiness(KnowledgeDocumentRevisionPO row) {
        if (row == null) {
            log.debug("gateway_knowledge_revision row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistence 操作，把业务载体渲染为 MP 行模型，原始字节再次按 {@code clone} 复制。
     * English summary: Executes the toPersistence operation, rendering a revision carrier into the MyBatis-Plus row model and
     * copying the raw bytes by {@code clone} again.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentRevisionPersistenceConverter.toPersistence(revisionBO)}。
     * 传入 {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public KnowledgeDocumentRevisionPO toPersistence(KnowledgeDocumentRevisionBO carrier) {
        if (carrier == null) {
            log.debug("gateway_knowledge_revision business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影冻结版本行；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toBusinessList operation, projecting every frozen revision row in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentRevisionPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<KnowledgeDocumentRevisionBO> toBusinessList(List<KnowledgeDocumentRevisionPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染冻结版本载体；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toPersistenceList operation, rendering every revision carrier in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentRevisionPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<KnowledgeDocumentRevisionPO> toPersistenceList(List<KnowledgeDocumentRevisionBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentRevisionPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public KnowledgeDocumentRevisionBO toTarget(KnowledgeDocumentRevisionPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentRevisionPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public KnowledgeDocumentRevisionPO toSource(KnowledgeDocumentRevisionBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」：
     * 上传事务在此一次性写入原始字节、内容 hash 与冻结的分块配置，租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only: the upload transaction writes the raw bytes, the content hash and the frozen chunking configuration exactly
     * here, while the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentRevisionPersistenceConverter.newRow(revisionBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public KnowledgeDocumentRevisionPO newRow(KnowledgeDocumentRevisionBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载的活跃行上整行覆盖可写业务列（解析文本、状态、分块数与
     * 冻结的向量空间列都在其中，原始字节按 {@code clone} 复制），保留两个父级定位键、主键与受保护元数据；
     * 业务 {@code revision} 只由 CAS 语句推进，故此处同样忽略。
     * English summary: Executes the applyBusiness operation, overwrite-replacing the writable business columns of a loaded active
     * row (the extracted text, status, chunk count and the frozen embedding-space columns included, with the raw bytes copied
     * by {@code clone}) while keeping the two parent locators, the identifier and the protected metadata; the business
     * {@code revision} is advanced only by the CAS statement and is therefore ignored here too.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentRevisionPersistenceConverter.applyBusiness(revisionBO, row)}。
     * 任一入参为 {@code null} 时如实不处理。/ Either argument being {@code null} is a no-op.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @param row 参数 已加载行模型；parameter the loaded row model.
     */
    public void applyBusiness(KnowledgeDocumentRevisionBO carrier, KnowledgeDocumentRevisionPO row) {
        if (carrier == null || row == null) {
            log.debug("gateway_knowledge_revision carrier or row is absent; nothing applied");
            return;
        }
        MAPPING.applyBusiness(carrier, row);
    }
}

/**
 * 中文说明：{@code KnowledgeDocumentRevisionMapping} 是 gateway_knowledge_revision 的 MapStruct 结构映射契约，逐列声明
 * 读写形态：三个 bigint 标识在 {@code Long} 与十进制 {@code String} 之间换算，{@code raw_bytes} 在两个方向上都由
 * {@code clone} 复制以避免调用方与持久行共享可变数组，{@code chunking_config} 保持 {@code JsonNode} 直传（不做 JSON
 * 往返也不引入第二套配置载体），{@code status} 只承认封闭 wire 词汇；{@code unmappedTargetPolicy=ERROR}
 * 保证任何新增列都必须在此显式表态。
 * English summary: {@code KnowledgeDocumentRevisionMapping} is the MapStruct structural contract for
 * gateway_knowledge_revision, stating every column shape: the three bigint identifiers convert between {@code Long} and
 * decimal {@code String}, {@code raw_bytes} is copied by {@code clone} in both directions so a caller and the persisted row never
 * share a mutable array, {@code chunking_config} crosses over as {@code JsonNode} without a JSON round trip or a second
 * configuration carrier, and {@code status} accepts only the closed wire vocabulary; {@code unmappedTargetPolicy=ERROR} forces
 * every new column to be declared here.
 *
 * 用法 / Usage: 由 {@link KnowledgeDocumentRevisionPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，
 * 也不登记为 Spring bean。
 * Held by {@link KnowledgeDocumentRevisionPersistenceConverter} through {@code Mappers}; neither published to callers nor
 * registered as a Spring bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface KnowledgeDocumentRevisionMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code KnowledgeDocumentRevisionBO} 的字段顺序投影 gateway_knowledge_revision 行。
     * English summary: Executes the toBusiness operation, projecting a gateway_knowledge_revision row onto
     * {@code KnowledgeDocumentRevisionBO}.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeDocumentRevisionPersistenceConverter#toBusiness(KnowledgeDocumentRevisionPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( text( source.getKbId() ) )")
    @Mapping(target = "documentId", expression = "java( text( source.getDocumentId() ) )")
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "mediaType", source = "mediaType")
    @Mapping(target = "rawBytes", expression = "java( bytes( source.getRawBytes() ) )")
    @Mapping(target = "byteCount", source = "byteCount")
    @Mapping(target = "contentHash", source = "contentHash")
    @Mapping(target = "extractedText", source = "extractedText")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "chunkingConfig", source = "chunkingConfig")
    @Mapping(target = "status", expression = "java( status( source.getStatus() ) )")
    @Mapping(target = "chunkCount", source = "chunkCount")
    @Mapping(target = "revision", expression = "java( value( source.getRevision() ) )")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    KnowledgeDocumentRevisionBO toBusiness(KnowledgeDocumentRevisionPO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回 gateway_knowledge_revision 行；主键与两个父级标识按十进制文本解析，
     * 原始字节再次复制，状态写回封闭词汇，租户、审计、软删与版本列全部忽略。
     * English summary: Executes the toRow operation, writing the carrier back onto a gateway_knowledge_revision row; the key and
     * the two parent identifiers parse from their decimal text, the raw bytes are copied again, the status writes back as the
     * closed vocabulary and the tenant, audit, soft-delete and version columns stay ignored.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeDocumentRevisionPersistenceConverter#toPersistence(KnowledgeDocumentRevisionBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( identifier( source.getKbId() ) )")
    @Mapping(target = "documentId", expression = "java( identifier( source.getDocumentId() ) )")
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "mediaType", source = "mediaType")
    @Mapping(target = "rawBytes", expression = "java( bytes( source.getRawBytes() ) )")
    @Mapping(target = "byteCount", source = "byteCount")
    @Mapping(target = "contentHash", source = "contentHash")
    @Mapping(target = "extractedText", source = "extractedText")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "chunkingConfig", source = "chunkingConfig")
    @Mapping(target = "status", expression = "java( wire( source.getStatus() ) )")
    @Mapping(target = "chunkCount", source = "chunkCount")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    KnowledgeDocumentRevisionPO toRow(KnowledgeDocumentRevisionBO source);

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载行上覆盖可写业务列；两个父级定位键、主键、只由 CAS 语句推进的
     * 业务 {@code revision} 与受保护元数据保持不变。
     * English summary: Executes the applyBusiness operation, replacing the writable business columns of a loaded row; the two
     * parent locators, the identifier, the business {@code revision} advanced only by the CAS statement and the protected
     * metadata stay untouched.
     *
     * 用法 / Usage: 仅由
     * {@link KnowledgeDocumentRevisionPersistenceConverter#applyBusiness(KnowledgeDocumentRevisionBO, KnowledgeDocumentRevisionPO)}
     * 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @param target 参数 已加载行模型；parameter the loaded row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "mediaType", source = "mediaType")
    @Mapping(target = "rawBytes", expression = "java( bytes( source.getRawBytes() ) )")
    @Mapping(target = "byteCount", source = "byteCount")
    @Mapping(target = "contentHash", source = "contentHash")
    @Mapping(target = "extractedText", source = "extractedText")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "chunkingConfig", source = "chunkingConfig")
    @Mapping(target = "status", expression = "java( wire( source.getStatus() ) )")
    @Mapping(target = "chunkCount", source = "chunkCount")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kbId", ignore = true)
    @Mapping(target = "documentId", ignore = true)
    @Mapping(target = "revision", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyBusiness(KnowledgeDocumentRevisionBO source,
                       @MappingTarget KnowledgeDocumentRevisionPO target);

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
                    "gateway_knowledge_revision identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 value 操作，把可空 bigint 业务 revision 投影为 long；列缺失即 0。方法带 {@code @Named}，
     * 只由 revision 的显式表达式调用，绝不被 MapStruct 当成通用 {@code Long} 换算方法自动挑中——
     * 否则 {@code byteCount} 这类可空 bigint 列的 {@code null} 会被伪造成 0。
     * English summary: Executes the value operation, projecting the nullable bigint business revision onto the long carrier; an
     * absent column reads as zero. The method carries {@code @Named} so only the explicit revision expression calls it and
     * MapStruct never picks it up as a generic {@code Long} converter—otherwise the {@code null} of a nullable bigint such as
     * {@code byteCount} would be fabricated into a zero.
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
     * 中文说明：执行 bytes 操作，按 {@code clone} 在两个方向上复制原始字节，缺失保持缺失；这是防御性复制，
     * 绝不做 JSON 往返，也不把字节内容写进任何日志。
     * English summary: Executes the bytes operation, copying the raw bytes by {@code clone} in both directions and keeping an
     * absent value absent; this is a defensive copy, never a JSON round trip, and the payload is never logged.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 原始字节；parameter the raw bytes.
     * @return 返回 复制后的字节数组或 {@code null}；returns the cloned array or {@code null}.
     */
    default byte[] bytes(byte[] value) {
        return value == null ? null : value.clone();
    }

    /**
     * 中文说明：执行 status 操作，把 {@code status} 封闭文本列解码为冻结版本状态枚举，未知词汇如实抛出。
     * English summary: Executes the status operation, decoding the closed {@code status} column into the frozen revision status
     * enum and surfacing an unknown vocabulary truthfully.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 状态枚举或 {@code null}；returns the status enum or {@code null}.
     */
    default KnowledgeRevisionStatusEnum status(String value) {
        return value == null ? null : KnowledgeRevisionStatusEnum.fromWire(value);
    }

    /**
     * 中文说明：执行 wire 操作，把状态枚举按其 {@code @JsonValue} wire 字符串写回封闭列，绝不写 ordinal。
     * English summary: Executes the wire operation, writing the status enum back into the closed column as its
     * {@code @JsonValue} wire string and never as an ordinal.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 状态枚举；parameter the status enum.
     * @return 返回 wire 字符串；returns the wire string.
     */
    default String wire(KnowledgeRevisionStatusEnum value) {
        return value == null ? null : value.wireValue();
    }
}
