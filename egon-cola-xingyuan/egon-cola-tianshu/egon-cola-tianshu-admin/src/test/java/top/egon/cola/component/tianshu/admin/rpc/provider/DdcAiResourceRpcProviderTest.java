package top.egon.cola.component.tianshu.admin.rpc.provider;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiNodeType;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPromptResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiResourceQuery;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillResult;
import top.egon.cola.component.rpc.tianshu.client.ai.DdcAiProtoConverterImpl;
import top.egon.cola.component.rpc.tianshu.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceEnvAppBindingBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiVersionSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.admin.security.rpc.DdcServicePrincipal;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiResourceService;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcAiResourceRpcProviderTest {

    private static final long TENANT_ID = 1001L;
    private static final long RESOURCE_ID = 810001L;
    private static final long VERSION_ID = 820004L;
    private static final String HASH = "a".repeat(64);
    private static final Instant CREATED_AT = Instant.parse("2026-09-29T10:00:00Z");
    private static final DdcAiScopeDTO SCOPE = new DdcAiScopeDTO(
            "retail", "prod-space", "prod", "assistant-app");

    @AfterEach
    void clearThreadContext() {
        MDC.clear();
    }

    @Test
    void springProviderUsesItsExplicitBeanNameAndQualifiedConstructorDependencies() {
        new ApplicationContextRunner()
                .withUserConfiguration(ProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("ddcAiResourceRpcProvider");
                    assertThat(DdcAiResourceRpcProvider.class.getAnnotation(Component.class).value())
                            .isEqualTo("ddcAiResourceRpcProvider");
                    var constructor = DdcAiResourceRpcProvider.class.getDeclaredConstructors()[0];
                    assertThat(constructor.getParameters()[0].getAnnotation(
                            org.springframework.beans.factory.annotation.Qualifier.class).value())
                            .isEqualTo("ddcAiResourceService");
                    assertThat(constructor.getParameters()[1].getAnnotation(
                            org.springframework.beans.factory.annotation.Qualifier.class).value())
                            .isEqualTo("ddcAiProtoConverterImpl");
                });
    }

    @Test
    void promptReadDelegatesToTheSinglePublishedVersionServiceQuery() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = new DdcAiResourceRpcProvider(service, new DdcAiProtoConverterImpl());
        DdcAiVersionResult published = versionResult(DdcAiKindEnum.PROMPT,
                DdcAiContentDTO.builder().promptText("approved prompt").nodes(List.of()).build());
        when(service.getPublishedVersion(any(), eq(DdcAiKindEnum.PROMPT))).thenReturn(published);
        AiResourceQuery query = query("review-order", 4L);

        AiPromptResult response = withPrincipal(principal(TENANT_ID, Set.of("prod-*")),
                () -> provider.getPrompt(query));

        assertThat(response.getResourceId()).isEqualTo("810001");
        assertThat(response.getResourceKey()).isEqualTo("review-order");
        assertThat(response.getVersionId()).isEqualTo("820004");
        assertThat(response.getVersionNo()).isEqualTo(4L);
        assertThat(response.getContentSha256()).isEqualTo(HASH);
        assertThat(response.getTemplate()).isEqualTo("approved prompt");
        verify(service).getPublishedVersion(argThat(value -> value.resourceId().equals(RESOURCE_ID)
                && value.namespaceCode().equals(SCOPE.namespaceCode())
                && value.resourceKey().equals("review-order")
                && value.versionNo().equals(4L)), eq(DdcAiKindEnum.PROMPT));
    }

    @Test
    void skillReadReturnsTheCompleteTreeAndPreservesDirectoryAndEmptyFilePresence() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = new DdcAiResourceRpcProvider(service, new DdcAiProtoConverterImpl());
        DdcAiVersionResult published = versionResult(DdcAiKindEnum.SKILL,
                DdcAiContentDTO.builder().nodes(List.of(
                        DdcAiNodeDTO.builder().path("").nodeType(DdcAiNodeTypeEnum.DIRECTORY).build(),
                        DdcAiNodeDTO.builder().path("README.txt").nodeType(DdcAiNodeTypeEnum.FILE)
                                .content("").build())).build());
        when(service.getPublishedVersion(any(), eq(DdcAiKindEnum.SKILL))).thenReturn(published);

        AiSkillResult response = withPrincipal(principal(TENANT_ID, Set.of("prod-space")),
                () -> provider.getSkill(query("review-order", null)));

        assertThat(response.getFilesList()).hasSize(2);
        assertThat(response.getFiles(0).getNodeType()).isEqualTo(AiNodeType.DIRECTORY);
        assertThat(response.getFiles(0).hasContent()).isFalse();
        assertThat(response.getFiles(1).getNodeType()).isEqualTo(AiNodeType.FILE);
        assertThat(response.getFiles(1).hasContent()).isTrue();
        assertThat(response.getFiles(1).getContent()).isEmpty();
        verify(service).getPublishedVersion(any(), eq(DdcAiKindEnum.SKILL));
    }

    @Test
    void knownAiScopeFailureKeepsPermissionStatusAndTypedTrailer() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = new DdcAiResourceRpcProvider(service, new DdcAiProtoConverterImpl());
        when(service.getPublishedVersion(any(), eq(DdcAiKindEnum.PROMPT)))
                .thenThrow(new CommonException(56307, "TIANSHU_AI_SCOPE_FORBIDDEN",
                        "AI permission is required for the requested namespace"));

        assertThatThrownBy(() -> withPrincipal(principal(TENANT_ID, Set.of("*")),
                () -> provider.getPrompt(query("review-order", null))))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(failure -> {
                    StatusRuntimeException status = (StatusRuntimeException) failure;
                    assertThat(status.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
                    assertThat(status.getTrailers()
                            .get(DdcRpcStatusExceptionMapper.ERROR_DETAIL).getCode())
                            .isEqualTo("TIANSHU_AI_SCOPE_FORBIDDEN");
                });
    }

    @Test
    void unpublishedResourceRemainsANotFoundStatusForAiConsumers() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = new DdcAiResourceRpcProvider(service, new DdcAiProtoConverterImpl());
        when(service.getPublishedVersion(any(), eq(DdcAiKindEnum.PROMPT)))
                .thenThrow(new CommonException(56306, "TIANSHU_AI_NOT_PUBLISHED",
                        "Resource has no published version"));

        assertThatThrownBy(() -> withPrincipal(principal(TENANT_ID, Set.of("*")),
                () -> provider.getPrompt(query("review-order", null))))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(failure -> {
                    StatusRuntimeException status = (StatusRuntimeException) failure;
                    assertThat(status.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                    assertThat(status.getTrailers()
                            .get(DdcRpcStatusExceptionMapper.ERROR_DETAIL).getCode())
                            .isEqualTo("TIANSHU_AI_NOT_PUBLISHED");
                });
    }

    @Test
    void trustedServicePrincipalIsLimitedToTenantAndNamespaceAiRead() {
        DdcScopeGate scopeGate = mock(DdcScopeGate.class);
        DdcAiResourceRepository resourceRepository = mock(DdcAiResourceRepository.class);
        DdcNamespaceRepository namespaceRepository = mock(DdcNamespaceRepository.class);
        DdcNamespaceEnvAppBindingRepository bindingRepository = mock(DdcNamespaceEnvAppBindingRepository.class);
        DdcNamespaceBO namespace = DdcNamespaceBO.builder().id(71L).publicId("namespace-prod-71")
                .bizCode(SCOPE.bizCode()).namespaceCode(SCOPE.namespaceCode()).enabled(true).build();
        when(namespaceRepository.findByBizCodeAndNamespaceCode(SCOPE.bizCode(), SCOPE.namespaceCode()))
                .thenReturn(Optional.of(namespace));
        when(bindingRepository.findAll(SCOPE.bizCode(), SCOPE.namespaceCode(), SCOPE.env(), SCOPE.appCode()))
                .thenReturn(List.of(DdcNamespaceEnvAppBindingBO.builder().enabled(true).build()));
        DdcAiScopeValidator validator = new DdcAiScopeValidator(
                scopeGate, resourceRepository, namespaceRepository, bindingRepository);
        DdcAiResourceBO resource = DdcAiResourceBO.builder().id(RESOURCE_ID).tenantId(TENANT_ID)
                .bizCode(SCOPE.bizCode()).namespaceId(71L).namespaceCode(SCOPE.namespaceCode())
                .envCode(SCOPE.env()).appCode(SCOPE.appCode()).build();

        MDC.put("tenantId", Long.toString(TENANT_ID));
        DdcServicePrincipal permitted = principal(TENANT_ID, Set.of("prod-*"));
        withPrincipal(permitted, () -> {
            validator.assertCanRead(resource);
            assertThatThrownBy(() -> validator.assertCanWrite(resource))
                    .isInstanceOfSatisfying(CommonException.class,
                            failure -> assertThat(failure.getCode()).isEqualTo(56307));
            return null;
        });

        DdcServicePrincipal wrongTenant = principal(TENANT_ID + 1, Set.of("prod-*"));
        assertThatThrownBy(() -> withPrincipal(wrongTenant, () -> {
            validator.assertCanRead(resource);
            return null;
        }))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56307));

        DdcServicePrincipal wrongNamespace = principal(TENANT_ID, Set.of("dev-*"));
        assertThatThrownBy(() -> withPrincipal(wrongNamespace, () -> {
            validator.assertCanRead(resource);
            return null;
        }))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56307));

        DdcServicePrincipal missingAiRead = principal(TENANT_ID, Set.of("prod-*"),
                "assistant-app", "prod", "retail", Set.of("CONFIG_PULL"));
        assertThatThrownBy(() -> withPrincipal(missingAiRead, () -> {
            validator.assertCanRead(resource);
            return null;
        }))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56307));

        DdcServicePrincipal wrongBusinessScope = principal(TENANT_ID, Set.of("prod-*"),
                "assistant-app", "prod", "other-biz", Set.of("AI_READ"));
        assertThatThrownBy(() -> withPrincipal(wrongBusinessScope, () -> {
            validator.assertCanRead(resource);
            return null;
        }))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56307));
        verify(resourceRepository).lockScopeParentsForShare(SCOPE);
    }

    private DdcAiVersionResult versionResult(DdcAiKindEnum kind, DdcAiContentDTO content) {
        DdcAiVersionSummaryDTO summary = DdcAiVersionSummaryDTO.builder()
                .id(VERSION_ID).resourceId(RESOURCE_ID).versionNo(4L).kind(kind.name())
                .displayName("Review Order").description("").contentSha256(HASH)
                .contentBytes(16L).nodeCount(content.getNodes().size()).changeNote("approved")
                .createdAt(CREATED_AT).createdBy("operator").build();
        return DdcAiVersionResult.builder().version(summary).content(content).build();
    }

    private AiResourceQuery query(String key, Long versionNo) {
        AiResourceQuery.Builder query = AiResourceQuery.newBuilder().setBizCode(SCOPE.bizCode())
                .setNamespaceCode(SCOPE.namespaceCode()).setEnv(SCOPE.env()).setAppCode(SCOPE.appCode())
                .setResourceKey(key).setResourceId(Long.toString(RESOURCE_ID));
        if (versionNo != null) query.setVersionNo(versionNo);
        return query.build();
    }

    private DdcServicePrincipal principal(long tenantId, Set<String> namespacePatterns) {
        return principal(tenantId, namespacePatterns, "assistant-app", "prod", "retail", Set.of("AI_READ"));
    }

    private DdcServicePrincipal principal(
            long tenantId,
            Set<String> namespacePatterns,
            String appCode,
            String env,
            String bizCode,
            Set<String> operations) {
        return new DdcServicePrincipal("ai-reader", "SDK", Set.of(appCode), Set.of(env),
                Set.of(bizCode), operations, appCode, env, bizCode, tenantId,
                "tenant-" + tenantId, namespacePatterns);
    }

    private <T> T withPrincipal(DdcServicePrincipal principal, java.util.concurrent.Callable<T> action) {
        Context context = principal.bind(Context.current());
        Context previous = context.attach();
        try {
            return action.call();
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new AssertionError(failure);
        } finally {
            context.detach(previous);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
            basePackageClasses = DdcAiResourceRpcProvider.class,
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = DdcAiResourceRpcProvider.class))
    static class ProviderConfiguration {
        @Bean("ddcAiResourceService")
        DdcAiResourceService ddcAiResourceService() {
            return mock(DdcAiResourceService.class);
        }

        @Bean("ddcAiProtoConverterImpl")
        DdcAiProtoConverterImpl ddcAiProtoConverter() {
            return new DdcAiProtoConverterImpl();
        }
    }
}
