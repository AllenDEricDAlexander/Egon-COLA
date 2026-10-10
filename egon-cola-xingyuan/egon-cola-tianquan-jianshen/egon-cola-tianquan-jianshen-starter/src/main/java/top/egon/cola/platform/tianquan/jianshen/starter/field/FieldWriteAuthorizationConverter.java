package top.egon.cola.platform.tianquan.jianshen.starter.field;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;

/** Projects only the three existing PDP keys; submitted fields remain owned by the guard. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FieldWriteAuthorizationConverter extends BaseForwardConverter<FieldWriteAuthorizationCommand, AuthorizationService.FieldPolicyRequest> {
    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "applicationCode", source = "applicationCode")
    @Mapping(target = "resourceCode", source = "resourceCode")
    AuthorizationService.FieldPolicyRequest toTarget(FieldWriteAuthorizationCommand source);
}
