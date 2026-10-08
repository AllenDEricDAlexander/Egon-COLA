package top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.vo.IdentityAuditVO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IdentityAuditViewConverter extends BaseForwardConverter<OAuthAuditBO, IdentityAuditVO> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "eventType", source = "eventType")
    @Mapping(target = "actorSub", source = "actorSub")
    @Mapping(target = "targetSub", source = "targetSub")
    @Mapping(target = "result", source = "result")
    @Mapping(target = "reason", source = "reasonCode")
    @Mapping(target = "occurredAt", source = "occurredAt")
    IdentityAuditVO toTarget(OAuthAuditBO source);
}
