package top.egon.cola.component.yuheng.admin.llm.converter;

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
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmModelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmRouteBindingDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.po.LlmModelPO;

/**
 * 中文说明：{@code LlmModelPersistenceConverter} 是 {@code gateway_llm_model} 在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link LlmModelPO} 行模型与 {@link LlmModelBO} 业务载体之间的双向映射：不透明主键以十进制文本往返，
 * 业务稳定 key 与库列 {@code model_key} 同名同形往返，{@code protocols}/{@code allowedSubjects}/{@code routes} 三列
 * 经与 {@code GatewayJsonbTypeHandler} 同源的 Jackson 序列化为结构化集合（枚举按 {@code @JsonValue} wire 字符串入集，
 * 绝不写 ordinal），{@code createdAt}/{@code updatedAt} 只读地投影自 MP 审计列，
 * 租户、操作者、软删与技术版本列一律不由业务载体写入。
 * English summary: {@code LlmModelPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_llm_model}, mapping {@link LlmModelPO} rows and {@link LlmModelBO} carriers both ways: the opaque key
 * round-trips as decimal text, the business stable key and the {@code model_key} column cross over one-to-one, the three
 * jsonb columns
 * {@code protocols}/{@code allowedSubjects}/{@code routes} become structured collections through the same Jackson
 * serializer as {@code GatewayJsonbTypeHandler} (enums enter the collections as their {@code @JsonValue} wire strings and
 * never as ordinals), the audit instants are read-only projections, and tenant, operator, soft-delete and technical
 * version columns are never written from the business carrier.
 *
 * 用法 / Usage: 由 {@code MpLlmConfigurationRepository} 注入使用（bean 名 {@code llmModelPersistenceConverter}）；
 * {@code newRow} 产出只带业务列的待插入行，{@code applyBusiness} 在已加载活跃行上整行覆盖可写业务列并保留定位键与
 * 受保护元数据，供 {@code revision} 递增后的乐观锁 CAS 复用。行模型禁止进入端口或服务签名。
 * Injected by the guarded model store under the bean name {@code llmModelPersistenceConverter}; {@code newRow} yields a
 * business-columns-only insert candidate and {@code applyBusiness} replaces the writable columns of a loaded active row for
 * the CAS path. A row model may never reach a port or a service signature.
 */
@Slf4j
@Component("llmModelPersistenceConverter")
public class LlmModelPersistenceConverter implements BaseConverter<
        LlmModelPO,
        LlmModelBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final LlmModelMapping MAPPING =
            Mappers.getMapper(LlmModelMapping.class);

    /**
     * 中文说明：执行 toBusiness 操作，把模型行模型投影为业务载体，三个 jsonb 列解码为结构化集合。
     * English summary: Executes the toBusiness operation, projecting a model row onto the business carrier and decoding the three
     * jsonb columns into structured collections.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmModelPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public LlmModelBO toBusiness(LlmModelPO row) {
        if (row == null) {
            log.debug("gateway_llm_model row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistence 操作，把业务载体渲染为 MP 行模型，结构化集合回写为 jsonb 列。
     * English summary: Executes the toPersistence operation, rendering a model carrier into the MyBatis-Plus row model and writing
     * the structured collections back as jsonb columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmModelPersistenceConverter.toPersistence(modelBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public LlmModelPO toPersistence(LlmModelBO carrier) {
        if (carrier == null) {
            log.debug("gateway_llm_model business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影模型行；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toBusinessList operation, projecting every model row in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmModelPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<LlmModelBO> toBusinessList(List<LlmModelPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染模型载体；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toPersistenceList operation, rendering every model carrier in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmModelPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<LlmModelPO> toPersistenceList(List<LlmModelBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmModelPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public LlmModelBO toTarget(LlmModelPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmModelPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public LlmModelPO toSource(LlmModelBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmModelPersistenceConverter.newRow(modelBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public LlmModelPO newRow(LlmModelBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载的活跃行上整行覆盖可写业务列（含三个 jsonb 列），
     * 保留定位键 {@code modelKey}、主键与受保护元数据，供乐观锁 CAS 更新使用。
     * English summary: Executes the applyBusiness operation, overwrite-replacing the writable business columns (the three jsonb
     * columns included) of a loaded active row while keeping the {@code modelKey} locator, the primary key and the protected
     * metadata for the optimistic-lock CAS update.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmModelPersistenceConverter.applyBusiness(modelBO, row)}。
     * 任一入参为 {@code null} 时如实不处理。/ Either argument being {@code null} is a no-op.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @param row 参数 已加载行模型；parameter the loaded row model.
     */
    public void applyBusiness(LlmModelBO carrier, LlmModelPO row) {
        if (carrier == null || row == null) {
            log.debug("gateway_llm_model carrier or row is absent; nothing applied");
            return;
        }
        MAPPING.applyBusiness(carrier, row);
    }
}

/**
 * 中文说明：{@code LlmModelMapping} 是 gateway_llm_model 的 MapStruct 结构映射契约，逐列声明读写形态：
 * 三个 jsonb 列在 {@code JsonNode} 与结构化集合之间由本契约的默认方法换算，缺列时写入 JSON {@code null} 节点而不是
 * SQL {@code NULL}（与 {@code GatewayJsonbTypeHandler} 的 NOT NULL 列口径一致），解码失败如实抛出而不静默放宽；
 * {@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code LlmModelMapping} is the MapStruct structural contract for gateway_llm_model, stating every column
 * shape: the three jsonb columns convert between {@code JsonNode} and structured collections in the default methods here, an
 * absent value writes a JSON {@code null} node instead of SQL {@code NULL} to match the {@code NOT NULL} handling of
 * {@code GatewayJsonbTypeHandler}, a decoding failure surfaces truthfully rather than widening, and
 * {@code unmappedTargetPolicy=ERROR} forces every new column to be declared.
 *
 * 用法 / Usage: 由 {@link LlmModelPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link LlmModelPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface LlmModelMapping {

    /**
     * 中文说明：保存 jsonb 列形态与结构化载体之间的 Jackson 转换器，与类型处理器同源且不开启 default typing。
     * English summary: Holds the Jackson converter between the jsonb column shapes and the structured carriers, matching the type
     * handler and keeping default typing disabled.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 中文说明：{@code protocols} 列的读取形态：协议 wire 字符串列表。
     * English summary: The read shape of the {@code protocols} column: a list of protocol wire strings.
     */
    TypeReference<List<LlmProtocolEnum>> PROTOCOL_LIST = new TypeReference<>() {
    };

    /**
     * 中文说明：{@code allowed_subjects} 列的读取形态：已验证 SERVICE subject 字符串列表。
     * English summary: The read shape of the {@code allowed_subjects} column: the list of verified service subjects.
     */
    TypeReference<List<String>> SUBJECT_LIST = new TypeReference<>() {
    };

    /**
     * 中文说明：{@code routes} 列的读取形态：顶层 {@link LlmRouteBindingDTO} 绑定列表。
     * English summary: The read shape of the {@code routes} column: the list of top-level {@link LlmRouteBindingDTO} bindings.
     */
    TypeReference<List<LlmRouteBindingDTO>> ROUTE_LIST = new TypeReference<>() {
    };

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code LlmModelBO} 的字段顺序投影 gateway_llm_model 行。
     * English summary: Executes the toBusiness operation, projecting a gateway_llm_model row onto {@code LlmModelBO}.
     *
     * 用法 / Usage: 仅由 {@link LlmModelPersistenceConverter#toBusiness(LlmModelPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "modelKey", source = "modelKey")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "protocols", expression = "java( protocols( source.getProtocols() ) )")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "allowedSubjects", expression = "java( subjects( source.getAllowedSubjects() ) )")
    @Mapping(target = "routes", expression = "java( routes( source.getRoutes() ) )")
    @Mapping(target = "revision", expression = "java( value( source.getRevision() ) )")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    LlmModelBO toBusiness(LlmModelPO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回 gateway_llm_model 行；主键按十进制文本解析，
     * 三个 jsonb 列按结构化集合渲染，租户、审计、软删与版本列全部忽略。
     * English summary: Executes the toRow operation, writing the carrier back onto a gateway_llm_model row; the key parses from its
     * decimal text, the three jsonb columns render from the structured collections, and the tenant, audit, soft-delete and
     * version columns stay ignored.
     *
     * 用法 / Usage: 仅由 {@link LlmModelPersistenceConverter#toPersistence(LlmModelBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "modelKey", source = "modelKey")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "protocols", expression = "java( protocolNode( source.getProtocols() ) )")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "allowedSubjects", expression = "java( subjectNode( source.getAllowedSubjects() ) )")
    @Mapping(target = "routes", expression = "java( routeNode( source.getRoutes() ) )")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    LlmModelPO toRow(LlmModelBO source);

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载行上覆盖业务列；定位键 {@code modelKey}、主键与受保护元数据保持不变。
     * English summary: Executes the applyBusiness operation, replacing the business columns of a loaded row; the {@code modelKey}
     * locator, the identifier and the protected metadata stay untouched.
     *
     * 用法 / Usage: 仅由 {@link LlmModelPersistenceConverter#applyBusiness(LlmModelBO, LlmModelPO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @param target 参数 已加载行模型；parameter the loaded row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "modelKey", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "protocols", expression = "java( protocolNode( source.getProtocols() ) )")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "allowedSubjects", expression = "java( subjectNode( source.getAllowedSubjects() ) )")
    @Mapping(target = "routes", expression = "java( routeNode( source.getRoutes() ) )")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyBusiness(LlmModelBO source,
                       @MappingTarget LlmModelPO target);

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
                    "gateway_llm_model identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 value 操作，把可空 bigint 业务 revision 投影为 long；列缺失即 0。
     * English summary: Executes the value operation, projecting the nullable bigint business revision onto the long carrier; an
     * absent column reads as zero.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 业务 revision；returns the business revision.
     */
    default long value(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 中文说明：执行 protocols 操作，把 jsonb 字符串数组解码为协议枚举列表，枚举按其 {@code @JsonCreator}
     * 逐个失败关闭；列缺失返回 {@code null}，交由业务载体的声明约束如实拒绝。
     * English summary: Executes the protocols operation, decoding the jsonb string array into protocol enums through each enum's
     * fail-closed {@code @JsonCreator}; a missing column yields {@code null} so the carrier's declared constraints reject it
     * truthfully.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 协议列表；returns the protocol list.
     */
    default List<LlmProtocolEnum> protocols(JsonNode value) {
        return decode(value, PROTOCOL_LIST, "protocols");
    }

    /**
     * 中文说明：执行 protocolsNode 操作，把协议列表渲染为 jsonb 数组（wire 字符串），缺失写 JSON {@code null} 节点。
     * English summary: Executes the protocolsNode operation, rendering the protocol list as a jsonb array of wire strings and
     * writing a JSON {@code null} node when absent.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 协议列表；parameter the protocol list。
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode protocolNode(List<LlmProtocolEnum> value) {
        return value == null ? OBJECT_MAPPER.nullNode() : OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * 中文说明：执行 subjects 操作，把 {@code allowed_subjects} jsonb 字符串数组解码为主体列表。
     * English summary: Executes the subjects operation, decoding the {@code allowed_subjects} jsonb string array into subjects.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 主体列表；returns the subject list.
     */
    default List<String> subjects(JsonNode value) {
        return decode(value, SUBJECT_LIST, "allowedSubjects");
    }

    /**
     * 中文说明：执行 subjectNode 操作，把主体列表渲染为 jsonb 数组，缺失写 JSON {@code null} 节点。
     * English summary: Executes the subjectNode operation, rendering the subject list as a jsonb array and writing a JSON
     * {@code null} node when absent.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 主体列表；parameter the subject list。
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode subjectNode(List<String> value) {
        return value == null ? OBJECT_MAPPER.nullNode() : OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * 中文说明：执行 routes 操作，把 {@code routes} jsonb 数组解码为顶层 {@link LlmRouteBindingDTO} 列表，
     * 不引入第二套路由载体也不做 JSON 往返复制。
     * English summary: Executes the routes operation, decoding the {@code routes} jsonb array into the top-level
     * {@link LlmRouteBindingDTO} list without a second routing carrier and without a JSON round trip.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 路由绑定列表；returns the route bindings.
     */
    default List<LlmRouteBindingDTO> routes(JsonNode value) {
        return decode(value, ROUTE_LIST, "routes");
    }

    /**
     * 中文说明：执行 routeNode 操作，把路由绑定列表渲染为 jsonb 数组，缺失写 JSON {@code null} 节点。
     * English summary: Executes the routeNode operation, rendering the route bindings as a jsonb array and writing a JSON
     * {@code null} node when absent.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 路由绑定列表；parameter the route bindings。
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode routeNode(List<LlmRouteBindingDTO> value) {
        return value == null ? OBJECT_MAPPER.nullNode() : OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * 中文说明：按固定读取形态解码 jsonb 列；列缺失保持 {@code null}，形态不符按原列名如实抛出。
     * English summary: Decodes a jsonb column against its fixed read shape; an absent column stays {@code null} and a malformed
     * one fails truthfully naming the column.
     *
     * 用法 / Usage: 仅由本映射的列解码方法调用。/ Called only by the column decoders of this mapping.
     * @param value 参数 列值；parameter column value。
     * @param shape 参数 读取形态；parameter the read shape.
     * @param column 参数 列名；parameter column name.
     * @param <V> 参数 载体类型；parameter carrier type。
     * @return 返回 解码结果或 {@code null}；returns the decoded value or {@code null}.
     */
    default <V> V decode(JsonNode value, TypeReference<V> shape, String column) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, shape);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored gateway_llm_model " + column + " is invalid",
                    failure
            );
        }
    }
}
