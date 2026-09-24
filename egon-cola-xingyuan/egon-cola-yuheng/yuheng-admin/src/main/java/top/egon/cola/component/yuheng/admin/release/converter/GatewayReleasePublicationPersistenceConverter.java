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
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleasePublicationBO;
import top.egon.cola.component.yuheng.admin.release.domain.dto.GatewayPublicationScopeDTO;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationPhaseEnum;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleasePublicationRecordPO;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;

/**
 * 中文说明：{@code GatewayReleasePublicationPersistenceConverter} 是 gateway_release_publication 发布日志在持久边界唯一的
 * MapStruct 转换器，负责 {@link GatewayReleasePublicationRecordPO} 行模型与 {@link GatewayReleasePublicationBO} 业务载体之间的
 * 双向映射；{@code phase_type} 与 {@code ddc_status} 按枚举名读写（绝不 ordinal），{@code content_value} 是原样存取的 TEXT 列
 * 而非 JSON，{@code content_sha256} 由业务侧计算、本转换器只做透传，冻结的四列目标 {@code target_role}/{@code target_biz_code}/
 * {@code target_env}/{@code target_app_code} 组合成单个 {@code GatewayPublicationScopeDTO}，目标角色为空即代表「无目标证据」的历史行，
 * 此时投影为 {@code null} 而不推断；createdAt/updatedAt 只读投影自 MP 审计列。
 * English summary: {@code GatewayReleasePublicationPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * the gateway_release_publication journal, mapping {@link GatewayReleasePublicationRecordPO} rows and
 * {@link GatewayReleasePublicationBO} carriers both ways. {@code phase_type} and {@code ddc_status} are stored as enum names (never
 * ordinals), {@code content_value} is a plain TEXT column rather than JSON, {@code content_sha256} is computed by the business layer and
 * only passed through here, and the four frozen target columns {@code target_role}/{@code target_biz_code}/{@code target_env}/
 * {@code target_app_code} collapse into a single {@code GatewayPublicationScopeDTO}: a null target role means a historical row without
 * target evidence and projects to {@code null} instead of being inferred; createdAt/updatedAt are read-only projections of the MP audit
 * columns.
 *
 * 用法 / Usage: 由 gateway_release_publication 的受守卫 MP 仓储注入使用（bean 名
 * {@code gatewayReleasePublicationPersistenceConverter}）；写方向不触碰租户、审计、软删与 MP version 列，
 * 因为它们由 {@code EgonColaMetaObjectHandler} 独占，而业务 CAS 列 {@code expected_version}/{@code ddc_target_version} 与 MP 版本互不替代。
 * Injected by the guarded gateway_release_publication repository under the bean name
 * {@code gatewayReleasePublicationPersistenceConverter}; the write direction never touches the tenant, audit, soft-delete or MP version
 * columns because {@code EgonColaMetaObjectHandler} owns them, while the business CAS columns {@code expected_version} and
 * {@code ddc_target_version} stay independent of the MP version.
 */
@Slf4j
@Component("gatewayReleasePublicationPersistenceConverter")
public class GatewayReleasePublicationPersistenceConverter implements BaseConverter<
        GatewayReleasePublicationRecordPO,
        GatewayReleasePublicationBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final GatewayReleasePublicationMapping MAPPING =
            Mappers.getMapper(GatewayReleasePublicationMapping.class);

    /**
     * 中文说明：执行 toPersistence 操作，把发布阶段载体渲染为 MP 行模型。
     * English summary: Executes the toPersistence operation, rendering a publication phase carrier into the MyBatis-Plus row model,
     * flattening the frozen target scope into its four columns and leaving the identifier and every protected technical column empty.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePublicationPersistenceConverter.toPersistence(publicationBO)}。
     * 传入 {@code null} 返回 {@code null}。Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public GatewayReleasePublicationRecordPO toPersistence(GatewayReleasePublicationBO carrier) {
        if (carrier == null) {
            log.debug("gateway_release_publication business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusiness 操作，把发布阶段行投影为业务载体。
     * English summary: Executes the toBusiness operation, projecting a publication phase row onto the business carrier, restoring both
     * enums from their stored names and rebuilding the frozen target scope from the four target columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePublicationPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public GatewayReleasePublicationBO toBusiness(GatewayReleasePublicationRecordPO row) {
        if (row == null) {
            log.debug("gateway_release_publication row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染发布阶段载体。
     * English summary: Executes the toPersistenceList operation, rendering every publication phase carrier in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePublicationPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<GatewayReleasePublicationRecordPO> toPersistenceList(
            List<GatewayReleasePublicationBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影发布阶段行。
     * English summary: Executes the toBusinessList operation, projecting every publication phase row in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePublicationPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<GatewayReleasePublicationBO> toBusinessList(
            List<GatewayReleasePublicationRecordPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePublicationPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public GatewayReleasePublicationBO toTarget(GatewayReleasePublicationRecordPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePublicationPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public GatewayReleasePublicationRecordPO toSource(GatewayReleasePublicationBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」，
     * 技术主键与租户、审计、软删、版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business columns
     * only; the technical identifier and the tenant, audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code gatewayReleasePublicationPersistenceConverter.newRow(publicationBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public GatewayReleasePublicationRecordPO newRow(GatewayReleasePublicationBO carrier) {
        return toPersistence(carrier);
    }
}

/**
 * 中文说明：{@code GatewayReleasePublicationMapping} 是 gateway_release_publication 的 MapStruct 结构映射契约，逐列复刻被替换的
 * 手写 JDBC 语义：{@code release_id} 以十进制文本往返，{@code attempt_no} 在 MP {@code Long} 列与 {@code int} 载体列之间显式换算，
 * 四个目标列与 {@code GatewayPublicationScopeDTO} 之间是「整组存在或整组为空」的扁平化关系，解码目标角色时沿用原
 * {@code GatewayEngineRoleEnum.valueOf(...)} 的严格语义；{@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态。
 * English summary: {@code GatewayReleasePublicationMapping} is the MapStruct structural contract for gateway_release_publication,
 * mirroring the hand-written JDBC it replaces: {@code release_id} round-trips as decimal text, {@code attempt_no} converts explicitly
 * between the MP {@code Long} column and the {@code int} carrier field, the four target columns and {@code GatewayPublicationScopeDTO}
 * relate as "the whole group is present or the whole group is null", and restoring the target role keeps the strict
 * {@code GatewayEngineRoleEnum.valueOf(...)} semantics of the original read; {@code unmappedTargetPolicy=ERROR} forces every new column to
 * be stated explicitly here.
 *
 * 用法 / Usage: 由 {@link GatewayReleasePublicationPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link GatewayReleasePublicationPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a
 * bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface GatewayReleasePublicationMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code GatewayReleasePublicationBO} 的字段顺序投影发布阶段行。
     * English summary: Executes the toBusiness operation, projecting a publication phase row onto {@code GatewayReleasePublicationBO}.
     *
     * 用法 / Usage: 仅由 {@link GatewayReleasePublicationPersistenceConverter#toBusiness(GatewayReleasePublicationRecordPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "releaseId", expression = "java( text( source.getReleaseId() ) )")
    @Mapping(target = "attemptNo", expression = "java( attemptNo( source.getAttemptNo() ) )")
    @Mapping(target = "phaseOrder", source = "phaseOrder")
    @Mapping(target = "phaseType", expression = "java( phaseType( source.getPhaseType() ) )")
    @Mapping(target = "configKey", source = "configKey")
    @Mapping(target = "contentValue", source = "contentValue")
    @Mapping(target = "contentSha256", source = "contentSha256")
    @Mapping(target = "expectedVersion", source = "expectedVersion")
    @Mapping(target = "changeId", source = "changeId")
    @Mapping(target = "ddcTargetVersion", source = "ddcTargetVersion")
    @Mapping(target = "status", expression = "java( publicationStatus( source.getDdcStatus() ) )")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    @Mapping(target = "targetScope", expression = "java( targetScope( source ) )")
    GatewayReleasePublicationBO toBusiness(GatewayReleasePublicationRecordPO source);

    /**
     * 中文说明：执行 toRow 操作，把发布阶段载体写回行模型；受保护技术列全部忽略，因为
     * {@code EgonColaMetaObjectHandler} 独占租户、审计、软删与 MP 版本列。
     * English summary: Executes the toRow operation, writing the publication phase carrier back onto the row model; every protected
     * technical column stays ignored because {@code EgonColaMetaObjectHandler} owns the tenant, audit, soft-delete and MP version
     * columns.
     *
     * 用法 / Usage: 仅由 {@link GatewayReleasePublicationPersistenceConverter#toPersistence(GatewayReleasePublicationBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "releaseId", expression = "java( identifier( source.getReleaseId() ) )")
    @Mapping(target = "attemptNo", expression = "java( attemptColumn( source.getAttemptNo() ) )")
    @Mapping(target = "phaseOrder", source = "phaseOrder")
    @Mapping(target = "phaseType", expression = "java( phaseTypeName( source.getPhaseType() ) )")
    @Mapping(target = "configKey", source = "configKey")
    @Mapping(target = "contentValue", source = "contentValue")
    @Mapping(target = "contentSha256", source = "contentSha256")
    @Mapping(target = "expectedVersion", source = "expectedVersion")
    @Mapping(target = "changeId", source = "changeId")
    @Mapping(target = "ddcTargetVersion", source = "ddcTargetVersion")
    @Mapping(target = "ddcStatus", expression = "java( publicationStatusName( source.getStatus() ) )")
    @Mapping(target = "errorCode", source = "errorCode")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "targetRole", expression = "java( targetRoleName( source.getTargetScope() ) )")
    @Mapping(target = "targetBizCode", expression = "java( targetBizCode( source.getTargetScope() ) )")
    @Mapping(target = "targetEnv", expression = "java( targetEnv( source.getTargetScope() ) )")
    @Mapping(target = "targetAppCode", expression = "java( targetAppCode( source.getTargetScope() ) )")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    GatewayReleasePublicationRecordPO toRow(GatewayReleasePublicationBO source);

    /**
     * 中文说明：执行 text 操作，把 MP 的 bigint 父键按十进制文本投影，等价于原 JDBC 的 {@code getString("release_id")}。
     * English summary: Executes the text operation, projecting the MP bigint parent key as decimal text, exactly what the legacy
     * {@code getString("release_id")} produced; {@code null} stays {@code null}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 父键；parameter parent key.
     * @return 返回 十进制文本；returns the decimal text.
     */
    default String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /**
     * 中文说明：执行 identifier 操作，把业务十进制发布标识解析为 MP bigint；空白视为无法归属，交由 NOT NULL 约束拒绝。
     * English summary: Executes the identifier operation, parsing the business decimal release identifier into the MP bigint; a blank
     * value stays unresolved and the NOT NULL column rejects it, as the legacy insert did.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 业务标识；parameter business identifier。
     * @return 返回 父键；returns the parent key.
     */
    default Long identifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "gateway release publication identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 attemptNo 操作，把 {@code attempt_no} 的 bigint 列投影为载体的 {@code int} 字段，
     * 等价于原 {@code getInt("attempt_no")}（列缺失即 0）。
     * English summary: Executes the attemptNo operation, narrowing the bigint {@code attempt_no} column onto the {@code int} carrier
     * field, equivalent to the legacy {@code getInt("attempt_no")} which yields 0 for an absent value.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 尝试序号列；parameter stored attempt number.
     * @return 返回 尝试序号；returns the attempt number.
     */
    default int attemptNo(Long value) {
        return value == null ? 0 : value.intValue();
    }

    /**
     * 中文说明：执行 attemptColumn 操作，把载体的 {@code int} 尝试序号写回 bigint 列，与 {@code CHECK (attempt_no > 0)} 无关，
     * 该业务约束仍由数据库与状态机负责。
     * English summary: Executes the attemptColumn operation, widening the {@code int} attempt number onto the bigint column; the
     * {@code CHECK (attempt_no > 0)} invariant stays enforced by the database and the state machine.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 尝试序号；parameter attempt number.
     * @return 返回 尝试序号列；returns the stored attempt number.
     */
    default Long attemptColumn(int value) {
        return Long.valueOf(value);
    }

    /**
     * 中文说明：执行 phaseType 操作，按存储的枚举名还原 {@code GatewayPublicationPhaseEnum}，与原
     * {@code valueOf(...)} 相同：空值保持为空，未知名抛错而不兜底。
     * English summary: Executes the phaseType operation, restoring {@code GatewayPublicationPhaseEnum} from the stored name exactly as
     * the legacy {@code valueOf(...)} did: {@code null} stays {@code null} and an unknown name fails instead of falling back.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 阶段名；parameter stored phase name。
     * @return 返回 阶段枚举或 {@code null}；returns the phase enum or {@code null}.
     */
    default GatewayPublicationPhaseEnum phaseType(String value) {
        return value == null ? null : GatewayPublicationPhaseEnum.valueOf(value);
    }

    /**
     * 中文说明：执行 phaseTypeName 操作，按枚举名写回 {@code phase_type} 列，绝不写入 ordinal。
     * English summary: Executes the phaseTypeName operation, writing {@code phase_type} as the enum name and never as an ordinal.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 阶段枚举；parameter phase enum。
     * @return 返回 阶段名或 {@code null}；returns the phase name or {@code null}.
     */
    default String phaseTypeName(GatewayPublicationPhaseEnum value) {
        return value == null ? null : value.name();
    }

    /**
     * 中文说明：执行 publicationStatus 操作，按 {@code ddc_status} 存储的枚举名还原
     * {@code GatewayPublicationStatusEnum}，沿用原 {@code valueOf(...)} 的严格语义。
     * English summary: Executes the publicationStatus operation, restoring {@code GatewayPublicationStatusEnum} from the stored
     * {@code ddc_status} name with the strict semantics of the original {@code valueOf(...)}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 DDC 状态名；parameter stored DDC status。
     * @return 返回 状态枚举或 {@code null}；returns the status enum or {@code null}.
     */
    default GatewayPublicationStatusEnum publicationStatus(String value) {
        return value == null ? null : GatewayPublicationStatusEnum.valueOf(value);
    }

    /**
     * 中文说明：执行 publicationStatusName 操作，按枚举名写回 {@code ddc_status} 列，与原 {@code status.name()} 绑定相同。
     * English summary: Executes the publicationStatusName operation, writing {@code ddc_status} as the enum name exactly like the legacy
     * {@code status.name()} binding.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param value 参数 状态枚举；parameter status enum。
     * @return 返回 状态名或 {@code null}；returns the status name or {@code null}.
     */
    default String publicationStatusName(GatewayPublicationStatusEnum value) {
        return value == null ? null : value.name();
    }

    /**
     * 中文说明：执行 targetScope 操作，把四列冻结目标重建为 {@code GatewayPublicationScopeDTO}；
     * {@code target_role} 为空即历史无证据行，返回 {@code null} 而不推断，其余列的空白与长度校验由 DTO 自身负责。
     * English summary: Executes the targetScope operation, rebuilding the four frozen target columns into a
     * {@code GatewayPublicationScopeDTO}; a null {@code target_role} marks a historical row without evidence and returns {@code null}
     * instead of inferring one, while the DTO itself enforces the blank and trimming rules of the remaining columns.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param source 参数 行模型；parameter row model。
     * @return 返回 目标范围或 {@code null}；returns the target scope or {@code null}.
     */
    default GatewayPublicationScopeDTO targetScope(GatewayReleasePublicationRecordPO source) {
        return source.getTargetRole() == null
                ? null
                : new GatewayPublicationScopeDTO(
                        source.getTargetBizCode(),
                        source.getTargetEnv(),
                        source.getTargetAppCode(),
                        GatewayEngineRoleEnum.valueOf(source.getTargetRole())
                );
    }

    /**
     * 中文说明：执行 targetRoleName 操作，把冻结角色的枚举名写回 {@code target_role} 列；缺失目标范围时按原实现直接失败，
     * 不伪造任何角色。
     * English summary: Executes the targetRoleName operation, writing the frozen role name into {@code target_role}; an absent target
     * scope fails exactly as the legacy insert did and no role is fabricated.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param scope 参数 目标范围；parameter target scope。
     * @return 返回 角色名；returns the role name.
     */
    default String targetRoleName(GatewayPublicationScopeDTO scope) {
        return scope.engineRole().name();
    }

    /**
     * 中文说明：执行 targetBizCode 操作，把冻结业务码写回 {@code target_biz_code} 列。
     * English summary: Executes the targetBizCode operation, writing the frozen business code into {@code target_biz_code}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param scope 参数 目标范围；parameter target scope。
     * @return 返回 业务码；returns the biz code.
     */
    default String targetBizCode(GatewayPublicationScopeDTO scope) {
        return scope.bizCode();
    }

    /**
     * 中文说明：执行 targetEnv 操作，把冻结环境写回 {@code target_env} 列。
     * English summary: Executes the targetEnv operation, writing the frozen environment into {@code target_env}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param scope 参数 目标范围；parameter target scope。
     * @return 返回 环境标识；returns the environment.
     */
    default String targetEnv(GatewayPublicationScopeDTO scope) {
        return scope.env();
    }

    /**
     * 中文说明：执行 targetAppCode 操作，把冻结应用码写回 {@code target_app_code} 列。
     * English summary: Executes the targetAppCode operation, writing the frozen application code into {@code target_app_code}.
     *
     * 用法 / Usage: 映射表达式内部使用。/ Used from the mapping expressions only.
     * @param scope 参数 目标范围；parameter target scope。
     * @return 返回 应用码；returns the app code.
     */
    default String targetAppCode(GatewayPublicationScopeDTO scope) {
        return scope.appCode();
    }
}
