package top.egon.cola.component.tianshu.admin.service.ai.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;

import java.util.Objects;

@Slf4j
@Component("ddcAiScopeValidator")
@RequiredArgsConstructor
@Validated
public class DdcAiScopeValidator {

    @Qualifier("ddcScopeGate")
    private final DdcScopeGate scopeGate;

    @Qualifier("ddcAiResourceRepository")
    private final DdcAiResourceRepository resourceRepository;

    @Qualifier("ddcNamespaceRepository")
    private final DdcNamespaceRepository namespaceRepository;

    @Qualifier("ddcNamespaceEnvAppBindingRepository")
    private final DdcNamespaceEnvAppBindingRepository bindingRepository;

    public Long resolveNamespaceIdForRead(@NotNull @Valid DdcAiScopeDTO scope) {
        return resolveNamespaceId(scope, Permission.READ);
    }

    public Long resolveNamespaceIdForWrite(@NotNull @Valid DdcAiScopeDTO scope) {
        return resolveNamespaceId(scope, Permission.WRITE);
    }

    private Long resolveNamespaceId(DdcAiScopeDTO scope, Permission permission) {
        if (permission != Permission.READ) {
            resourceRepository.lockScopeParentsForShare(scope);
            invalidateScopeCache(scope);
        }
        scopeGate.assertEnabled(scope.bizCode(), scope.appCode(), scope.env(), scope.namespaceCode());
        DdcNamespaceBO namespace = namespaceRepository.findByBizCodeAndNamespaceCode(
                        scope.bizCode(), scope.namespaceCode())
                .orElseThrow(() -> new CommonException(DdcErrorStatus.SCOPE_DISABLED));
        boolean bound = bindingRepository.findAll(scope.bizCode(), scope.namespaceCode(), scope.env(), scope.appCode())
                .stream().anyMatch(binding -> Boolean.TRUE.equals(binding.getEnabled()));
        if (!bound) throw new CommonException(DdcErrorStatus.SCOPE_DISABLED);
        requireNamespacePermission(namespace.getPublicId(), permission);
        return namespace.getId();
    }

    private void invalidateScopeCache(DdcAiScopeDTO scope) {
        scopeGate.invalidate("biz:" + scope.bizCode());
        scopeGate.invalidate("namespace:" + scope.bizCode() + ":" + scope.namespaceCode());
        scopeGate.invalidate("env:" + scope.env());
        scopeGate.invalidate("app:" + scope.bizCode() + ":" + scope.appCode());
    }

    public void assertCanRead(@NotNull @Valid DdcAiResourceBO resource) {
        assertResourceScope(resource, Permission.READ);
    }

    public void assertCanWrite(@NotNull @Valid DdcAiResourceBO resource) {
        assertResourceScope(resource, Permission.WRITE);
    }

    public void assertCanPublish(@NotNull @Valid DdcAiResourceBO resource) {
        assertResourceScope(resource, Permission.PUBLISH);
    }

    private void assertResourceScope(DdcAiResourceBO resource, Permission permission) {
        String namespaceCode = resource.getNamespaceCode();
        if (namespaceCode == null) {
            DdcNamespaceBO namespace = namespaceRepository.findActiveById(resource.getNamespaceId())
                    .filter(row -> Objects.equals(row.getBizCode(), resource.getBizCode()))
                    .orElseThrow(() -> new CommonException(DdcErrorStatus.SCOPE_DISABLED));
            namespaceCode = namespace.getNamespaceCode();
        }
        Long resolvedNamespaceId = resolveNamespaceId(new DdcAiScopeDTO(resource.getBizCode(),
                namespaceCode, resource.getEnvCode(), resource.getAppCode()), permission);
        if (!resolvedNamespaceId.equals(resource.getNamespaceId())) {
            throw new CommonException(DdcErrorStatus.SCOPE_DISABLED);
        }
    }

    private void requireNamespacePermission(String namespacePublicId, Permission permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Rbac3UserDetails user)) {
            throw scopeForbidden();
        }
        String basePermission = "TIANSHU_AI_" + permission.name();
        if (!user.permissions().contains(basePermission)
                && !user.permissions().contains(basePermission + ":*")
                && !user.permissions().contains(basePermission + ":" + namespacePublicId)) {
            throw scopeForbidden();
        }
    }

    private static CommonException scopeForbidden() {
        return new CommonException(56307, "TIANSHU_AI_SCOPE_FORBIDDEN",
                "AI permission is required for the requested namespace");
    }

    private enum Permission {
        READ,
        WRITE,
        PUBLISH
    }
}
