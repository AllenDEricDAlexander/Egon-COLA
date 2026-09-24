package top.egon.cola.component.yuheng.admin.llm.converter;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmChannelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.vo.LlmChannelVO;

/**
 * 中文说明：{@code LlmChannelConverter} 是渠道业务载体的单向出站投影器，把 {@link LlmChannelBO} 投影为
 * 原 API-004/005 的 {@link LlmChannelVO}：业务稳定 key 写作对外 {@code key}，超时、并发与协议按字段直取，
 * {@code secretRef} 只搬运引用名而绝不解析密钥；持久边界特有的不透明主键与审计时刻不进入响应，
 * 因此本投影不可逆，也不提供反向方法。
 * English summary: {@code LlmChannelConverter} is the one-way outbound projection of the channel business carrier, mapping
 * {@link LlmChannelBO} onto the original API-004/005 {@link LlmChannelVO}: the business stable key is emitted as the
 * external {@code key}, the timeouts, concurrency and protocol cross over field by field, and {@code secretRef} carries the
 * reference name only and never a resolved secret; the persistence-boundary identifier and audit instants stay out of the
 * response, so the projection is irreversible and no reverse method is offered.
 *
 * 用法 / Usage: 由 API 投影层在返回渠道列表或单渠道时调用（bean 名 {@code llmChannelConverter}），
 * 传入 {@code null} 如实得到 {@code null} 而不伪造空响应；{@code revision} 必须是仓储推进后的权威值，
 * 因此本投影只在保存或读取完成后使用。/ Injected under the bean name {@code llmChannelConverter} when the API projects a
 * channel page or a single channel; {@code null} input yields {@code null} instead of a fabricated empty response, and
 * {@code revision} has to be the repository-advanced authoritative value, so this projection runs only after a read or a
 * save.
 */
@Slf4j
@Component("llmChannelConverter")
public class LlmChannelConverter implements BaseForwardConverter<
        LlmChannelBO,
        LlmChannelVO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final LlmChannelProjection MAPPING =
            Mappers.getMapper(LlmChannelProjection.class);

    /**
     * 中文说明：执行 toTarget 操作，把渠道业务载体投影为 API-004/005 完整响应；
     * 传入 {@code null} 时如实返回 {@code null} 并留下调试日志，不伪造空投影。
     * English summary: Executes the toTarget operation, projecting the channel carrier onto the API-004/005 response;
     * {@code null} input yields {@code null} with a debug log instead of a fabricated projection.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmChannelConverter.toTarget(channelBO)}。
     * @param source 参数 渠道业务载体；parameter the channel carrier.
     * @return 返回 渠道响应投影；returns the channel projection.
     */
    @Override
    public LlmChannelVO toTarget(LlmChannelBO source) {
        if (source == null) {
            log.debug("LLM channel carrier is absent; no API projection rendered");
            return null;
        }
        return MAPPING.toView(source);
    }

    /**
     * 中文说明：执行 toViewList 操作，按列表顺序投影渠道载体；null 或空输入返回空列表而非 {@code null}，
     * 与分页合同里空页输出 {@code []} 的口径一致。
     * English summary: Executes the toViewList operation, projecting every channel carrier in order; null or empty input yields
     * an empty list rather than {@code null}, matching the empty page answering with {@code []}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmChannelConverter.toViewList(channel carriers)}。
     * @param carriers 参数 渠道业务载体列表；parameter the channel carriers.
     * @return 返回 渠道响应投影列表；returns the channel projections.
     */
    public List<LlmChannelVO> toViewList(List<LlmChannelBO> carriers) {
        return toTargetList(carriers);
    }
}

/**
 * 中文说明：{@code LlmChannelProjection} 是渠道载体到响应的 MapStruct 结构映射契约，
 * {@code unmappedTargetPolicy=ERROR} 强制 {@link LlmChannelVO} 的每个对外字段都显式表态，
 * 因此新增响应字段必须在此说明它来自哪个业务事实，不能靠默认命名静默带出不透明主键或审计时刻。
 * English summary: {@code LlmChannelProjection} is the MapStruct structural contract from the channel carrier to the response,
 * where {@code unmappedTargetPolicy=ERROR} forces every externally visible {@link LlmChannelVO} field to state itself, so a
 * new response field has to declare which business fact it comes from instead of silently leaking the opaque identifier or
 * the audit instants through default naming.
 *
 * 用法 / Usage: 由 {@link LlmChannelConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link LlmChannelConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface LlmChannelProjection {

    /**
     * 中文说明：执行 toView 操作，按 {@link LlmChannelVO} 的字段顺序投影渠道业务载体。
     * English summary: Executes the toView operation, projecting the channel carrier onto {@link LlmChannelVO}.
     *
     * 用法 / Usage: 仅由 {@link LlmChannelConverter#toTarget(LlmChannelBO)} 调用。
     * @param source 参数 渠道业务载体；parameter the channel carrier.
     * @return 返回 渠道响应投影；returns the channel projection.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "key", source = "channelKey")
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
    LlmChannelVO toView(LlmChannelBO source);
}
