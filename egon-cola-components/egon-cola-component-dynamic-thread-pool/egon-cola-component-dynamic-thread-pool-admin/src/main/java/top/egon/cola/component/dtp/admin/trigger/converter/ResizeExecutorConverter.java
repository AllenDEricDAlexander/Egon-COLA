package top.egon.cola.component.dtp.admin.trigger.converter;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.dtp.admin.trigger.model.ResizeExecutorRequest;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;

/**
 * @description 调容输入转换，目标身份来自路径而非请求体
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "resizeExecutorConverter"))
public interface ResizeExecutorConverter extends BaseForwardConverter<ResizeExecutorRequest, ExecutorUpdateCommand> {

    @Override
    @Mapping(target = "appName", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "executorName", ignore = true)
    @Mapping(target = "concurrencyLimit", ignore = true)
    @Mapping(target = "version", ignore = true)
    ExecutorUpdateCommand toTarget(ResizeExecutorRequest source);

    @Mapping(target = "appName", source = "appName")
    @Mapping(target = "instanceId", source = "instanceId")
    @Mapping(target = "executorName", source = "executorName")
    @Mapping(target = "concurrencyLimit", ignore = true)
    @Mapping(target = "version", ignore = true)
    ExecutorUpdateCommand toCommand(ResizeExecutorRequest source, String appName, String instanceId, String executorName);
}
