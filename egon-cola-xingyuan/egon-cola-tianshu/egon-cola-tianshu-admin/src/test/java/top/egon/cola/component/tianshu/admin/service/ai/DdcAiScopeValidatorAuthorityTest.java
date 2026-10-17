package top.egon.cola.component.tianshu.admin.service.ai;

import io.grpc.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceEnvAppBindingBO;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.admin.security.rpc.DdcServicePrincipal;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiSystemActor;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcAiScopeValidatorAuthorityTest {

    private static final DdcAiScopeDTO SCOPE = new DdcAiScopeDTO("retail", "prod-space", "prod", "assistant-app");
    private static final long NAMESPACE_ID = 71L;

    private final DdcScopeGate scopeGate = mock(DdcScopeGate.class);
    private final DdcAiResourceRepository resourceRepository = mock(DdcAiResourceRepository.class);
    private final DdcNamespaceRepository namespaceRepository = mock(DdcNamespaceRepository.class);
    private final DdcNamespaceEnvAppBindingRepository bindingRepository = mock(DdcNamespaceEnvAppBindingRepository.class);
    private final DdcAiScopeValidator validator = new DdcAiScopeValidator(
            scopeGate, resourceRepository, namespaceRepository, bindingRepository);

    @BeforeEach
    void enabledScope() {
        SecurityContextHolder.clearContext();
        MDC.put("tenantId", "42");
        when(namespaceRepository.findByBizCodeAndNamespaceCode(SCOPE.bizCode(), SCOPE.namespaceCode()))
                .thenReturn(Optional.of(DdcNamespaceBO.builder().id(NAMESPACE_ID).publicId("ns-public")
                        .bizCode(SCOPE.bizCode()).namespaceCode(SCOPE.namespaceCode()).enabled(true).build()));
        when(bindingRepository.findAll(SCOPE.bizCode(), SCOPE.namespaceCode(), SCOPE.env(), SCOPE.appCode()))
                .thenReturn(List.of(DdcNamespaceEnvAppBindingBO.builder().enabled(true).build()));
    }

    @AfterEach
    void clear() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @CsvSource({
            "READ,AI_READ,true", "READ,AI_REPORT,false",
            "REPORT,AI_REPORT,true", "REPORT,AI_READ,false",
            "WRITE,AI_WRITE,true", "WRITE,AI_PUBLISH,false",
            "PUBLISH,AI_PUBLISH,true", "PUBLISH,AI_WRITE,false"})
    void rpcPrincipalRequiresTheMatchingAllowedOperation(String permission, String allowed, boolean permitted) {
        Runnable call = () -> invoke(permission);
        if (permitted) {
            assertThatCode(() -> withPrincipal(principal(Set.of(allowed)), call)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> withPrincipal(principal(Set.of(allowed)), call))
                    .isInstanceOfSatisfying(CommonException.class, failure -> assertThat(failure.getCode()).isEqualTo(56307));
        }
    }

    @Test
    void reportIsReadOnlyAndDoesNotLockScopeParents() {
        withPrincipal(principal(Set.of("AI_REPORT")), () -> validator.assertCanReport(resource()));

        verify(resourceRepository, never()).lockScopeParentsForShare(any());
    }

    @Test
    void writeStillLocksScopeParents() {
        withPrincipal(principal(Set.of("AI_WRITE")), () -> validator.assertCanWrite(resource()));

        verify(resourceRepository).lockScopeParentsForShare(SCOPE);
    }

    @Test
    void systemActorIsAdmittedOnlyInsideATenantContext() {
        assertThatCode(() -> DdcAiSystemActor.bind(DdcAiSystemActor.OPTIMIZER, () -> {
            validator.assertCanPublish(resource());
            return null;
        })).doesNotThrowAnyException();

        MDC.remove("tenantId");
        assertThatThrownBy(() -> DdcAiSystemActor.bind(DdcAiSystemActor.JUDGE, () -> {
            validator.assertCanRead(resource());
            return null;
        })).isInstanceOf(CommonException.class);
        assertThat(DdcAiSystemActor.current()).isEmpty();
    }

    @Test
    void namespaceOutsideCredentialPatternsIsRejected() {
        DdcServicePrincipal narrow = new DdcServicePrincipal("runtime", "SDK", Set.of("assistant-app"),
                Set.of("prod"), Set.of("retail"), Set.of("AI_REPORT"), "assistant-app", "prod", "retail", 42L,
                "tenant-42", Set.of("other-*"));

        assertThatThrownBy(() -> withPrincipal(narrow, () -> validator.assertCanReport(resource())))
                .isInstanceOf(CommonException.class);
    }

    @Test
    void unscopedRpcPrincipalIsAuthorizedByCredentialPatterns() {
        DdcServicePrincipal unscoped = new DdcServicePrincipal("runtime", "SDK", Set.of("assistant-*"),
                Set.of("prod"), Set.of("*"), Set.of("AI_WRITE"), null, null, null, 42L, "tenant-42", Set.of("prod-*"));
        DdcServicePrincipal foreign = new DdcServicePrincipal("runtime", "SDK", Set.of("billing-*"),
                Set.of("prod"), Set.of("*"), Set.of("AI_WRITE"), null, null, null, 42L, "tenant-42", Set.of("prod-*"));

        assertThatCode(() -> withPrincipal(unscoped, () -> validator.assertCanWrite(resource())))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> withPrincipal(foreign, () -> validator.assertCanWrite(resource())))
                .isInstanceOf(CommonException.class);
    }

    @Test
    void systemActorRejectsMalformedIdentity() {
        assertThatThrownBy(() -> DdcAiSystemActor.bind("user:alice", () -> null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void invoke(String permission) {
        Consumer<DdcAiResourceBO> action = switch (permission) {
            case "READ" -> validator::assertCanRead;
            case "REPORT" -> validator::assertCanReport;
            case "WRITE" -> validator::assertCanWrite;
            default -> validator::assertCanPublish;
        };
        action.accept(resource());
    }

    private static DdcAiResourceBO resource() {
        return DdcAiResourceBO.builder().bizCode(SCOPE.bizCode()).namespaceId(NAMESPACE_ID)
                .namespaceCode(SCOPE.namespaceCode()).envCode(SCOPE.env()).appCode(SCOPE.appCode()).build();
    }

    private static DdcServicePrincipal principal(Set<String> operations) {
        return new DdcServicePrincipal("runtime", "SDK", Set.of("assistant-app"), Set.of("prod"), Set.of("retail"),
                operations, "assistant-app", "prod", "retail", 42L, "tenant-42", Set.of("prod-*"));
    }

    private static void withPrincipal(DdcServicePrincipal principal, Runnable action) {
        Context context = principal.bind(Context.current());
        Context previous = context.attach();
        try {
            action.run();
        } finally {
            context.detach(previous);
        }
    }
}
