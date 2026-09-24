package top.egon.cola.component.yuheng.admin.credential.converter;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.yuheng.admin.credential.domain.bo.GatewayCredentialBO;
import top.egon.cola.component.yuheng.admin.credential.domain.po.GatewayCredentialRecordPO;

/**
 * 中文说明：{@code GatewayCredentialPersistenceConverter} 是 gateway_application_credential 聚合在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link GatewayCredentialRecordPO} 行模型与 {@link GatewayCredentialBO} 业务载体之间的双向映射；
 * 主键与 {@code applicationId} 外键以十进制文本往返，有效期保持 UTC {@code Instant}，{@code status} 是开放 varchar 列故按原 wire
 * 字符串存取；行模型独有的 {@code secretReference} 既不由业务写入也不向业务投影，明文与密钥引用因此不会跨出持久边界。
 * English summary: {@code GatewayCredentialPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * gateway_application_credential, mapping {@link GatewayCredentialRecordPO} rows and {@link GatewayCredentialBO} carriers both ways.
 * The key and the {@code applicationId} foreign key round-trip as decimal text, validity stays a UTC {@code Instant}, the open
 * {@code status} column keeps its original wire string, and the row-only {@code secretReference} is neither written from nor projected
 * onto the business carrier so ciphertext references never cross the persistence boundary.
 *
 * 用法 / Usage: 由 {@code MpGatewayCredentialRepository} 等受守卫仓储注入使用（bean 名 {@code gatewayCredentialPersistenceConverter}），
 * RecordPO 只能出现在本转换器与受守卫仓储之间，禁止进入端口或服务签名。
 * Injected by the guarded credential repository under the bean name {@code gatewayCredentialPersistenceConverter}; a RecordPO may only
 * meet the business carrier inside this converter and the guarded repository, never in a port or service signature.
 */
@Slf4j
@Component("gatewayCredentialPersistenceConverter")
public class GatewayCredentialPersistenceConverter implements BaseConverter<
        GatewayCredentialRecordPO,
        GatewayCredentialBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayCredentialMapping MAPPING =
            Mappers.getMapper(GatewayCredentialMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把凭证业务载体渲染为 MP 行模型；{@code secretReference} 保持未写入（原实现即写 NULL）。
     * English summary: Executes the toPersistence operation, rendering a credential carrier into the MyBatis-Plus row model;
     * {@code secretReference} stays unwritten exactly as the legacy insert bound it to NULL, and the technical columns are left to the
     * guarded persistence layer.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayCredentialPersistenceConverter.toPersistence(credentialBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayCredentialRecordPO toPersistence(GatewayCredentialBO carrier) {
        if (carrier == null) {
            log.debug("gateway credential business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把凭证行模型投影为业务载体，不携带 {@code secretReference}。
     * English summary: Executes the toBusiness operation, projecting a credential row onto the business carrier without the row-only
     * {@code secretReference}; creation and update instants come from the MP audit columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayCredentialPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayCredentialBO toBusiness(GatewayCredentialRecordPO row) {
        if (row == null) {
            log.debug("gateway credential row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染凭证载体。
     * English summary: Executes the toPersistenceList operation, rendering every credential carrier in order; null or empty input yields
     * an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayCredentialPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayCredentialRecordPO> toPersistenceList(
            List<GatewayCredentialBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影凭证行。
     * English summary: Executes the toBusinessList operation, projecting every credential row in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayCredentialPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayCredentialBO> toBusinessList(
            List<GatewayCredentialRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayCredentialPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayCredentialBO toTarget(GatewayCredentialRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayCredentialPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayCredentialRecordPO toSource(GatewayCredentialBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，
     * {@code secretReference} 与租户、审计、软删、版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; {@code secretReference} plus the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayCredentialPersistenceConverter.newRow(credentialBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayCredentialRecordPO newRow(GatewayCredentialBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayCredentialMapping} 是 gateway_application_credential 的 MapStruct 结构映射契约，逐列复刻被替换的手写 JDBC
 * 读写语义（原 SELECT 从不取 {@code secret_reference}，原 INSERT 对其写 NULL），{@code unmappedTargetPolicy=ERROR} 保证新增列必须显式表态。
 * English summary: {@code GatewayCredentialMapping} is the MapStruct structural contract for gateway_application_credential, mirroring
 * the JDBC it replaces column by column (the legacy SELECT never read {@code secret_reference} and the legacy INSERT bound it to NULL);
 * {@code unmappedTargetPolicy=ERROR} forces every new column to be stated here.
 *
 * 用法 / Usage: 由 {@link GatewayCredentialPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayCredentialPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayCredentialMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayCredentialBO} 的字段顺序投影凭证行。
     * English summary: Executes the toBusiness operation, projecting a credential row onto {@code GatewayCredentialBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayCredentialPersistenceConverter#toBusiness(GatewayCredentialRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "applicationId", expression = "java( text( source.getApplicationId() ) )")
    @Mapping(target = "accessKey", source = "accessKey")
    @Mapping(target = "secretCiphertext", source = "secretCiphertext")
    @Mapping(target = "keyVersion", source = "keyVersion")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "validFrom", source = "validFrom")
    @Mapping(target = "validUntil", source = "validUntil")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    GatewayCredentialBO toBusiness(GatewayCredentialRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把凭证业务载体写回行模型；密钥引用与受保护技术列全部忽略。
     * English summary: Executes the toRow operation, writing the credential carrier back onto the row model; the key reference and every
     * protected technical column stay ignored.
     *
     * 用法 / Usage: 仅由 {@link GatewayCredentialPersistenceConverter#toPersistence(GatewayCredentialBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "applicationId", expression = "java( identifier( source.getApplicationId() ) )")
    @Mapping(target = "accessKey", source = "accessKey")
    @Mapping(target = "secretCiphertext", source = "secretCiphertext")
    @Mapping(target = "keyVersion", source = "keyVersion")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "validFrom", source = "validFrom")
    @Mapping(target = "validUntil", source = "validUntil")
    @Mapping(target = "secretReference", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayCredentialRecordPO toRow(GatewayCredentialBO source);

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
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为 MP bigint；空白视为未生成，主键交由 {@code ASSIGN_ID} 补位。
     * English summary: Executes the identifier operation, parsing a business decimal identifier into the MP bigint; a blank value is
     * treated as not yet generated so {@code ASSIGN_ID} can fill the key, while a malformed value is rejected.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 业务标识；parameter business identifier。
     * @return 返回 标识；returns the identifier.
     */
    default Long identifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "gateway credential identifier must be decimal: " + value,
                    failure
            );
        }
    }
}
