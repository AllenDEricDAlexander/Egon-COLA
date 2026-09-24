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
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeJobBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStageEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeJobPO;

/**
 * 中文说明：{@code KnowledgeJobPersistenceConverter} 是 {@code gateway_knowledge_job} 在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link KnowledgeJobPO} 行模型与 {@link KnowledgeJobBO} 业务载体之间的双向映射：主键、父知识库与
 * {@code retry_of_job_id} 三个 bigint 标识以十进制文本往返，{@code type}/{@code status}/{@code stage} 三个封闭列在
 * {@code String} 与 {@link KnowledgeJobTypeEnum}/{@link KnowledgeJobStatusEnum}/{@link KnowledgeJobStageEnum} 之间按
 * {@code fromWire}/{@code wireValue} 失败关闭地往返（绝不写 ordinal），{@code payload} 与 {@code result} 两个 jsonb 列保持
 * {@code JsonNode} 形态直传，{@code next_attempt_at}/{@code lease_expires_at} 以 {@code Instant} 往返，
 * {@code createdAt}/{@code updatedAt} 只读地投影自 MP 审计列；租户、操作者、软删与技术版本列一律不由业务载体写入。
 * English summary: {@code KnowledgeJobPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_knowledge_job}, mapping {@link KnowledgeJobPO} rows and {@link KnowledgeJobBO} carriers both ways: the primary
 * key, the parent knowledge base and {@code retry_of_job_id} round-trip as decimal text, the three closed columns
 * {@code type}/{@code status}/{@code stage} round-trip between {@code String} and
 * {@link KnowledgeJobTypeEnum}/{@link KnowledgeJobStatusEnum}/{@link KnowledgeJobStageEnum} fail-closed through
 * {@code fromWire}/{@code wireValue} (never as ordinals), the two jsonb columns {@code payload} and {@code result} cross over as
 * {@code JsonNode} without a second round trip, {@code next_attempt_at} and {@code lease_expires_at} cross over as
 * {@code Instant}, the audit instants are read-only projections of the MP audit columns, and tenant, operator, soft-delete and
 * technical version columns are never written from the business carrier.
 *
 * 用法 / Usage: 由 {@code MpKnowledgeRepository} 与 {@code KnowledgeJobServiceImpl} 注入使用
 * （bean 名 {@code knowledgeJobPersistenceConverter}）；{@code newRow} 产出只带业务列的待插入行，入队与重试后继行都经它落库；
 * {@code applyBusiness} 只覆盖执行状态列（{@code status}/{@code stage}/{@code attempt}/{@code nextAttemptAt}/
 * {@code errorCode}/{@code result}），既有的意图身份（父知识库、类型、资源、actor、幂等键、请求 hash、冻结 payload、
 * 重试血缘）、三个租约列、业务 {@code revision} 与受保护元数据一律忽略——租约读写只能走 {@code claimJob}/
 * {@code heartbeatJob}/{@code finishJob} 的 lease-token CAS，行模型禁止进入端口或服务签名，日志只记表名与稳定标识。
 * Injected by the guarded job store and the job service under the bean name {@code knowledgeJobPersistenceConverter};
 * {@code newRow} yields a business-columns-only insert candidate used by enqueueing and by retry successor rows, while
 * {@code applyBusiness} overwrites only the execution-state columns ({@code status}/{@code stage}/{@code attempt}/
 * {@code nextAttemptAt}/{@code errorCode}/{@code result}) and ignores the established intent identity (parent knowledge base,
 * type, resource, actor, idempotency key, request hash, frozen payload, retry lineage), the three lease columns, the business
 * {@code revision} and the protected metadata—lease reads and writes go exclusively through the lease-token CAS statements
 * {@code claimJob}/{@code heartbeatJob}/{@code finishJob}. A row model may never reach a port or a service signature and logging
 * stays on the table name and stable identifiers.
 */
@Slf4j
@Component("knowledgeJobPersistenceConverter")
public class KnowledgeJobPersistenceConverter implements BaseConverter<
        KnowledgeJobPO,
        KnowledgeJobBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final KnowledgeJobMapping MAPPING =
            Mappers.getMapper(KnowledgeJobMapping.class);

    /**
     * 中文说明：执行 toBusiness 操作，把任务行模型投影为业务载体，三个封闭列解码为枚举、两个 jsonb 列保持结构化节点。
     * English summary: Executes the toBusiness operation, projecting a job row onto the business carrier, decoding the three
     * closed columns into enums and keeping the two jsonb columns as structured nodes.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeJobPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public KnowledgeJobBO toBusiness(KnowledgeJobPO row) {
        if (row == null) {
            log.debug("gateway_knowledge_job row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistence 操作，把业务载体渲染为 MP 行模型，枚举写回封闭词汇、节点写回 jsonb 列。
     * English summary: Executes the toPersistence operation, rendering a job carrier into the MyBatis-Plus row model, writing the
     * enums back as the closed vocabularies and the nodes back as jsonb columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeJobPersistenceConverter.toPersistence(jobBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public KnowledgeJobPO toPersistence(KnowledgeJobBO carrier) {
        if (carrier == null) {
            log.debug("gateway_knowledge_job business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影任务行；null 或空输入返回空列表而非 {@code null}，
     * 领取（claim）批次为空即如实为空而不是伪造任务。
     * English summary: Executes the toBusinessList operation, projecting every job row in order; null or empty input yields an
     * empty list and never {@code null}, so an empty claim stays truthfully empty instead of fabricating work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeJobPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<KnowledgeJobBO> toBusinessList(List<KnowledgeJobPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染任务载体；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toPersistenceList operation, rendering every job carrier in order; null or empty input yields
     * an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeJobPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<KnowledgeJobPO> toPersistenceList(List<KnowledgeJobBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeJobPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public KnowledgeJobBO toTarget(KnowledgeJobPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeJobPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public KnowledgeJobPO toSource(KnowledgeJobBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」：
     * 意图身份（父知识库、类型、资源、actor、幂等键、请求 hash、冻结 payload）与初始执行状态一次写入，
     * 尚未领取时三个租约列保持载体给定的缺失或零值，租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only: the intent identity (parent knowledge base, type, resource, actor, idempotency key, request hash, frozen
     * payload) and the initial execution state are written at once, the three lease columns keep whatever absence or zero the
     * carrier declares while nothing is claimed, and the tenant, audit, soft-delete and version columns stay empty for the
     * guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeJobPersistenceConverter.newRow(jobBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public KnowledgeJobPO newRow(KnowledgeJobBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载的活跃行上只覆盖执行状态列；意图身份、重试血缘、三个租约列、
     * 定位主键、父知识库与受保护元数据一律保持不变，业务 {@code revision} 只由 CAS 语句推进。
     * English summary: Executes the applyBusiness operation, overwriting only the execution-state columns of a loaded active row;
     * the intent identity, the retry lineage, the three lease columns, the locating identifier, the parent knowledge base and
     * the protected metadata stay untouched, and the business {@code revision} is advanced only by the CAS statement.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeJobPersistenceConverter.applyBusiness(jobBO, row)}。
     * 任一入参为 {@code null} 时如实不处理。/ Either argument being {@code null} is a no-op.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @param row 参数 已加载行模型；parameter the loaded row model.
     */
    public void applyBusiness(KnowledgeJobBO carrier, KnowledgeJobPO row) {
        if (carrier == null || row == null) {
            log.debug("gateway_knowledge_job carrier or row is absent; nothing applied");
            return;
        }
        MAPPING.applyBusiness(carrier, row);
    }
}

/**
 * 中文说明：{@code KnowledgeJobMapping} 是 gateway_knowledge_job 的 MapStruct 结构映射契约，逐列声明读写形态：
 * 三个 bigint 标识在 {@code Long} 与十进制 {@code String} 之间换算，{@code type}/{@code status}/{@code stage} 只承认
 * 各自的封闭 wire 词汇（未知值按 {@code fromWire} 失败关闭，绝不回落为 ordinal 或默认值），{@code payload} 与
 * {@code result} 以 {@code JsonNode} 直传（不做 JSON 往返、不引入第二套结果载体），租约三列原样搬运但只由
 * lease-token CAS 语句真正推进；{@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code KnowledgeJobMapping} is the MapStruct structural contract for gateway_knowledge_job, stating every
 * column shape: the three bigint identifiers convert between {@code Long} and decimal {@code String},
 * {@code type}/{@code status}/{@code stage} accept only their own closed wire vocabularies (an unknown value fails closed through
 * {@code fromWire} and never falls back to an ordinal or a default), {@code payload} and {@code result} cross over as
 * {@code JsonNode} without a JSON round trip or a second result carrier, and the three lease columns travel as they are while
 * only the lease-token CAS statements advance them; {@code unmappedTargetPolicy=ERROR} forces every new column to be declared
 * here.
 *
 * 用法 / Usage: 由 {@link KnowledgeJobPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link KnowledgeJobPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a
 * Spring bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface KnowledgeJobMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code KnowledgeJobBO} 的字段顺序投影 gateway_knowledge_job 行。
     * English summary: Executes the toBusiness operation, projecting a gateway_knowledge_job row onto {@code KnowledgeJobBO}.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeJobPersistenceConverter#toBusiness(KnowledgeJobPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( text( source.getKbId() ) )")
    @Mapping(target = "type", expression = "java( type( source.getType() ) )")
    @Mapping(target = "resourceId", source = "resourceId")
    @Mapping(target = "actorId", source = "actorId")
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    @Mapping(target = "requestHash", source = "requestHash")
    @Mapping(target = "status", expression = "java( status( source.getStatus() ) )")
    @Mapping(target = "stage", expression = "java( stage( source.getStage() ) )")
    @Mapping(target = "attempt", source = "attempt")
    @Mapping(target = "nextAttemptAt", source = "nextAttemptAt")
    @Mapping(target = "leaseOwner", source = "leaseOwner")
    @Mapping(target = "leaseToken", source = "leaseToken")
    @Mapping(target = "leaseExpiresAt", source = "leaseExpiresAt")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "result", source = "result")
    @Mapping(target = "retryOfJobId", expression = "java( text( source.getRetryOfJobId() ) )")
    @Mapping(target = "revision", expression = "java( value( source.getRevision() ) )")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    KnowledgeJobBO toBusiness(KnowledgeJobPO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回 gateway_knowledge_job 行；主键与两个 bigint 引用按十进制文本解析，
     * 三个封闭列写回 wire 词汇，租户、审计、软删与版本列全部忽略。
     * English summary: Executes the toRow operation, writing the carrier back onto a gateway_knowledge_job row; the key and the two
     * bigint references parse from their decimal text, the three closed columns write back as wire vocabularies and the tenant,
     * audit, soft-delete and version columns stay ignored.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeJobPersistenceConverter#toPersistence(KnowledgeJobBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "kbId", expression = "java( identifier( source.getKbId() ) )")
    @Mapping(target = "type", expression = "java( wire( source.getType() ) )")
    @Mapping(target = "resourceId", source = "resourceId")
    @Mapping(target = "actorId", source = "actorId")
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    @Mapping(target = "requestHash", source = "requestHash")
    @Mapping(target = "status", expression = "java( wire( source.getStatus() ) )")
    @Mapping(target = "stage", expression = "java( wire( source.getStage() ) )")
    @Mapping(target = "attempt", source = "attempt")
    @Mapping(target = "nextAttemptAt", source = "nextAttemptAt")
    @Mapping(target = "leaseOwner", source = "leaseOwner")
    @Mapping(target = "leaseToken", source = "leaseToken")
    @Mapping(target = "leaseExpiresAt", source = "leaseExpiresAt")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "result", source = "result")
    @Mapping(target = "retryOfJobId", expression = "java( identifier( source.getRetryOfJobId() ) )")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    KnowledgeJobPO toRow(KnowledgeJobBO source);

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载行上只覆盖执行状态列；意图身份与重试血缘一旦入队即不可改写，
     * 否则幂等复用与 stale 判定都会失真；三个租约列只能由 {@code claimJob}/{@code heartbeatJob}/{@code finishJob}
     * 的 lease-token CAS 推进，整行覆盖会窃取或丢弃在跑的租约，故与主键、父知识库、只由 CAS 推进的业务
     * {@code revision} 和受保护元数据一起忽略。
     * English summary: Executes the applyBusiness operation, overwriting only the execution-state columns of a loaded row; the
     * intent identity and the retry lineage become immutable once queued because otherwise idempotency reuse and staleness
     * detection would both drift; the three lease columns advance exclusively through the lease-token CAS statements
     * {@code claimJob}/{@code heartbeatJob}/{@code finishJob}, and a whole-row overwrite would steal or drop a live lease, so they
     * stay ignored together with the identifier, the parent knowledge base, the CAS-advanced business {@code revision} and the
     * protected metadata.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeJobPersistenceConverter#applyBusiness(KnowledgeJobBO, KnowledgeJobPO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @param target 参数 已加载行模型；parameter the loaded row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "status", expression = "java( wire( source.getStatus() ) )")
    @Mapping(target = "stage", expression = "java( wire( source.getStage() ) )")
    @Mapping(target = "attempt", source = "attempt")
    @Mapping(target = "nextAttemptAt", source = "nextAttemptAt")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "result", source = "result")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kbId", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "resourceId", ignore = true)
    @Mapping(target = "actorId", ignore = true)
    @Mapping(target = "payload", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    @Mapping(target = "requestHash", ignore = true)
    @Mapping(target = "leaseOwner", ignore = true)
    @Mapping(target = "leaseToken", ignore = true)
    @Mapping(target = "leaseExpiresAt", ignore = true)
    @Mapping(target = "retryOfJobId", ignore = true)
    @Mapping(target = "revision", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyBusiness(KnowledgeJobBO source,
                       @MappingTarget KnowledgeJobPO target);

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
                    "gateway_knowledge_job identifier must be decimal: " + value,
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
     * 中文说明：执行 type 操作，把 {@code type} 封闭文本列解码为任务类型枚举，未知词汇如实抛出；
     * 类型路由只由 {@code KnowledgeJobStrategy} 注册表负责，本映射不做任何 switch。
     * English summary: Executes the type operation, decoding the closed {@code type} column into the job type enum and surfacing an
     * unknown vocabulary truthfully; routing stays with the {@code KnowledgeJobStrategy} registry, so this mapping holds no
     * switch.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 类型枚举或 {@code null}；returns the type enum or {@code null}.
     */
    default KnowledgeJobTypeEnum type(String value) {
        return value == null ? null : KnowledgeJobTypeEnum.fromWire(value);
    }

    /**
     * 中文说明：执行 status 操作，把 {@code status} 封闭文本列解码为任务状态枚举，未知词汇如实抛出。
     * English summary: Executes the status operation, decoding the closed {@code status} column into the job status enum and
     * surfacing an unknown vocabulary truthfully.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 状态枚举或 {@code null}；returns the status enum or {@code null}.
     */
    default KnowledgeJobStatusEnum status(String value) {
        return value == null ? null : KnowledgeJobStatusEnum.fromWire(value);
    }

    /**
     * 中文说明：执行 stage 操作，把 {@code stage} 封闭文本列解码为任务阶段枚举，未知词汇如实抛出。
     * English summary: Executes the stage operation, decoding the closed {@code stage} column into the job stage enum and
     * surfacing an unknown vocabulary truthfully.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 阶段枚举或 {@code null}；returns the stage enum or {@code null}.
     */
    default KnowledgeJobStageEnum stage(String value) {
        return value == null ? null : KnowledgeJobStageEnum.fromWire(value);
    }

    /**
     * 中文说明：执行 wire 操作，把三个封闭枚举之一按其 {@code @JsonValue} wire 字符串写回对应列，绝不写 ordinal。
     * English summary: Executes the wire operation, writing one of the three closed enums back into its column as the
     * {@code @JsonValue} wire string and never as an ordinal.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 类型枚举；parameter the type enum.
     * @return 返回 wire 字符串；returns the wire string.
     */
    default String wire(KnowledgeJobTypeEnum value) {
        return value == null ? null : value.wireValue();
    }

    /**
     * 中文说明：执行 wire 操作（状态重载），把任务状态按其 {@code @JsonValue} wire 字符串写回封闭列。
     * English summary: Executes the wire operation (status overload), writing the job status back into the closed column as its
     * {@code @JsonValue} wire string.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 状态枚举；parameter the status enum.
     * @return 返回 wire 字符串；returns the wire string.
     */
    default String wire(KnowledgeJobStatusEnum value) {
        return value == null ? null : value.wireValue();
    }

    /**
     * 中文说明：执行 wire 操作（阶段重载），把任务阶段按其 {@code @JsonValue} wire 字符串写回封闭列。
     * English summary: Executes the wire operation (stage overload), writing the job stage back into the closed column as its
     * {@code @JsonValue} wire string.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 阶段枚举；parameter the stage enum.
     * @return 返回 wire 字符串；returns the wire string.
     */
    default String wire(KnowledgeJobStageEnum value) {
        return value == null ? null : value.wireValue();
    }
}
