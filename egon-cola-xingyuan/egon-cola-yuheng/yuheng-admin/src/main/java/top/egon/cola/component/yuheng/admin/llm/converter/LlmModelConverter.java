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
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmModelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.vo.LlmModelVO;

/**
 * 中文说明：{@code LlmModelConverter} 是模型业务载体的单向出站投影器，把 {@link LlmModelBO} 投影为
 * 原 API-006/007 的 {@link LlmModelVO}：业务稳定 key 写作对外 {@code key}，{@code kind}、协议集合、
 * {@code dimensions}、{@code embeddingSpaceId}、{@code allowedSubjects} 与 {@code routes} 按业务事实直取，
 * 三个 jsonb 列已在持久边界解码为结构化集合，本投影不再触碰任何 JSON 形态；
 * 持久边界特有的不透明主键与审计时刻不进入响应，因此本投影不可逆，也不提供反向方法。
 * English summary: {@code LlmModelConverter} is the one-way outbound projection of the model business carrier, mapping
 * {@link LlmModelBO} onto the original API-006/007 {@link LlmModelVO}: the business stable key is emitted as the external
 * {@code key}, while {@code kind}, the protocol set, {@code dimensions}, {@code embeddingSpaceId}, {@code allowedSubjects}
 * and {@code routes} cross over as business facts; the three jsonb columns are already decoded into structured collections at
 * the persistence boundary, so this projection never touches a JSON shape again, and the persistence-boundary identifier and
 * audit instants stay out of the response, which makes the projection irreversible with no reverse method.
 *
 * 用法 / Usage: 由 API 投影层在返回模型列表或单模型时调用（bean 名 {@code llmModelConverter}），传入 {@code null}
 * 如实得到 {@code null} 而不伪造空响应；{@code revision} 必须是仓储推进后的权威值，因为响应合同要求
 * {@code revision >= 1}，因此本投影只在读取或保存完成后使用。/ Injected under the bean name {@code llmModelConverter}
 * when the API projects a model page or a single model; {@code null} input yields {@code null} instead of a fabricated empty
 * response, and {@code revision} has to be the repository-advanced authoritative value because the response contract requires
 * {@code revision >= 1}, so this projection runs only after a read or a save.
 */
@Slf4j
@Component("llmModelConverter")
public class LlmModelConverter implements BaseForwardConverter<
        LlmModelBO,
        LlmModelVO> {

    /**
     * 中文说明：表示 MapStruct 生成的同包结构映射器实例，本类只做空值守护与日志边界。
     * English summary: The MapStruct generated same-package structural mapper; this class only adds null guarding and logging.
     *
     * 用法 / Usage: 仅由本转换器的公开方法调用。/ Invoked only by the public methods of this converter.
     */
    private static final LlmModelProjection MAPPING =
            Mappers.getMapper(LlmModelProjection.class);

    /**
     * 中文说明：执行 toTarget 操作，把模型业务载体投影为 API-006/007 完整响应；
     * 传入 {@code null} 时如实返回 {@code null} 并留下调试日志，不伪造空投影。
     * English summary: Executes the toTarget operation, projecting the model carrier onto the API-006/007 response;
     * {@code null} input yields {@code null} with a debug log instead of a fabricated projection.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmModelConverter.toTarget(modelBO)}。
     * @param source 参数 模型业务载体；parameter the model carrier.
     * @return 返回 模型响应投影；returns the model projection.
     */
    @Override
    public LlmModelVO toTarget(LlmModelBO source) {
        if (source == null) {
            log.debug("LLM model carrier is absent; no API projection rendered");
            return null;
        }
        return MAPPING.toView(source);
    }

    /**
     * 中文说明：执行 toViewList 操作，按列表顺序投影模型载体；null 或空输入返回空列表而非 {@code null}，
     * 与分页合同里空页输出 {@code []} 的口径一致。
     * English summary: Executes the toViewList operation, projecting every model carrier in order; null or empty input yields
     * an empty list rather than {@code null}, matching the empty page answering with {@code []}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmModelConverter.toViewList(model carriers)}。
     * @param carriers 参数 模型业务载体列表；parameter the model carriers.
     * @return 返回 模型响应投影列表；returns the model projections.
     */
    public List<LlmModelVO> toViewList(List<LlmModelBO> carriers) {
        return toTargetList(carriers);
    }
}

/**
 * 中文说明：{@code LlmModelProjection} 是模型载体到响应的 MapStruct 结构映射契约，
 * {@code unmappedTargetPolicy=ERROR} 强制 {@link LlmModelVO} 的每个对外字段都显式表态，
 * 因此新增响应字段必须在此说明它来自哪个业务事实，不能靠默认命名静默带出不透明主键或审计时刻。
 * English summary: {@code LlmModelProjection} is the MapStruct structural contract from the model carrier to the response,
 * where {@code unmappedTargetPolicy=ERROR} forces every externally visible {@link LlmModelVO} field to state itself, so a new
 * response field has to declare which business fact it comes from instead of silently leaking the opaque identifier or the
 * audit instants through default naming.
 *
 * 用法 / Usage: 由 {@link LlmModelConverter} 通过 {@code Mappers} 持有，不对外暴露，也不登记为 Spring bean。
 * Held by {@link LlmModelConverter} through {@code Mappers}; neither published to callers nor registered as a bean.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface LlmModelProjection {

    /**
     * 中文说明：执行 toView 操作，按 {@link LlmModelVO} 的字段顺序投影模型业务载体。
     * English summary: Executes the toView operation, projecting the model carrier onto {@link LlmModelVO}.
     *
     * 用法 / Usage: 仅由 {@link LlmModelConverter#toTarget(LlmModelBO)} 调用。
     * @param source 参数 模型业务载体；parameter the model carrier.
     * @return 返回 模型响应投影；returns the model projection.
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "key", source = "modelKey")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "protocols", source = "protocols")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "dimensions", source = "dimensions")
    @Mapping(target = "embeddingSpaceId", source = "embeddingSpaceId")
    @Mapping(target = "allowedSubjects", source = "allowedSubjects")
    @Mapping(target = "routes", source = "routes")
    @Mapping(target = "revision", source = "revision")
    LlmModelVO toView(LlmModelBO source);
}
