package top.egon.cola.component.yuheng.admin.mcp.converter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpArtifactMetadataBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.enums.McpArtifactStatusEnum;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpArtifactMetadataRecordPO;

/**
 * 中文说明：{@code McpArtifactMetadataPersistenceConverter} 是 {@code gateway_mcp_app_artifact} 在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link McpArtifactMetadataRecordPO} 行模型与 {@link McpArtifactMetadataBO} 业务载体之间的双向映射：
 * 端口上的制品编号与分组标识按十进制文本与 {@code bigint} 列往返，制品版本落在 {@code app_version}、摘要落在
 * {@code artifact_sha256}；两个 jsonb 集合列（{@code permission_manifest} 与 {@code allowed_origins}）经 Spring 托管的 Jackson
 * 在 {@code JsonNode} 与 {@code Set<String>} 之间往返，并逐字保留被替换的字符串集合规范形态——编码失败抛
 * {@code MCP persistence value cannot be serialized}、解码失败抛 {@code stored MCP string set is invalid}，
 * 集合读回时一律做不可变复制；{@code status} 列按 {@code McpArtifactStatusEnum} 的 wire 字面量读写（永不 ordinal），
 * 新行按旧语句的 {@code 'ACTIVE'} 字面量落位；{@code createdBy}/{@code createdAt} 只读投影自边界审计列，
 * 写方向绝不触碰租户、审计、软删、版本与技术主键。
 * English summary: {@code McpArtifactMetadataPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_mcp_app_artifact}, mapping {@link McpArtifactMetadataRecordPO} rows and {@link McpArtifactMetadataBO} carriers both
 * ways: the artifact identifier and the Group identifier round-trip as decimal text against their {@code bigint} columns, the artifact
 * version lands in {@code app_version} and the digest in {@code artifact_sha256}; the two jsonb collection columns
 * ({@code permission_manifest} and {@code allowed_origins}) round-trip through the Spring-managed Jackson mapper between a
 * {@code JsonNode} and a {@code Set<String>} and keep the canonical string-set shape of the replaced persistence boundary verbatim -
 * an encoding failure raises {@code MCP persistence value cannot be serialized}, a decoding failure
 * {@code stored MCP string set is invalid} - and every read collection is copied immutably; {@code status} travels as the
 * {@code McpArtifactStatusEnum} wire literal (never an ordinal) and a fresh row carries the legacy {@code 'ACTIVE'} literal;
 * {@code createdBy}/{@code createdAt} are read-only projections of the boundary audit columns and the write direction never touches the
 * tenant, audit, soft-delete, version or technical identifier columns.
 *
 * 用法 / Usage: 由 {@code gateway_mcp_app_artifact} 的受守卫 MP 门面注入（bean 名 {@code mcpArtifactMetadataPersistenceConverter}）；
 * 写方向只渲染业务列，审计与租户列由 {@code EgonColaMetaObjectHandler} 与 {@code ASSIGN_ID} 独占。门面只在显式置空
 * jsonb 列时才通过 {@link Wrappers} 携带 {@link JsonNode}，集合编解码一律经 {@link #stringSet(JsonNode)} 与
 * {@link #stringSetNode(Set)} 完成。
 * Injected by the guarded {@code gateway_mcp_app_artifact} facade under the bean name
 * {@code mcpArtifactMetadataPersistenceConverter}; the write direction renders business columns only because
 * {@code EgonColaMetaObjectHandler} and {@code ASSIGN_ID} own the audit and tenant columns. A facade that writes a single collection
 * column encodes and decodes it only through {@link #stringSetNode(Set)} and {@link #stringSet(JsonNode)}.
 */
@Slf4j
@Component("mcpArtifactMetadataPersistenceConverter")
@RequiredArgsConstructor
public class McpArtifactMetadataPersistenceConverter implements BaseConverter<
        McpArtifactMetadataRecordPO,
        McpArtifactMetadataBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只补充 jsonb 集合列编解码与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds the jsonb collection codec and
     * logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final McpArtifactMetadataMapping MAPPING =
            Mappers.getMapper(McpArtifactMetadataMapping.class);

    /**
     * 中文说明：表示 STRING_SET 这一固定值，声明两个 jsonb 集合列的字符串集合读取形态，与被替换边界
     * {@code new TypeReference<Set<String>>()} 完全一致。
     * English summary: Represents the fixed string-set value, the read shape of the two jsonb collection columns, identical to the
     * {@code new TypeReference<Set<String>>()} of the replaced persistence boundary.
     *
     * 用法 / Usage: 仅由本类的集合解码使用。/ Used only by the collection decoding of this class.
     */
    private static final TypeReference<Set<String>> STRING_SET =
            new TypeReference<>() {
            };

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：执行 toPersistence 操作，把制品元数据载体渲染为 MP 行模型并编码两个 jsonb 集合列；
     * {@code status} 留空由 {@link #newRow(McpArtifactMetadataBO)} 按旧语句落 {@code ACTIVE}，撤销路径由门面显式下推。
     * English summary: Executes the toPersistence operation, rendering an artifact metadata carrier into the MyBatis-Plus row model and
     * encoding the two jsonb collection columns; {@code status} stays unset here because {@link #newRow(McpArtifactMetadataBO)} applies
     * the legacy {@code ACTIVE} literal, while revocation is pushed explicitly by the facade.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpArtifactMetadataPersistenceConverter.toPersistence(artifactBO)}。传入 {@code null}
     * 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public McpArtifactMetadataRecordPO toPersistence(McpArtifactMetadataBO carrier) {
        if (carrier == null) {
            log.debug("gateway_mcp_app_artifact business carrier is absent; no row rendered");
            return null;
        }
        McpArtifactMetadataRecordPO row = MAPPING.toRow(carrier);
        row.setPermissionManifest(stringSetNode(carrier.getPermissions()));
        row.setAllowedOrigins(stringSetNode(carrier.getAllowedOrigins()));
        return row;
    }

    /**
     * 中文说明：执行 toBusiness 操作，把制品元数据行投影为业务载体，解码两个 jsonb 集合列，
     * 并把边界审计列投影为旧载体的 {@code createdBy}/{@code createdAt}。
     * English summary: Executes the toBusiness operation, projecting an artifact metadata row onto the business carrier, decoding the two
     * jsonb collection columns and rendering the boundary audit columns as the legacy {@code createdBy}/{@code createdAt}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpArtifactMetadataPersistenceConverter.toBusiness(row)}。传入 {@code null}
     * 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public McpArtifactMetadataBO toBusiness(McpArtifactMetadataRecordPO row) {
        if (row == null) {
            log.debug("gateway_mcp_app_artifact row is absent; no business carrier projected");
            return null;
        }
        McpArtifactMetadataBO carrier = MAPPING.toBusiness(row);
        carrier.setPermissions(stringSet(row.getPermissionManifest()));
        carrier.setAllowedOrigins(stringSet(row.getAllowedOrigins()));
        return carrier;
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染制品元数据载体；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toPersistenceList operation, rendering every artifact carrier in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpArtifactMetadataPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<McpArtifactMetadataRecordPO> toPersistenceList(List<McpArtifactMetadataBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影制品元数据行；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toBusinessList operation, projecting every artifact row in order; null or empty input yields an empty
     * list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpArtifactMetadataPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<McpArtifactMetadataBO> toBusinessList(List<McpArtifactMetadataRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpArtifactMetadataPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public McpArtifactMetadataBO toTarget(McpArtifactMetadataRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpArtifactMetadataPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public McpArtifactMetadataRecordPO toSource(McpArtifactMetadataBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，
     * 并按被替换语句的 {@code status} 字面量把新制品落为 {@code ACTIVE}；技术主键、租户、审计、软删与版本列一律留空
     * 交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only, stamped with the replaced statement's {@code status} literal so a fresh artifact lands {@code ACTIVE}; the technical
     * identifier and the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpArtifactMetadataPersistenceConverter.newRow(artifactBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public McpArtifactMetadataRecordPO newRow(McpArtifactMetadataBO carrier) {
        McpArtifactMetadataRecordPO row = toPersistence(carrier);
        if (row != null) {
            row.setStatus(McpArtifactStatusEnum.ACTIVE.wireValue());
        }
        return row;
    }

    /**
     * 中文说明：执行 stringSetNode 操作，把字符串集合编码为 jsonb 列值；{@code null} 如实返回 {@code null}，
     * 由门面在需要时显式下推 SQL NULL，编码失败沿用被替换边界的
     * 「MCP persistence value cannot be serialized」文案。
     * English summary: Executes the stringSetNode operation, encoding a string set into the jsonb column value; {@code null} truthfully
     * yields {@code null} so the facade pushes an explicit SQL NULL when it has to, and an encoding failure keeps the replaced
     * boundary's {@code MCP persistence value cannot be serialized} message.
     *
     * 用法 / Usage: 由 {@link #toPersistence(McpArtifactMetadataBO)} 与需要单列覆写的门面调用。
     * @param value 参数 字符串集合；parameter string set。
     * @return 返回 jsonb 列值或 {@code null}；returns the jsonb column value or {@code null}.
     */
    public JsonNode stringSetNode(Set<String> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.valueToTree(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "MCP persistence value cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 stringSet 操作，把 jsonb 列值解码为不可变字符串集合，与被替换边界的
     * {@code Set.copyOf(readValue(...))} 形态一致；列值为 SQL NULL 时如实返回 {@code null}，
     * 交由 {@code McpArtifactMetadataBO.normalized(...)} 按旧文案拒绝，解码失败沿用
     * 「stored MCP string set is invalid」文案。
     * English summary: Executes the stringSet operation, decoding a jsonb column value into an immutable string set exactly like the
     * replaced boundary's {@code Set.copyOf(readValue(...))}; an SQL NULL column value truthfully yields {@code null} so
     * {@code McpArtifactMetadataBO.normalized(...)} rejects it with the legacy wording, and a decoding failure keeps the
     * {@code stored MCP string set is invalid} message.
     *
     * 用法 / Usage: 仅由 {@link #toBusiness(McpArtifactMetadataRecordPO)} 调用。
     * @param value 参数 jsonb 列值；parameter jsonb column value。
     * @return 返回 不可变字符串集合或 {@code null}；returns the immutable string set or {@code null}.
     */
    public Set<String> stringSet(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return Set.copyOf(objectMapper.convertValue(value, STRING_SET));
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored MCP string set is invalid",
                    failure
            );
        }
    }
}

/**
 * 中文说明：{@code McpArtifactMetadataMapping} 是 {@code gateway_mcp_app_artifact} 的 MapStruct 结构映射契约，
 * 逐列复刻被替换的手写语句语义：{@code app_version} 与 {@code artifact_sha256} 是载体上
 * {@code version}/{@code sha256} 的落位列，两个 jsonb 集合目标在本接口显式忽略（编解码由外部转换器经
 * Spring 托管的 {@code jacksonObjectMapper} 完成，映射接口不持有裸序列化器），可空 {@code size_bytes}
 * 按原 {@code getLong} 读作 0，旧载体的 {@code createdBy}/{@code createdAt} 投影自边界审计列；
 * 受保护技术列全部忽略，因为 {@code EgonColaMetaObjectHandler} 独占租户、审计、软删与 MP 版本列；
 * {@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code McpArtifactMetadataMapping} is the MapStruct structural contract for {@code gateway_mcp_app_artifact},
 * mirroring the hand-written statements it replaces column by column: {@code app_version} and {@code artifact_sha256} are where the
 * carrier's {@code version}/{@code sha256} land, the two jsonb collection targets are ignored explicitly here (their codec runs in the
 * enclosing converter through the Spring-managed {@code jacksonObjectMapper}, so this interface holds no raw serializer), a null
 * {@code size_bytes} reads as 0 like the legacy {@code getLong}, and the legacy {@code createdBy}/{@code createdAt} project from the
 * boundary audit columns; every protected technical column stays ignored because {@code EgonColaMetaObjectHandler} owns the tenant,
 * audit, soft-delete and MP version columns; {@code unmappedTargetPolicy=ERROR} forces every new column to be stated explicitly here.
 *
 * 用法 / Usage: 由 {@link McpArtifactMetadataPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link McpArtifactMetadataPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface McpArtifactMetadataMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code McpArtifactMetadataBO} 的字段顺序投影制品元数据行。
     * English summary: Executes the toBusiness operation, projecting an artifact metadata row onto {@code McpArtifactMetadataBO}.
     *
     * 用法 / Usage: 仅由 {@link McpArtifactMetadataPersistenceConverter#toBusiness(McpArtifactMetadataRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "gatewayGroupId", expression = "java( text( source.getGatewayGroupId() ) )")
    @Mapping(target = "appCode", source = "appCode")
    @Mapping(target = "version", source = "appVersion")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "resourceUri", source = "resourceUri")
    @Mapping(target = "artifactReference", source = "artifactReference")
    @Mapping(target = "sha256", source = "artifactSha256")
    @Mapping(target = "sizeBytes", expression = "java( size( source.getSizeBytes() ) )")
    @Mapping(target = "mimeType", source = "mimeType")
    @Mapping(target = "contentSecurityPolicy", source = "contentSecurityPolicy")
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "allowedOrigins", ignore = true)
    @Mapping(target = "createdBy", source = "createUserId")
    @Mapping(target = "createdAt", source = "createTime")
    McpArtifactMetadataBO toBusiness(McpArtifactMetadataRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把制品元数据载体写回行模型；端口编号按十进制写入技术主键，
     * {@code status} 与两个 jsonb 集合列由外层转换器负责，受保护技术列全部忽略。
     * English summary: Executes the toRow operation, writing the artifact carrier back onto the row model; the port identifier is written
     * decimal-first into the technical primary key, {@code status} and the two jsonb collection columns belong to the enclosing
     * converter, and every protected technical column stays ignored.
     *
     * 用法 / Usage: 仅由 {@link McpArtifactMetadataPersistenceConverter#toPersistence(McpArtifactMetadataBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "gatewayGroupId", expression = "java( identifier( source.getGatewayGroupId() ) )")
    @Mapping(target = "appCode", source = "appCode")
    @Mapping(target = "appVersion", source = "version")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "resourceUri", source = "resourceUri")
    @Mapping(target = "artifactReference", source = "artifactReference")
    @Mapping(target = "artifactSha256", source = "sha256")
    @Mapping(target = "sizeBytes", source = "sizeBytes")
    @Mapping(target = "mimeType", source = "mimeType")
    @Mapping(target = "contentSecurityPolicy", source = "contentSecurityPolicy")
    @Mapping(target = "permissionManifest", ignore = true)
    @Mapping(target = "allowedOrigins", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    McpArtifactMetadataRecordPO toRow(McpArtifactMetadataBO source);

    /**
     * 中文说明：执行 text 操作，把 MP 的 {@code bigint} 列按十进制文本投影，{@code null} 保持 {@code null}。
     * English summary: Executes the text operation, projecting an MP {@code bigint} column as decimal text; {@code null} stays
     * {@code null}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 十进制文本；returns the decimal text.
     */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /**
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP {@code bigint}；空白视为未生成，
     * 交由 {@code ASSIGN_ID} 补位，非法文本如实拒绝。
     * English summary: Executes the identifier operation, parsing the business decimal identifier into the MP {@code bigint}; a blank
     * value is treated as not yet generated so {@code ASSIGN_ID} can fill it, while a malformed value is rejected truthfully.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 业务标识；parameter business identifier。
     * @return 返回 主键或 {@code null}；returns the identifier or {@code null}.
     */
    default Long identifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "gateway_mcp_app_artifact identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 size 操作，把可空字节数列投影为载体的 {@code long} 字段，等价原
     * {@code getLong("size_bytes")}（列缺失即 0）。
     * English summary: Executes the size operation, narrowing a nullable byte-count column onto the {@code long} carrier field,
     * equivalent to the legacy {@code getLong("size_bytes")} which yields 0 for an absent column.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 列值；parameter column value。
     * @return 返回 字节数；returns the byte count.
     */
    default long size(Long value) {
        return value == null ? 0L : value;
    }
}
