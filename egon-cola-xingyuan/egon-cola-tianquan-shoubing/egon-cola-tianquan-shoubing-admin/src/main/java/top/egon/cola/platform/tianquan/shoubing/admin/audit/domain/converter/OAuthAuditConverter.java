package top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.po.OAuthAuditPO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthAuditConverter extends BaseConverter<OAuthAuditPO, OAuthAuditBO> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "eventType", source = "eventType")
    @Mapping(target = "subjectRef", source = "subjectRef")
    @Mapping(target = "clientRef", source = "clientRef")
    @Mapping(target = "tokenRef", source = "tokenRef")
    @Mapping(target = "resourceRef", source = "resourceRef")
    @Mapping(target = "result", source = "result")
    @Mapping(target = "reasonCode", source = "reasonCode")
    @Mapping(target = "traceId", source = "traceId")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "auditKind", source = "auditKind")
    @Mapping(target = "publicId", source = "publicId")
    @Mapping(target = "actorSub", source = "actorSub")
    @Mapping(target = "targetSub", source = "targetSub")
    @Mapping(target = "identityTenantId", source = "identityTenantId")
    OAuthAuditBO toTarget(OAuthAuditPO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "eventType", source = "eventType")
    @Mapping(target = "subjectRef", source = "subjectRef")
    @Mapping(target = "clientRef", source = "clientRef")
    @Mapping(target = "tokenRef", source = "tokenRef")
    @Mapping(target = "resourceRef", source = "resourceRef")
    @Mapping(target = "result", source = "result")
    @Mapping(target = "reasonCode", source = "reasonCode")
    @Mapping(target = "traceId", source = "traceId")
    @Mapping(target = "occurredAt", source = "occurredAt")
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "auditKind", source = "auditKind")
    @Mapping(target = "publicId", source = "publicId")
    @Mapping(target = "actorSub", source = "actorSub")
    @Mapping(target = "targetSub", source = "targetSub")
    @Mapping(target = "identityTenantId", source = "identityTenantId")
    OAuthAuditPO toSource(OAuthAuditBO target);
}
