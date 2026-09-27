package top.egon.cola.component.yuheng.llm.proxy.repository;

import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotProjectionBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.po.LlmChannelPO;
import top.egon.cola.component.yuheng.llm.proxy.domain.po.LlmModelPO;

/**
 * 中文说明：{@code LlmModelSnapshotConverter} 用 MapStruct 完成行/类型化配置到请求快照 BO 的不可逆投影；JSONB 内容
 * 在 repository 先做协议校验与类型解码，本转换器只负责字段映射、route 渠道合成以及可空 secretRef 保真。
 * English summary: MapStruct owns this one-way projection from the row/typed configuration to the request snapshot BO;
 * the repository validates and decodes JSONB first, while this converter maps fields, composes route channels and
 * preserves a nullable secretRef.
 */
@Slf4j
@Component("llmModelSnapshotConverter")
public class LlmModelSnapshotConverter
        implements BaseForwardConverter<LlmModelSnapshotProjectionBO, LlmModelSnapshotBO> {

    private static final LlmModelSnapshotMapping MAPPING = Mappers.getMapper(LlmModelSnapshotMapping.class);

    @Override
    public LlmModelSnapshotBO toTarget(LlmModelSnapshotProjectionBO source) {
        if (source == null) {
            log.debug("llm model snapshot projection is absent");
            return null;
        }
        return MAPPING.toTarget(source);
    }

    public LlmModelSnapshotProjectionBO toProjection(
            LlmModelPO model,
            List<LlmProtocolEnum> protocols,
            List<String> allowedSubjects,
            List<LlmModelSnapshotBO.RouteBO> routes) {
        if (model == null) {
            log.debug("llm model row is absent while composing a snapshot");
            return null;
        }
        return MAPPING.toProjection(model, protocols, allowedSubjects, routes);
    }

    public LlmModelSnapshotBO.RouteBO toRoute(
            String channelKey,
            String upstreamModel,
            Integer priority,
            Integer weight,
            Set<LlmCapabilityEnum> capabilities) {
        return MAPPING.toRoute(channelKey, upstreamModel, priority, weight, capabilities);
    }

    public LlmModelSnapshotBO.RouteBO withChannel(
            LlmModelSnapshotBO.RouteBO route,
            LlmModelSnapshotBO.ChannelBO channel) {
        if (route == null) {
            log.debug("llm route projection is absent while resolving its channel");
            return null;
        }
        return MAPPING.withChannel(route, channel);
    }

    public LlmModelSnapshotBO.ChannelBO toChannel(LlmChannelPO source) {
        if (source == null) {
            return null;
        }
        return MAPPING.toChannel(source);
    }
}

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface LlmModelSnapshotMapping
        extends BaseForwardConverter<LlmModelSnapshotProjectionBO, LlmModelSnapshotBO> {

    @Override
    @BeanMapping(builder = @Builder(disableBuilder = true))
    LlmModelSnapshotBO toTarget(LlmModelSnapshotProjectionBO source);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "modelKey", source = "model.modelKey")
    @Mapping(target = "name", source = "model.name")
    @Mapping(target = "createdAt", source = "model.createTime")
    @Mapping(target = "kind", source = "model.kind")
    @Mapping(target = "enabled", source = "model.enabled")
    @Mapping(target = "dimensions", source = "model.dimensions")
    @Mapping(target = "embeddingSpaceId", source = "model.embeddingSpaceId")
    @Mapping(target = "revision", source = "model.revision")
    @Mapping(target = "protocols", source = "protocols")
    @Mapping(target = "allowedSubjects", source = "allowedSubjects")
    @Mapping(target = "routes", source = "routes")
    LlmModelSnapshotProjectionBO toProjection(
            LlmModelPO model,
            List<LlmProtocolEnum> protocols,
            List<String> allowedSubjects,
            List<LlmModelSnapshotBO.RouteBO> routes);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "channelKey", source = "channelKey")
    @Mapping(target = "upstreamModel", source = "upstreamModel")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "weight", source = "weight")
    @Mapping(target = "capabilities", source = "capabilities")
    @Mapping(target = "channel", ignore = true)
    LlmModelSnapshotBO.RouteBO toRoute(
            String channelKey,
            String upstreamModel,
            Integer priority,
            Integer weight,
            Set<LlmCapabilityEnum> capabilities);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "channelKey", source = "route.channelKey")
    @Mapping(target = "upstreamModel", source = "route.upstreamModel")
    @Mapping(target = "priority", source = "route.priority")
    @Mapping(target = "weight", source = "route.weight")
    @Mapping(target = "capabilities", source = "route.capabilities")
    @Mapping(target = "channel", source = "channel")
    LlmModelSnapshotBO.RouteBO withChannel(LlmModelSnapshotBO.RouteBO route,
                                           LlmModelSnapshotBO.ChannelBO channel);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    LlmModelSnapshotBO.ChannelBO toChannel(LlmChannelPO source);
}
