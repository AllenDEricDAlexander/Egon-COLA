package top.egon.cola.component.yuheng.admin.routing.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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
import top.egon.cola.component.yuheng.admin.routing.domain.bo.GatewayPolicyDraftBO;
import top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayPolicyDraftRecordPO;

/**
 * 中文说明：{@code GatewayPolicyDraftPersistenceConverter} 是 gateway_policy_draft 聚合在持久边界唯一的 MapStruct 转换器，负责
 * {@link GatewayPolicyDraftRecordPO} 行模型与 {@link GatewayPolicyDraftBO} 业务载体之间的双向映射；
 * {@code gatewayGroupId} 以十进制文本往返，{@code policyId} 保持对外不透明编码字符串，
 * {@code policy_content} jsonb 列经 Jackson 在节点与 {@code Map<String, Object>} 之间转换，updatedAt/updatedBy 只读投影自 MP 审计列。
 * English summary: {@code GatewayPolicyDraftPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * gateway_policy_draft, mapping {@link GatewayPolicyDraftRecordPO} rows and {@link GatewayPolicyDraftBO} carriers both ways.
 * {@code gatewayGroupId} round-trips as decimal text, {@code policyId} stays an opaque coded string, the {@code policy_content} jsonb
 * column travels through Jackson between a node and a {@code Map<String, Object>}, and updatedAt/updatedBy are read-only projections of
 * the MP audit columns.
 *
 * 用法 / Usage: 由 {@code MpGatewayDraftRepository} 等受守卫仓储注入使用（bean 名 {@code gatewayPolicyDraftPersistenceConverter}）；
 * 子行以 {@code (gateway_group_id, policy_id)} 为业务身份，写方向不生成 MP {@code id}，更新须先载入行再回写业务列。
 * Injected by the guarded policy draft repository under the bean name {@code gatewayPolicyDraftPersistenceConverter}; a child row is
 * keyed by {@code (gateway_group_id, policy_id)}, so the write direction never produces an MP {@code id} and an update loads the row
 * first.
 */
@Slf4j
@Component("gatewayPolicyDraftPersistenceConverter")
public class GatewayPolicyDraftPersistenceConverter implements BaseConverter<
        GatewayPolicyDraftRecordPO,
        GatewayPolicyDraftBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayPolicyDraftMapping MAPPING =
            Mappers.getMapper(GatewayPolicyDraftMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把策略草稿载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering a policy draft carrier into the MyBatis-Plus row model, encoding the
     * policy content as a jsonb node and leaving the identifier and every technical column to the guarded persistence layer.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayPolicyDraftPersistenceConverter.toPersistence(policyBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayPolicyDraftRecordPO toPersistence(GatewayPolicyDraftBO carrier) {
        if (carrier == null) {
            log.debug("gateway policy draft carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把策略草稿行投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting a policy draft row onto the business carrier, decoding the jsonb
     * policy content and reading the last change instant and actor from the MP update audit columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayPolicyDraftPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayPolicyDraftBO toBusiness(GatewayPolicyDraftRecordPO row) {
        if (row == null) {
            log.debug("gateway policy draft row is absent; no carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染策略草稿载体。
     * English summary: Executes the toPersistenceList operation, rendering every policy draft carrier in order; null or empty input yields
     * an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayPolicyDraftPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayPolicyDraftRecordPO> toPersistenceList(
            List<GatewayPolicyDraftBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影策略草稿行。
     * English summary: Executes the toBusinessList operation, projecting every policy draft row in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayPolicyDraftPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayPolicyDraftBO> toBusinessList(
            List<GatewayPolicyDraftRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayPolicyDraftPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayPolicyDraftBO toTarget(GatewayPolicyDraftRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayPolicyDraftPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayPolicyDraftRecordPO toSource(GatewayPolicyDraftBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，
     * 技术主键、租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; the technical id, tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayPolicyDraftPersistenceConverter.newRow(policyBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayPolicyDraftRecordPO newRow(GatewayPolicyDraftBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：执行 applyBusiness 操作，把业务载体的可写列整行覆盖回已加载的行模型，保留技术 id 与受保护元数据，
     * 供乐观锁 CAS 更新使用。
     * English summary: Executes the applyBusiness operation, overwrite-replacing the writable business columns of a loaded row from the
     * carrier while keeping the technical id and the protected metadata, for the optimistic-lock CAS update path.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayPolicyDraftPersistenceConverter.applyBusiness(policyBO, row)}。任一入参为 {@code null} 时如实不处理。
     * Either argument being {@code null} is a no-op.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @param row 参数 已加载行模型；parameter the loaded row model.
     */
    public void applyBusiness(GatewayPolicyDraftBO carrier, GatewayPolicyDraftRecordPO row) {
        if (carrier == null || row == null) {
            log.debug("gateway policy draft carrier or row is absent; nothing applied");
            return;
        }
        MAPPING.applyBusiness(carrier, row);
    }
}

/**
 * 中文说明：{@code GatewayPolicyDraftMapping} 是 gateway_policy_draft 的 MapStruct 结构映射契约，逐列复刻被替换的手写 JDBC 语义：
 * NOT NULL 的 {@code policy_content} 在业务内容为空时写入 JSON {@code null} 节点（等价于原 {@code json(null)} 绑定），解码失败按原
 * 实现抛出「stored draft value is invalid」。
 * English summary: {@code GatewayPolicyDraftMapping} is the MapStruct structural contract for gateway_policy_draft, mirroring the JDBC it
 * replaces: the NOT NULL {@code policy_content} column receives a JSON {@code null} node when the business content is absent (the legacy
 * {@code json(null)} binding) and a decoding failure raises the legacy {@code stored draft value is invalid} error.
 *
 * 用法 / Usage: 由 {@link GatewayPolicyDraftPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayPolicyDraftPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayPolicyDraftMapping {

    /**
     * 中文说明：保存 Jackson 序列化器，禁止 default typing，只做列值与结构化载体之间的转换。
     * English summary: Holds the Jackson converter between the column node and the structured carrier; default typing stays disabled.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 中文说明：表示 CONTENT_MAP 这一固定值，声明 {@code policy_content} 列的结构化载体类型。
     * English summary: Represents the fixed content map value, the structured carrier type of the {@code policy_content} column.
     *
     * 用法 / Usage: 仅由本映射的默认方法使用。/ Used only by the default methods of this mapping.
     */
    TypeReference<Map<String, Object>> CONTENT_MAP = new TypeReference<>() {
    };

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayPolicyDraftBO} 的字段顺序投影策略草稿行。
     * English summary: Executes the toBusiness operation, projecting a policy draft row onto {@code GatewayPolicyDraftBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayPolicyDraftPersistenceConverter#toBusiness(GatewayPolicyDraftRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "gatewayGroupId", expression = "java( text( source.getGatewayGroupId() ) )")
    @Mapping(target = "policyId", source = "policyId")
    @Mapping(target = "policyType", source = "policyType")
    @Mapping(target = "policyScope", source = "policyScope")
    @Mapping(target = "content", expression = "java( content( source.getPolicyContent() ) )")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "updatedAt", source = "updateTime")
    @Mapping(target = "updatedBy", source = "updateUserId")
    GatewayPolicyDraftBO toBusiness(GatewayPolicyDraftRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把策略草稿载体写回行模型；主键与受保护技术列全部忽略。
     * English summary: Executes the toRow operation, writing the policy draft carrier back onto the row model; the identifier and every
     * protected technical column stay ignored.
     *
     * 用法 / Usage: 仅由 {@link GatewayPolicyDraftPersistenceConverter#toPersistence(GatewayPolicyDraftBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "gatewayGroupId", expression = "java( identifier( source.getGatewayGroupId() ) )")
    @Mapping(target = "policyId", source = "policyId")
    @Mapping(target = "policyType", source = "policyType")
    @Mapping(target = "policyScope", source = "policyScope")
    @Mapping(target = "policyContent", expression = "java( requiredNode( source.getContent() ) )")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayPolicyDraftRecordPO toRow(GatewayPolicyDraftBO source);

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载行上覆盖业务列；分组定位键、主键与受保护元数据保持不变。
     * English summary: Executes the applyBusiness operation, replacing the business columns of a loaded row; the gateway group locator, the
     * identifier and the protected metadata stay untouched.
     *
     * 用法 / Usage: 仅由 {@link GatewayPolicyDraftPersistenceConverter#applyBusiness(GatewayPolicyDraftBO, GatewayPolicyDraftRecordPO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @param target 参数 已加载行模型；parameter the loaded row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "gatewayGroupId", ignore = true)
    @Mapping(target = "policyId", source = "policyId")
    @Mapping(target = "policyType", source = "policyType")
    @Mapping(target = "policyScope", source = "policyScope")
    @Mapping(target = "policyContent", expression = "java( requiredNode( source.getContent() ) )")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyBusiness(GatewayPolicyDraftBO source,
                       @MappingTarget GatewayPolicyDraftRecordPO target);

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 列按十进制文本投影，等价于原 JDBC 的 {@code getString(...)}。
     * English summary: Executes the text operation, projecting an MP bigint column as decimal text, exactly what the legacy
     * {@code getString(...)} produced; {@code null} stays {@code null}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 标识；parameter identifier。
     * @return 返回 十进制文本；returns the decimal text.
     */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /**
     * 中文说明：执行 identifier 操作，把业务十进制分组标识解析为 MP bigint；空白直接拒绝，因为策略必须归属一个分组。
     * English summary: Executes the identifier operation, parsing the business decimal group identifier into the MP bigint; a blank value is
     * rejected outright because a policy always belongs to a gateway group.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 业务标识；parameter business identifier。
     * @return 返回 标识；returns the identifier.
     */
    default Long identifier(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("policy draft identifier is required");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "gateway policy draft identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 content 操作，把 {@code policy_content} jsonb 节点解码为结构化对象；JSON {@code null} 投影为 {@code null}，
     * 结构不符按原实现抛出「stored draft value is invalid」。
     * English summary: Executes the content operation, decoding the {@code policy_content} jsonb node into the structured object; a JSON
     * {@code null} projects to {@code null} and a shape mismatch raises the legacy {@code stored draft value is invalid} failure.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 jsonb 节点；parameter jsonb node。
     * @return 返回 结构化对象；returns the structured object.
     */
    default Map<String, Object> content(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, CONTENT_MAP);
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored draft value is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 requiredNode 操作，把结构化对象编码为 jsonb 节点；NOT NULL 列在业务值为空时写入 JSON {@code null} 节点。
     * English summary: Executes the requiredNode operation, encoding the structured object into a jsonb node; the NOT NULL column receives a
     * JSON {@code null} node when the business value is absent, matching the replaced {@code json(null)} binding.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 结构化对象；parameter structured object。
     * @return 返回 jsonb 节点；returns the jsonb node.
     */
    default JsonNode requiredNode(Map<String, Object> value) {
        return value == null
                ? OBJECT_MAPPER.nullNode()
                : OBJECT_MAPPER.valueToTree(value);
    }
}
