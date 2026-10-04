package top.egon.cola.component.tianshu.admin.model.convertor;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.tianshu.model.event.DdcRegistryRevisionEvent;
import top.egon.cola.component.tianshu.model.registry.DdcRegistryEvent;

/** Projects an immutable registry fact into its tenant-scoped outbox envelope. */
@Mapper(
        componentModel = "spring",
        implementationName = "DdcRegistryRevisionEventConverterImpl",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface DdcRegistryRevisionEventConverter
        extends BaseForwardConverter<DdcRegistryEvent, DdcRegistryRevisionEvent> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "event", source = ".")
    @Mapping(target = "schemaVersion", constant = "1")
    DdcRegistryRevisionEvent toTarget(DdcRegistryEvent source);

    default DdcRegistryRevisionEvent toTarget(DdcRegistryEvent source, Long tenantId) {
        if (source == null || tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("registry event and trusted tenant are required");
        }
        DdcRegistryRevisionEvent target = toTarget(source);
        target.setTenantId(tenantId);
        target.setEventId(DdcRegistryRevisionEvent.eventId(tenantId, source));
        return target;
    }
}
