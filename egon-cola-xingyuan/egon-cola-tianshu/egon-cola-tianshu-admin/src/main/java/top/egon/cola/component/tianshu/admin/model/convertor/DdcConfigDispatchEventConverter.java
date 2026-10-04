package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;

@Mapper(
        componentModel = "spring",
        implementationName = "DdcConfigDispatchEventConverterImpl",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface DdcConfigDispatchEventConverter
        extends BaseForwardConverter<DdcAtomicPublishCommand, DdcConfigDispatchPreparedEvent> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "eventId", expression = "java(DdcConfigDispatchPreparedEvent.eventId(source.getTenantId(), source.getChangeId(), source.getAttempt()))")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "schemaVersion", constant = "1")
    @Mapping(target = "attempt", source = "attempt")
    @Mapping(target = "configId", source = "configId")
    @Mapping(target = "expectedPublishedVersion", source = "expectedPublishedVersion")
    @Mapping(target = "eventChecksum", ignore = true)
    @Mapping(target = "message", source = "message")
    DdcConfigDispatchPreparedEvent toTarget(DdcAtomicPublishCommand source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "configId", source = "configId")
    @Mapping(target = "changeId", source = "message.changeId")
    @Mapping(target = "bizCode", source = "message.bizCode")
    @Mapping(target = "env", source = "message.env")
    @Mapping(target = "appCode", source = "message.appCode")
    @Mapping(target = "resourceName", source = "message.resourceName")
    @Mapping(target = "expectedPublishedVersion", source = "expectedPublishedVersion")
    @Mapping(target = "targetVersion", source = "message.targetVersion")
    @Mapping(target = "content", source = "message.content")
    @Mapping(target = "eventChecksum", source = "eventChecksum")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "attempt", source = "attempt")
    @Mapping(target = "eventId", source = "eventId")
    DdcAtomicPublishCommand toCommand(DdcConfigDispatchPreparedEvent source);

    @AfterMapping
    default void calculateChecksum(@MappingTarget DdcConfigDispatchPreparedEvent target) {
        target.setEventChecksum(target.calculateEventChecksum());
    }

    default boolean isValid(DdcConfigDispatchPreparedEvent event) {
        return event != null
                && event.hasValidIdentityAndChecksum()
                && event.getMessage() != null
                && event.getMessage().getChecksum() != null
                && event.getMessage().getChecksum().equals(
                        top.egon.cola.component.tianshu.format.DdcChecksum.sha256(event.getMessage()));
    }
}
