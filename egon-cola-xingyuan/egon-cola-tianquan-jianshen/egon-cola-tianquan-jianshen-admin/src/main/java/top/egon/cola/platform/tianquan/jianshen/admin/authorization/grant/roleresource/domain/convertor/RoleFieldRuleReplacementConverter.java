package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.convertor;

import java.time.Instant;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Context;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.MappingConstants;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleFieldRulesCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.repository.RoleResourceGrantRepository.FieldRuleReplacementBO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.repository.RoleResourceGrantRepository.FieldRuleDesiredVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleFieldRulesCommandDTO.RoleFieldRuleDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.domain.po.FieldRulePO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.domain.enums.FieldRuleAccessLevelEnum;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldAccessLevel;

/** One-way projection with authoritative persistence identity; a client rule cannot supply its own role or audit context. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoleFieldRuleReplacementConverter extends BaseForwardConverter<RoleFieldRuleDTO, FieldRulePO> {
    @Override
    default FieldRulePO toTarget(RoleFieldRuleDTO source) {
        throw new IllegalArgumentException("Server field-rule persistence context is required");
    }

    @Mapping(target = "id", expression = "java(context.id())")
    @Mapping(target = "tenantId", expression = "java(context.tenantId())")
    @Mapping(target = "applicationId", expression = "java(context.applicationId())")
    @Mapping(target = "roleId", expression = "java(context.roleId())")
    @Mapping(target = "actorId", expression = "java(context.actorId())")
    @Mapping(target = "now", expression = "java(context.now())")
    FieldRulePO toTarget(RoleFieldRuleDTO source, @Context RulePersistenceContextVO context);

    @Mapping(target = "now", expression = "java(now)")
    FieldRuleReplacementBO toReplacement(
            ReplaceRoleFieldRulesCommandDTO source, @Context Instant now);

    FieldRuleDesiredVO toDesired(RoleFieldRuleDTO source);

    RoleFieldRuleDTO toRequest(FieldRuleDesiredVO source);

    FieldRuleAccessLevelEnum toAccessLevel(FieldAccessLevel source);

    @AfterMapping
    default void applyExplicitStatus(RoleFieldRuleDTO source, @Context RulePersistenceContextVO context, @MappingTarget FieldRulePO target) {
        if (target.getStatus() != source.getStatus()) {
            target.changeStatus(source.getStatus(), context.actorId(), context.now());
        }
    }

    record RulePersistenceContextVO(Long id, Long tenantId, Long applicationId, Long roleId, String actorId, Instant now) {
        public RulePersistenceContextVO {
            if (id == null || id <= 0 || tenantId == null || tenantId <= 0 || applicationId == null || applicationId <= 0 || roleId == null || roleId <= 0
                    || actorId == null || !actorId.matches("[\\x21-\\x7E]{1,128}")) {
                throw new IllegalArgumentException("Field-rule persistence identity is invalid");
            }
            Objects.requireNonNull(now, "now");
        }
    }
}
