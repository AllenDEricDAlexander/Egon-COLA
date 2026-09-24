package top.egon.cola.component.yuheng.admin.llm.converter;

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
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmChannelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.po.LlmChannelPO;

/**
 * 中文说明：{@code LlmChannelPersistenceConverter} 是 {@code gateway_llm_channel} 在持久边界唯一的 MapStruct 转换器，
 * 负责 {@link LlmChannelPO} 行模型与 {@link LlmChannelBO} 业务载体之间的双向映射：不透明主键以十进制文本往返，
 * 业务稳定 key 与库列 {@code channel_key} 同名同形往返，业务 {@code revision} 与 MP 乐观锁 {@code version} 各自独立，
 * {@code createdAt}/{@code updatedAt} 只读地投影自 {@code create_time}/{@code update_time}；
 * 租户、创建与更新操作者、软删与技术版本列一律不由业务载体写入，封闭列按 {@code @EnumValue} 枚举而非 ordinal 落库。
 * English summary: {@code LlmChannelPersistenceConverter} is the only MapStruct converter at the persistence boundary of
 * {@code gateway_llm_channel}, mapping {@link LlmChannelPO} rows and {@link LlmChannelBO} carriers both ways: the opaque
 * key round-trips as decimal text, the business stable key and the {@code channel_key} column cross over one-to-one, the
 * business {@code revision} stays separate from the optimistic-lock {@code version}, and the audit instants are read-only
 * projections of {@code create_time} and {@code update_time}; tenant, both operator columns, soft delete and the technical
 * version are never written from the business carrier, and closed columns persist as {@code @EnumValue} enums.
 *
 * 用法 / Usage: 由 {@code MpLlmConfigurationRepository} 注入使用（bean 名 {@code llmChannelPersistenceConverter}）；
 * {@code newRow} 产出只带业务列的待插入行，{@code applyBusiness} 在已加载活跃行上整行覆盖可写业务列并保留定位键与
 * 受保护元数据，供乐观锁 CAS 复用。行模型只能出现在本转换器与受守卫仓储之间，禁止进入端口或服务签名。
 * Injected by the guarded channel store under the bean name {@code llmChannelPersistenceConverter}; {@code newRow} yields a
 * business-columns-only insert candidate and {@code applyBusiness} overwrite-replaces the writable columns of a loaded
 * active row while keeping the locator key and the protected metadata for the CAS path. A row model may never reach a
 * port or a service signature.
 */
@Slf4j
@Component("llmChannelPersistenceConverter")
public class LlmChannelPersistenceConverter implements BaseConverter<
        LlmChannelPO,
        LlmChannelBO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final LlmChannelMapping MAPPING =
            Mappers.getMapper(LlmChannelMapping.class);

    /**
     * 中文说明：执行 toBusiness 操作，把渠道行模型投影为业务载体，含只读审计时刻与业务 revision。
     * English summary: Executes the toBusiness operation, projecting a channel row onto the business carrier including the
     * read-only audit instants and the business revision.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmChannelPersistenceConverter.toBusiness(row)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    public LlmChannelBO toBusiness(LlmChannelPO row) {
        if (row == null) {
            log.debug("gateway_llm_channel row is absent; no business carrier projected");
            return null;
        }
        return MAPPING.toBusiness(row);
    }

    /**
     * 中文说明：执行 toPersistence 操作，把业务载体渲染为 MP 行模型，租户、审计、软删与版本列留给受守卫边界补齐。
     * English summary: Executes the toPersistence operation, rendering a channel carrier into the MyBatis-Plus row model while
     * leaving the tenant, audit, soft-delete and version columns for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmChannelPersistenceConverter.toPersistence(channelBO)}。传入 {@code null} 返回 {@code null}。
     * Passing {@code null} returns {@code null}.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    public LlmChannelPO toPersistence(LlmChannelBO carrier) {
        if (carrier == null) {
            log.debug("gateway_llm_channel business carrier is absent; no row rendered");
            return null;
        }
        return MAPPING.toRow(carrier);
    }

    /**
     * 中文说明：执行 toBusinessList 操作，按列表顺序投影渠道行；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toBusinessList operation, projecting every channel row in order; null or empty input yields
     * an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmChannelPersistenceConverter.toBusinessList(rows)}。
     * @param rows 参数 行模型列表；parameter row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    public List<LlmChannelBO> toBusinessList(List<LlmChannelPO> rows) {
        return toTargetList(rows);
    }

    /**
     * 中文说明：执行 toPersistenceList 操作，按列表顺序渲染渠道载体；null 或空输入返回空列表而非 {@code null}。
     * English summary: Executes the toPersistenceList operation, rendering every channel carrier in order; null or empty input
     * yields an empty list and never {@code null}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmChannelPersistenceConverter.toPersistenceList(carriers)}。
     * @param carriers 参数 业务载体列表；parameter business carriers.
     * @return 返回 行模型列表；returns the row models.
     */
    public List<LlmChannelPO> toPersistenceList(List<LlmChannelBO> carriers) {
        return toSourceList(carriers);
    }

    /**
     * 中文说明：执行 toTarget 操作，即 {@code BaseConverter} 的正向投影（行模型 -> 业务载体）。
     * English summary: Executes the toTarget operation, the forward projection of {@code BaseConverter} (row model to business
     * carrier).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmChannelPersistenceConverter.toTarget(row)}。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @Override
    public LlmChannelBO toTarget(LlmChannelPO source) {
        return toBusiness(source);
    }

    /**
     * 中文说明：执行 toSource 操作，即 {@code BaseConverter} 的反向渲染（业务载体 -> 行模型）。
     * English summary: Executes the toSource operation, the reverse rendering of {@code BaseConverter} (business carrier to row
     * model).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmChannelPersistenceConverter.toSource(carrier)}。
     * @param target 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @Override
    public LlmChannelPO toSource(LlmChannelBO target) {
        return toPersistence(target);
    }

    /**
     * 中文说明：执行 newRow 操作，是 {@code toPersistence} 的插入别名，语义为「只带业务列的新行」：定位键
     * {@code channelKey} 与全部业务列取自可信载体，租户、审计、软删与版本列一律留空交由受守卫边界补齐。
     * English summary: Executes the newRow operation, an insert alias of {@code toPersistence} producing a row carrying business
     * columns only: the {@code channelKey} locator and the business columns come from the trusted carrier while the tenant,
     * audit, soft-delete and version columns stay empty for the guarded boundary to fill.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmChannelPersistenceConverter.newRow(channelBO)}。
     * @param carrier 参数 业务载体；parameter business carrier。
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public LlmChannelPO newRow(LlmChannelBO carrier) {
        return toPersistence(carrier);
    }

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载的活跃行上整行覆盖可写业务列，保留定位键 {@code channelKey}、
     * 主键与受保护元数据，供 {@code revision} 递增后的乐观锁 CAS 更新使用。
     * English summary: Executes the applyBusiness operation, overwrite-replacing the writable business columns of a loaded active
     * row while keeping the {@code channelKey} locator, the primary key and the protected metadata, for the optimistic-lock
     * CAS update after the business revision advances.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmChannelPersistenceConverter.applyBusiness(channelBO, row)}。
     * 任一入参为 {@code null} 时如实不处理。/ Either argument being {@code null} is a no-op.
     * @param carrier 参数 业务载体；parameter business carrier。
     * @param row 参数 已加载行模型；parameter the loaded row model.
     */
    public void applyBusiness(LlmChannelBO carrier, LlmChannelPO row) {
        if (carrier == null || row == null) {
            log.debug("gateway_llm_channel carrier or row is absent; nothing applied");
            return;
        }
        MAPPING.applyBusiness(carrier, row);
    }
}

/**
 * 中文说明：{@code LlmChannelMapping} 是 gateway_llm_channel 的 MapStruct 结构映射契约，逐列声明读写形态；
 * {@code unmappedTargetPolicy=ERROR} 保证任何新增列都必须在此显式表态，受保护技术列一律 {@code ignore}。
 * English summary: {@code LlmChannelMapping} is the MapStruct structural contract for gateway_llm_channel, stating every column
 * read and write shape; {@code unmappedTargetPolicy=ERROR} forces every new column to be declared here while the protected
 * technical columns stay ignored.
 *
 * 用法 / Usage: 由 {@link LlmChannelPersistenceConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link LlmChannelPersistenceConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface LlmChannelMapping {

    /**
     * 中文说明：执行 toBusiness 操作，按 {@code LlmChannelBO} 的字段顺序投影 gateway_llm_channel 行。
     * English summary: Executes the toBusiness operation, projecting a gateway_llm_channel row onto {@code LlmChannelBO}.
     *
     * 用法 / Usage: 仅由 {@link LlmChannelPersistenceConverter#toBusiness(LlmChannelPO)} 调用。
     * @param source 参数 行模型；parameter row model。
     * @return 返回 业务载体；returns the business carrier.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( text( source.getId() ) )")
    @Mapping(target = "channelKey", source = "channelKey")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "deployment", source = "deployment")
    @Mapping(target = "protocol", source = "protocol")
    @Mapping(target = "baseUrl", source = "baseUrl")
    @Mapping(target = "secretRef", source = "secretRef")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "connectTimeoutMs", source = "connectTimeoutMs")
    @Mapping(target = "headerTimeoutMs", source = "headerTimeoutMs")
    @Mapping(target = "idleTimeoutMs", source = "idleTimeoutMs")
    @Mapping(target = "totalTimeoutMs", source = "totalTimeoutMs")
    @Mapping(target = "maxConcurrent", source = "maxConcurrent")
    @Mapping(target = "revision", expression = "java( value( source.getRevision() ) )")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    LlmChannelBO toBusiness(LlmChannelPO source);

    /**
     * 中文说明：执行 toRow 操作，把业务载体写回 gateway_llm_channel 行；主键按十进制文本解析，
     * 租户、审计、软删与版本列全部忽略。
     * English summary: Executes the toRow operation, writing the carrier back onto a gateway_llm_channel row; the key parses from
     * its decimal text and the tenant, audit, soft-delete and version columns stay ignored.
     *
     * 用法 / Usage: 仅由 {@link LlmChannelPersistenceConverter#toPersistence(LlmChannelBO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @return 返回 行模型；returns the row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", expression = "java( identifier( source.getId() ) )")
    @Mapping(target = "channelKey", source = "channelKey")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "deployment", source = "deployment")
    @Mapping(target = "protocol", source = "protocol")
    @Mapping(target = "baseUrl", source = "baseUrl")
    @Mapping(target = "secretRef", source = "secretRef")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "connectTimeoutMs", source = "connectTimeoutMs")
    @Mapping(target = "headerTimeoutMs", source = "headerTimeoutMs")
    @Mapping(target = "idleTimeoutMs", source = "idleTimeoutMs")
    @Mapping(target = "totalTimeoutMs", source = "totalTimeoutMs")
    @Mapping(target = "maxConcurrent", source = "maxConcurrent")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    LlmChannelPO toRow(LlmChannelBO source);

    /**
     * 中文说明：执行 applyBusiness 操作，在已加载行上覆盖业务列；定位键 {@code channelKey}、主键与受保护元数据保持不变。
     * English summary: Executes the applyBusiness operation, replacing the business columns of a loaded row; the {@code channelKey}
     * locator, the identifier and the protected metadata stay untouched.
     *
     * 用法 / Usage: 仅由 {@link LlmChannelPersistenceConverter#applyBusiness(LlmChannelBO, LlmChannelPO)} 调用。
     * @param source 参数 业务载体；parameter business carrier。
     * @param target 参数 已加载行模型；parameter the loaded row model.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "channelKey", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "deployment", source = "deployment")
    @Mapping(target = "protocol", source = "protocol")
    @Mapping(target = "baseUrl", source = "baseUrl")
    @Mapping(target = "secretRef", source = "secretRef")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "connectTimeoutMs", source = "connectTimeoutMs")
    @Mapping(target = "headerTimeoutMs", source = "headerTimeoutMs")
    @Mapping(target = "idleTimeoutMs", source = "idleTimeoutMs")
    @Mapping(target = "totalTimeoutMs", source = "totalTimeoutMs")
    @Mapping(target = "maxConcurrent", source = "maxConcurrent")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void applyBusiness(LlmChannelBO source,
                       @MappingTarget LlmChannelPO target);

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
                    "gateway_llm_channel identifier must be decimal: " + value,
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
}
