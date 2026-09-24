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
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeDocumentPO;

/**
 * 中文说明：{@code KnowledgeDocumentPersistenceConverter} 是 {@code gateway_knowledge_document} 在持久边界唯一的
 * MapStruct 转换器，负责 {@link KnowledgeDocumentPO} 行模型与 {@link KnowledgeDocumentBO} 业务载体之间的双向映射：
 * 主键与父知识库、当前激活版本、最近任务三个 bigint 指针全部以十进制文本往返（尚未激活或尚无任务时保持
 * {@code null}，绝不伪造 0），业务 {@code revision} 与 MP 乐观锁 {@code version} 互不替代，
 * {@code createdAt}/{@code updatedAt} 只读地投影自 MP 审计列；租户、操作者、软删与技术版本列一律不由业务载体写入。
 * English summary: {@code KnowledgeDocumentPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_knowledge_document}, mapping {@link KnowledgeDocumentPO} rows and {@link KnowledgeDocumentBO} carriers both
 * ways: the primary key and the three bigint pointers to the parent knowledge base, the active revision and the latest job
 * round-trip as decimal text (staying {@code null} while nothing is active or queued and never fabricating a zero), the
 * business {@code revision} stays separate from the optimistic-lock {@code version}, and the audit instants are read-only
 * projections of the MP audit columns; tenant, operator, soft-delete and technical version columns are never written from the
 * business carrier.
 *
 * 用法 / Usage: 由 {@code MpKnowledgeRepository} 注入使用（bean 名 {@code knowledgeDocumentPersistenceConverter}）；
 * {@code newRow} 产出只带业务列的待插入行，{@code applyBusiness} 在已加载活跃行上整行覆盖文件名与两个指针列，
 * 保留定位主键、父知识库 {@code kbId} 与受保护元数据，并且从不写入业务 {@code revision}——该列只能由仓储的
 * 乐观锁 CAS 或 {@code activateRevision} 具名语句推进。行模型禁止进入端口或服务签名。
 * Injected by the guarded knowledge store under the bean name {@code knowledgeDocumentPersistenceConverter}; {@code newRow}
 * yields a business-columns-only insert candidate and {@code applyBusiness} replaces the file name and the two pointer columns
 * of a loaded active row, keeping the locating identifier, the parent {@code kbId} and the protected metadata, while never
 * writing the business {@code revision} that only the repository CAS or the named {@code activateRevision} statement advances.
 * A row model may never reach a port or a service signature.
 */
@Slf4j
@Component("knowledgeDocumentPersistenceConverter")
public class KnowledgeDocumentPersistenceConverter implements BaseConverter<
        KnowledgeDocumentPO,
        KnowledgeDocumentBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final KnowledgeDocumentMapping MAPPING =
            Mappers.getMapper(KnowledgeDocumentMapping.class);

    /**
     * 中文说明：执行 toBusiness 操作，把资料行模型投影为业务载体，三个 bigint 指针转为十进制文本。
     * English summary: Executes the toBusiness operation, projecting a document row onto the business carrier and turning the
     * three bigint pointers into decimal text.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public KnowledgeDocumentBO toBusiness(KnowledgeDocumentPO row) {
        if (row == null) {
            log.debug("gateway_knowledge_document row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistence 操作，把业务载体渲染为 MP 行模型，十进制文本指针解析回 bigint 列。
     * English summary: Executes the toPersistence operation, rendering a document carrier into the MyBatis-Plus row model and
     * parsing the decimal-text pointers back into bigint columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentPersistenceConverter.toPersistence(documentBO)}。
     * 传入 {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public KnowledgeDocumentPO toPersistence(KnowledgeDocumentBO carrier) {
        if (carrier == null) {
            log.debug("gateway_knowledge_document business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影资料行；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toBusinessList operation, projecting every document row in order; null or empty input yields
     * an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<KnowledgeDocumentBO> toBusinessList(List<KnowledgeDocumentPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染资料载体；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toPersistenceList operation, rendering every document carrier in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<KnowledgeDocumentPO> toPersistenceList(List<KnowledgeDocumentBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public KnowledgeDocumentBO toTarget(KnowledgeDocumentPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public KnowledgeDocumentPO toSource(KnowledgeDocumentBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」：
     * 父知识库、文件名与两个指针取自可信载体，租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only: the parent knowledge base, the file name and the two pointers come from the trusted carrier while the
     * tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentPersistenceConverter.newRow(documentBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public KnowledgeDocumentPO newRow(KnowledgeDocumentBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载的活跃行上整行覆盖可写业务列（文件名与两个指针列），
     * 保留定位主键、父知识库 {@code kbId} 与受保护元数据；业务 {@code revision} 只由 CAS 语句推进，故此处同样忽略。
     * English summary: Executes the applyBusiness operation, overwrite-replacing the writable business columns (the file name and
     * the two pointer columns) of a loaded active row while keeping the locating identifier, the parent {@code kbId} and the
     * protected metadata; the business {@code revision} is advanced only by the CAS statement and is therefore ignored here.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeDocumentPersistenceConverter.applyBusiness(documentBO, row)}。
     * 任一入参为 {@code null} 时如实不处理。/ Either argument being {@code null} is a no-op.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @param row 参数 已加载行模型；parameter the loaded row model.
     */
    public void applyBusiness(KnowledgeDocumentBO carrier, KnowledgeDocumentPO row) {
        if (carrier == null || row == null) {
            log.debug("gateway_knowledge_document carrier or row is absent; nothing applied");
            return;
        }
        MAPPING.applyBusiness(carrier, row);
    }
}

/**
 * 中文说明：{@code KnowledgeDocumentMapping} 是 gateway_knowledge_document 的 MapStruct 结构映射契约，逐列声明读写形态：
 * 四个 bigint 标识在 {@code Long} 与十进制 {@code String} 之间由本契约的默认方法换算，缺失保持缺失；
 * {@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态，受保护技术列一律 {@code ignore}。
 * English summary: {@code KnowledgeDocumentMapping} is the MapStruct structural contract for gateway_knowledge_document,
 * stating every column shape: the four bigint identifiers convert between {@code Long} and decimal {@code String} in the
 * default methods here and an absent value stays absent; {@code unmappedTargetPolicy=ERROR} forces every new column to be
 * declared while the protected technical columns stay ignored.
 *
 * 用法 / Usage: 由 {@link KnowledgeDocumentPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link KnowledgeDocumentPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as
 * a Spring bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface KnowledgeDocumentMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code KnowledgeDocumentBO} 的字段顺序投影 gateway_knowledge_document 行。
     * English summary: Executes the toBusiness operation, projecting a gateway_knowledge_document row onto
     * {@code KnowledgeDocumentBO}.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeDocumentPersistenceConverter#toBusiness(KnowledgeDocumentPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( text( source.getKbId() ) )")
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "activeRevisionId", expression = "java( text( source.getActiveRevisionId() ) )")
    @Mapping(target = "latestJobId", expression = "java( text( source.getLatestJobId() ) )")
    @Mapping(target = "revision", expression = "java( value( source.getRevision() ) )")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    KnowledgeDocumentBO toBusiness(KnowledgeDocumentPO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回 gateway_knowledge_document 行；主键与三个指针按十进制文本解析，
     * 租户、审计、软删与版本列全部忽略。
     * English summary: Executes the toRow operation, writing the carrier back onto a gateway_knowledge_document row; the key and
     * the three pointers parse from their decimal text and the tenant, audit, soft-delete and version columns stay ignored.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeDocumentPersistenceConverter#toPersistence(KnowledgeDocumentBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( identifier( source.getKbId() ) )")
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "activeRevisionId", expression = "java( identifier( source.getActiveRevisionId() ) )")
    @Mapping(target = "latestJobId", expression = "java( identifier( source.getLatestJobId() ) )")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    KnowledgeDocumentPO toRow(KnowledgeDocumentBO source);

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载行上覆盖可写业务列；定位主键、父知识库 {@code kbId}、
     * 只由 CAS 语句推进的业务 {@code revision} 与受保护元数据保持不变。
     * English summary: Executes the applyBusiness operation, replacing the writable business columns of a loaded row; the
     * locating identifier, the parent {@code kbId}, the business {@code revision} advanced only by the CAS statement and the
     * protected metadata stay untouched.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeDocumentPersistenceConverter#applyBusiness(KnowledgeDocumentBO, KnowledgeDocumentPO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @param target 参数 已加载行模型；parameter the loaded row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "activeRevisionId", expression = "java( identifier( source.getActiveRevisionId() ) )")
    @Mapping(target = "latestJobId", expression = "java( identifier( source.getLatestJobId() ) )")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kbId", ignore = true)
    @Mapping(target = "revision", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyBusiness(KnowledgeDocumentBO source,
                       @MappingTarget KnowledgeDocumentPO target);

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
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白视为未生成，交由 {@code ASSIGN_ID} 补位，
     * 形态不符如实抛出而不写 0。
     * English summary: Executes the identifier operation, parsing a business decimal identifier into the MP bigint; a blank value
     * is treated as not yet generated so {@code ASSIGN_ID} can fill it, while a malformed value fails truthfully instead of
     * writing a zero.
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
                    "gateway_knowledge_document identifier must be decimal: " + value,
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
}
