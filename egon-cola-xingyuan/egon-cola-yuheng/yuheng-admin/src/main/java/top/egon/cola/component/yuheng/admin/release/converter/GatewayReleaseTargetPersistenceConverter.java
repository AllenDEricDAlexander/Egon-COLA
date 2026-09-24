package top.egon.cola.component.yuheng.admin.release.converter;

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
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseTargetBO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetRecordPO;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;

/**
 * 中文说明：{@code GatewayReleaseTargetPersistenceConverter} 是 gateway_release_target 聚合在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link GatewayReleaseTargetRecordPO} 行模型与 {@link GatewayReleaseTargetBO} 业务载体之间的双向映射；
 * {@code engine_role} 写方向存 {@code GatewayEngineRoleEnum} 枚举名（绝不 ordinal），读方向沿用原
 * {@code fromWire(...).orElse(null)} 语义——未知或空白角色一律投影为 {@code null} 而不推断；
 * {@code observed_at} 保持 {@code java.time.Instant}。
 * English summary: {@code GatewayReleaseTargetPersistenceConverter} is the only MapStruct converter at the persistence boundary of the
 * gateway_release_target aggregate, mapping {@link GatewayReleaseTargetRecordPO} rows and {@link GatewayReleaseTargetBO} carriers both
 * ways. The write direction stores {@code engine_role} as the {@code GatewayEngineRoleEnum} name (never its ordinal) while the read
 * direction keeps the legacy {@code fromWire(...).orElse(null)} semantics, so an unknown or blank role projects to {@code null} instead of
 * being guessed; {@code observed_at} stays a {@code java.time.Instant}.
 *
 * 用法 / Usage: 由 gateway_release_target 的受守卫 MP 仓储注入使用（bean 名 {@code gatewayReleaseTargetPersistenceConverter}）；
 * 行的业务身份是 {@code (release_id, attempt_no, instance_id, lease_id)}，父键不在载体内，因此写方向忽略
 * {@code releaseId}/{@code attemptNo}，由仓储按所属尝试显式补齐。
 * Injected by the guarded gateway_release_target repository under the bean name {@code gatewayReleaseTargetPersistenceConverter}; a row is
 * keyed by {@code (release_id, attempt_no, instance_id, lease_id)}, so the parent columns are ignored on write and filled by the
 * repository from the owning attempt.
 */
@Slf4j
@Component("gatewayReleaseTargetPersistenceConverter")
public class GatewayReleaseTargetPersistenceConverter implements BaseConverter<
        GatewayReleaseTargetRecordPO,
        GatewayReleaseTargetBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayReleaseTargetMapping MAPPING =
            Mappers.getMapper(GatewayReleaseTargetMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把实例目标载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering an instance target carrier into the MyBatis-Plus row model, leaving
     * the parent release key, the attempt number, the identifier and every protected technical column to the guarded persistence layer.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseTargetPersistenceConverter.toPersistence(targetBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayReleaseTargetRecordPO toPersistence(GatewayReleaseTargetBO carrier) {
        if (carrier == null) {
            log.debug("gateway_release_target business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把实例目标行投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting an instance target row onto the business carrier, restoring the
     * engine role through the tolerant wire parser and keeping the observed instant as {@code java.time.Instant}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseTargetPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayReleaseTargetBO toBusiness(GatewayReleaseTargetRecordPO row) {
        if (row == null) {
            log.debug("gateway_release_target row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染目标载体。
     * English summary: Executes the toPersistenceList operation, rendering every target carrier in order; null or empty input yields an
     * empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseTargetPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayReleaseTargetRecordPO> toPersistenceList(
            List<GatewayReleaseTargetBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影目标行。
     * English summary: Executes the toBusinessList operation, projecting every target row in order; null or empty input yields an empty
     * list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseTargetPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayReleaseTargetBO> toBusinessList(
            List<GatewayReleaseTargetRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseTargetPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayReleaseTargetBO toTarget(GatewayReleaseTargetRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseTargetPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayReleaseTargetRecordPO toSource(GatewayReleaseTargetBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，
     * 父键、尝试序号、技术主键与租户、审计、软删、版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; the parent key, the attempt number, the technical identifier and the tenant, audit, soft-delete and version columns stay
     * empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleaseTargetPersistenceConverter.newRow(targetBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayReleaseTargetRecordPO newRow(GatewayReleaseTargetBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayReleaseTargetMapping} 是 gateway_release_target 的 MapStruct 结构映射契约，逐列复刻被替换的手写
 * JDBC 语义：{@code engine_role} 可空且未知值不推断，{@code applied_version}/{@code applied_artifact_sha256}/
 * {@code error_code} 原样透传可空文本；{@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code GatewayReleaseTargetMapping} is the MapStruct structural contract for gateway_release_target, mirroring the
 * hand-written JDBC it replaces: {@code engine_role} is nullable with no inferred fallback, and {@code applied_version},
 * {@code applied_artifact_sha256} and {@code error_code} pass through as nullable values; {@code unmappedTargetPolicy=ERROR} forces every
 * new column to be stated explicitly here.
 *
 * 用法 / Usage: 由 {@link GatewayReleaseTargetPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayReleaseTargetPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayReleaseTargetMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayReleaseTargetBO} 的字段顺序投影目标行。
     * English summary: Executes the toBusiness operation, projecting a target row onto {@code GatewayReleaseTargetBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayReleaseTargetPersistenceConverter#toBusiness(GatewayReleaseTargetRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "instanceId", source = "instanceId")
    @Mapping(target = "leaseId", source = "leaseId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "appliedVersion", source = "appliedVersion")
    @Mapping(target = "appliedArtifactSha256", source = "appliedArtifactSha256")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "observedAt", source = "observedAt")
    @Mapping(target = "engineRole", expression = "java( engineRole( source.getEngineRole() ) )")
    GatewayReleaseTargetBO toBusiness(GatewayReleaseTargetRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把目标载体写回行模型；父键与受保护技术列全部忽略，因为
     * {@code EgonColaMetaObjectHandler} 独占租户、审计、软删与版本列，而 {@code release_id}/{@code attempt_no} 来自所属尝试。
     * English summary: Executes the toRow operation, writing the target carrier back onto the row model; the parent key and every
     * protected technical column stay ignored, because {@code EgonColaMetaObjectHandler} owns the tenant, audit, soft-delete and version
     * columns while {@code release_id} and {@code attempt_no} come from the owning attempt.
     *
     * 用法 / Usage: 仅由 {@link GatewayReleaseTargetPersistenceConverter#toPersistence(GatewayReleaseTargetBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "instanceId", source = "instanceId")
    @Mapping(target = "leaseId", source = "leaseId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "appliedVersion", source = "appliedVersion")
    @Mapping(target = "appliedArtifactSha256", source = "appliedArtifactSha256")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "observedAt", source = "observedAt")
    @Mapping(target = "engineRole", expression = "java( engineRoleName( source.getEngineRole() ) )")
    @Mapping(target = "releaseId", ignore = true)
    @Mapping(target = "attemptNo", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayReleaseTargetRecordPO toRow(GatewayReleaseTargetBO source);

    /**
     * 中文说明：执行 engineRole 操作，按原实现的宽容解析把 {@code engine_role} 列还原为角色枚举：先去除首尾空格，
     * 缺失或未知值投影为 {@code null}，绝不推断为任一角色。
     * English summary: Executes the engineRole operation, restoring the {@code engine_role} column with the legacy tolerant parser: the
     * value is trimmed, and a missing or unknown role projects to {@code null} instead of being guessed.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 存储的角色名；parameter stored role name。
     * @return 返回 引擎角色或 {@code null}；returns the engine role or {@code null}.
     */
    default GatewayEngineRoleEnum engineRole(String value) {
        return GatewayEngineRoleEnum.fromWire(value).orElse(null);
    }

    /**
     * 中文说明：执行 engineRoleName 操作，按枚举名写回 {@code engine_role} 列，与原
     * {@code role == null ? null : role.name()} 绑定相同，绝不写入 ordinal。
     * English summary: Executes the engineRoleName operation, writing the {@code engine_role} column as the enum name exactly like the
     * legacy {@code role == null ? null : role.name()} binding, and never as an ordinal.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 引擎角色；parameter engine role。
     * @return 返回 角色名或 {@code null}；returns the role name or {@code null}.
     */
    default String engineRoleName(GatewayEngineRoleEnum value) {
        return value == null ? null : value.name();
    }
}
