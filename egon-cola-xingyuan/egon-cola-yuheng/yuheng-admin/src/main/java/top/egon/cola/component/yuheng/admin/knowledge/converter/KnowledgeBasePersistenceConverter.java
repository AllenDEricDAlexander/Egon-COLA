package top.egon.cola.component.yuheng.admin.knowledge.converter;

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
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMemberDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeEgressPolicyEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeBasePO;

/**
 * 中文说明：{@code KnowledgeBasePersistenceConverter} 是 {@code gateway_knowledge_base} 在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link KnowledgeBasePO} 行模型与 {@link KnowledgeBaseBO} 业务载体之间的双向映射：不透明主键与 {@code owner_actor_id}
 * 之外的所有 bigint 标识都以十进制文本往返，{@code members} jsonb 列经与 {@code GatewayJsonbTypeHandler} 同源的 Jackson
 * 解码为顶层 {@link KnowledgeMemberDTO} 列表（角色按其 {@code @JsonValue} wire 字符串入列，绝不写 ordinal），
 * {@code egress_policy} 封闭列在 {@code String} 与 {@link KnowledgeEgressPolicyEnum} 之间按 {@code fromWire}/{@code wireValue}
 * 失败关闭地往返，{@code createdAt}/{@code updatedAt} 只读地投影自 MP 审计列 {@code create_time}/{@code update_time}，
 * 业务 {@code revision} 与 MP 乐观锁 {@code version} 互不替代；租户、操作者、软删与技术版本列一律不由业务载体写入。
 * English summary: {@code KnowledgeBasePersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_knowledge_base}, mapping {@link KnowledgeBasePO} rows and {@link KnowledgeBaseBO} carriers both ways: opaque
 * bigint identifiers round-trip as decimal text, the {@code members} jsonb column becomes a list of top-level
 * {@link KnowledgeMemberDTO} records through the same Jackson serializer as {@code GatewayJsonbTypeHandler} (roles enter the
 * list as their {@code @JsonValue} wire strings and never as ordinals), the closed {@code egress_policy} column round-trips
 * between {@code String} and {@link KnowledgeEgressPolicyEnum} fail-closed through {@code fromWire}/{@code wireValue}, the
 * audit instants are read-only projections of {@code create_time} and {@code update_time}, and the business {@code revision}
 * stays separate from the optimistic-lock {@code version}; tenant, operator, soft-delete and technical version columns are
 * never written from the business carrier.
 *
 * 用法 / Usage: 由 {@code MpKnowledgeRepository} 注入使用（bean 名 {@code knowledgeBasePersistenceConverter}），
 * 也是 {@code KnowledgeServiceImpl} 的六个持久转换器之一；{@code newRow} 产出只带业务列的待插入行（租户、审计、软删与
 * 版本列留给受守卫边界补齐），{@code applyBusiness} 在已加载活跃行上整行覆盖可写业务列，保留 {@code id}、
 * 不可转移的 {@code ownerActorId} 与受保护元数据，并且从不写入业务 {@code revision}——该列只能由仓储的
 * 乐观锁 CAS 语句推进。行模型禁止进入端口或服务签名。
 * Injected by the guarded knowledge store under the bean name {@code knowledgeBasePersistenceConverter} and listed among the
 * six persistence converters of the knowledge service; {@code newRow} yields a business-columns-only insert candidate whose
 * tenant, audit, soft-delete and version columns are left to the guarded boundary, while {@code applyBusiness}
 * overwrite-replaces the writable business columns of a loaded active row, keeping the identifier, the non-transferable
 * {@code ownerActorId} and the protected metadata, and never writing the business {@code revision} that only the repository
 * CAS statement advances. A row model may never reach a port or a service signature.
 */
@Slf4j
@Component("knowledgeBasePersistenceConverter")
public class KnowledgeBasePersistenceConverter implements BaseConverter<
        KnowledgeBasePO,
        KnowledgeBaseBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final KnowledgeBaseMapping MAPPING =
            Mappers.getMapper(KnowledgeBaseMapping.class);

    /**
     * 中文说明：执行 toBusiness 操作，把知识库行模型投影为业务载体，{@code members} jsonb 列解码为成员列表、
     * {@code egress_policy} 解码为策略枚举。
     * English summary: Executes the toBusiness operation, projecting a knowledge-base row onto the business carrier while
     * decoding the {@code members} jsonb column into the member list and {@code egress_policy} into the policy enum.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeBasePersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public KnowledgeBaseBO toBusiness(KnowledgeBasePO row) {
        if (row == null) {
            log.debug("gateway_knowledge_base row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistence 操作，把业务载体渲染为 MP 行模型，成员列表与出口策略回写为 jsonb/封闭文本列。
     * English summary: Executes the toPersistence operation, rendering a knowledge-base carrier into the MyBatis-Plus row model
     * and writing the member list and the egress policy back as the jsonb and closed-text columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeBasePersistenceConverter.toPersistence(baseBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public KnowledgeBasePO toPersistence(KnowledgeBaseBO carrier) {
        if (carrier == null) {
            log.debug("gateway_knowledge_base business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影知识库行；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toBusinessList operation, projecting every knowledge-base row in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeBasePersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<KnowledgeBaseBO> toBusinessList(List<KnowledgeBasePO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染知识库载体；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toPersistenceList operation, rendering every knowledge-base carrier in order; null or empty
     * input yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeBasePersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<KnowledgeBasePO> toPersistenceList(List<KnowledgeBaseBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeBasePersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public KnowledgeBaseBO toTarget(KnowledgeBasePO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeBasePersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public KnowledgeBasePO toSource(KnowledgeBaseBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」：
     * 名称、描述、可信 owner、成员、出口策略与冻结的模型/向量空间列全部取自可信载体，租户、审计、软删与版本列
     * 一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only: name, description, the trusted owner, members, egress policy and the frozen model and embedding-space
     * columns come from the trusted carrier while the tenant, audit, soft-delete and version columns stay empty for the
     * guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeBasePersistenceConverter.newRow(baseBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public KnowledgeBasePO newRow(KnowledgeBaseBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载的活跃行上整行覆盖可写业务列（含 {@code members} jsonb 列），
     * 保留定位主键、不可转移的 owner 与受保护元数据；业务 {@code revision} 只由仓储 CAS 语句推进，故此处同样忽略。
     * English summary: Executes the applyBusiness operation, overwrite-replacing the writable business columns (the
     * {@code members} jsonb column included) of a loaded active row while keeping the locating primary key, the
     * non-transferable owner and the protected metadata; the business {@code revision} is advanced only by the repository CAS
     * statement and is therefore ignored here too.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code knowledgeBasePersistenceConverter.applyBusiness(baseBO, row)}。
     * 任一入参为 {@code null} 时如实不处理。/ Either argument being {@code null} is a no-op.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @param row 参数 已加载行模型；parameter the loaded row model.
     */
    public void applyBusiness(KnowledgeBaseBO carrier, KnowledgeBasePO row) {
        if (carrier == null || row == null) {
            log.debug("gateway_knowledge_base carrier or row is absent; nothing applied");
            return;
        }
        MAPPING.applyBusiness(carrier, row);
    }
}

/**
 * 中文说明：{@code KnowledgeBaseMapping} 是 gateway_knowledge_base 的 MapStruct 结构映射契约，逐列声明读写形态：
 * {@code members} 在 {@code JsonNode} 与 {@link KnowledgeMemberDTO} 列表之间由本契约的默认方法换算，缺列时写入 JSON
 * {@code null} 节点而不是 SQL {@code NULL}（与 {@code GatewayJsonbTypeHandler} 的 NOT NULL 列口径一致），解码失败如实抛出
 * 而不静默放宽；{@code egress_policy} 只承认封闭 wire 词汇，未知值按 {@code fromWire} 失败关闭；
 * {@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code KnowledgeBaseMapping} is the MapStruct structural contract for gateway_knowledge_base, stating every
 * column shape: {@code members} converts between {@code JsonNode} and the {@link KnowledgeMemberDTO} list in the default
 * methods here, an absent value writes a JSON {@code null} node instead of SQL {@code NULL} to match the {@code NOT NULL}
 * handling of {@code GatewayJsonbTypeHandler}, and a decoding failure surfaces truthfully rather than widening;
 * {@code egress_policy} accepts only the closed wire vocabulary and fails closed through {@code fromWire}; and
 * {@code unmappedTargetPolicy=ERROR} forces every new column to be declared here.
 *
 * 用法 / Usage: 由 {@link KnowledgeBasePersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link KnowledgeBasePersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a
 * Spring bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface KnowledgeBaseMapping {

    /**
     * 中文说明：保存 jsonb 列形态与结构化载体之间的 Jackson 转换器，与类型处理器同源且不开启 default typing。
     * English summary: Holds the Jackson converter between the jsonb column shapes and the structured carrier, matching the type
     * handler and keeping default typing disabled.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 中文说明：{@code members} 列的读取形态：顶层 {@link KnowledgeMemberDTO} 成员记录列表。
     * English summary: The read shape of the {@code members} column: the list of top-level {@link KnowledgeMemberDTO} records.
     */
    TypeReference<List<KnowledgeMemberDTO>> MEMBER_LIST = new TypeReference<>() {
    };

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code KnowledgeBaseBO} 的字段顺序投影 gateway_knowledge_base 行。
     * English summary: Executes the toBusiness operation, projecting a gateway_knowledge_base row onto {@code KnowledgeBaseBO}.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeBasePersistenceConverter#toBusiness(KnowledgeBasePO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "ownerActorId", source = "ownerActorId")
    @Mapping(target = "members", expression = "java( members( source.getMembers() ) )")
    @Mapping(target = "egressPolicy", expression = "java( egressPolicy( source.getEgressPolicy() ) )")
    @Mapping(target = "chatModel", source = "chatModel")
    @Mapping(target = "embeddingModel", source = "embeddingModel")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "revision", expression = "java( value( source.getRevision() ) )")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    KnowledgeBaseBO toBusiness(KnowledgeBasePO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回 gateway_knowledge_base 行；主键按十进制文本解析，成员与出口策略
     * 回写为 jsonb/封闭文本列，租户、审计、软删与版本列全部忽略。
     * English summary: Executes the toRow operation, writing the carrier back onto a gateway_knowledge_base row; the key parses
     * from its decimal text, members and the egress policy render back as the jsonb and closed-text columns, and the tenant,
     * audit, soft-delete and version columns stay ignored.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeBasePersistenceConverter#toPersistence(KnowledgeBaseBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "ownerActorId", source = "ownerActorId")
    @Mapping(target = "members", expression = "java( memberNode( source.getMembers() ) )")
    @Mapping(target = "egressPolicy", expression = "java( wire( source.getEgressPolicy() ) )")
    @Mapping(target = "chatModel", source = "chatModel")
    @Mapping(target = "embeddingModel", source = "embeddingModel")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    KnowledgeBasePO toRow(KnowledgeBaseBO source);

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载行上覆盖可写业务列；定位主键、不可转移的 {@code ownerActorId}、
     * 只由 CAS 语句推进的业务 {@code revision} 与受保护元数据保持不变。
     * English summary: Executes the applyBusiness operation, replacing the writable business columns of a loaded row; the
     * locating identifier, the non-transferable {@code ownerActorId}, the business {@code revision} advanced only by the CAS
     * statement and the protected metadata stay untouched.
     *
     * 用法 / Usage: 仅由 {@link KnowledgeBasePersistenceConverter#applyBusiness(KnowledgeBaseBO, KnowledgeBasePO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @param target 参数 已加载行模型；parameter the loaded row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "members", expression = "java( memberNode( source.getMembers() ) )")
    @Mapping(target = "egressPolicy", expression = "java( wire( source.getEgressPolicy() ) )")
    @Mapping(target = "chatModel", source = "chatModel")
    @Mapping(target = "embeddingModel", source = "embeddingModel")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "ownerActorId", ignore = true)
    @Mapping(target = "revision", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyBusiness(KnowledgeBaseBO source,
                       @MappingTarget KnowledgeBasePO target);

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 主键按十进制文本投影，{@code null} 保持 {@code null}。
     * English summary: Executes the text operation, projecting the MP bigint key as decimal text; {@code null} stays {@code null}.
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
                    "gateway_knowledge_base identifier must be decimal: " + value,
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
     * 中文说明：执行 members 操作，把 {@code members} jsonb 数组解码为顶层 {@link KnowledgeMemberDTO} 列表，
     * 角色按其 {@code @JsonCreator} 逐个失败关闭；列缺失返回 {@code null}，交由业务载体的声明约束如实拒绝。
     * English summary: Executes the members operation, decoding the {@code members} jsonb array into the top-level
     * {@link KnowledgeMemberDTO} list through each role's fail-closed {@code @JsonCreator}; a missing column yields
     * {@code null} so the carrier's declared constraints reject it truthfully.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 成员列表；returns the member list.
     */
    default List<KnowledgeMemberDTO> members(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, MEMBER_LIST);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored gateway_knowledge_base members is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 memberNode 操作，把成员列表渲染为 jsonb 数组（角色为 wire 字符串），缺失写 JSON {@code null} 节点，
     * 不引入第二套成员载体也不做 JSON 往返复制。
     * English summary: Executes the memberNode operation, rendering the member list as a jsonb array of wire-string roles and
     * writing a JSON {@code null} node when absent, without a second member carrier and without a JSON round trip.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 成员列表；parameter the member list.
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode memberNode(List<KnowledgeMemberDTO> value) {
        return value == null ? OBJECT_MAPPER.nullNode() : OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * 中文说明：执行 egressPolicy 操作，把 {@code egress_policy} 封闭文本列解码为策略枚举，未知词汇如实抛出。
     * English summary: Executes the egressPolicy operation, decoding the closed {@code egress_policy} column into the policy enum
     * and surfacing an unknown vocabulary truthfully.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 策略枚举或 {@code null}；returns the policy enum or {@code null}.
     */
    default KnowledgeEgressPolicyEnum egressPolicy(String value) {
        return value == null ? null : KnowledgeEgressPolicyEnum.fromWire(value);
    }

    /**
     * 中文说明：执行 wire 操作，把策略枚举按其 {@code @JsonValue} wire 字符串写回封闭列，绝不写 ordinal。
     * English summary: Executes the wire operation, writing the policy enum back into the closed column as its
     * {@code @JsonValue} wire string and never as an ordinal.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 策略枚举；parameter the policy enum.
     * @return 返回 wire 字符串；returns the wire string.
     */
    default String wire(KnowledgeEgressPolicyEnum value) {
        return value == null ? null : value.wireValue();
    }
}
