package top.egon.cola.platform.tianquan.jianshen.starter.field;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.Decision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.security.CurrentRbac3User;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class Rbac3FieldWriteAuthorizationComponentTest {
    private static final Instant NOW = Instant.parse("2026-10-10T00:00:00Z");
    private ValidatorFactory factory;
    private Validator validator;
    private AuthorizationService authorization;
    private CurrentRbac3User currentUser;
    private FieldWriteAuthorizationConverter converter;
    private Rbac3FieldWriteAuthorizationComponent guard;

    @BeforeEach void setUp() {
        factory = Validation.buildDefaultValidatorFactory(); validator = factory.getValidator();
        authorization = mock(AuthorizationService.class); currentUser = mock(CurrentRbac3User.class);
        Rbac3UserDetails details = mock(Rbac3UserDetails.class);
        when(details.identity()).thenReturn(identity()); when(currentUser.require()).thenReturn(details);
        converter = Mappers.getMapper(FieldWriteAuthorizationConverter.class);
        guard = new Rbac3FieldWriteAuthorizationComponent(authorization, currentUser, converter, validator);
    }

    @AfterEach void close() { factory.close(); SecurityContextHolder.clearContext(); }

    @Test void allSubmittedFieldsMustBeWriteFromOnePdpDecision() {
        FieldPolicyDecision decision = decision(Decision.ALLOW, Map.of("title", access(FieldAccessLevel.WRITE)));
        when(authorization.decideFields(any())).thenReturn(decision);
        assertThat(guard.requireWritableFields(command(Set.of("title")))).isSameAs(decision);
        verify(authorization, times(1)).decideFields(new AuthorizationService.FieldPolicyRequest("article:edit", "cms", "article"));
        verifyNoMoreInteractions(authorization);
    }

    @Test void rejectsEveryReadMaskNoneMissingAndOverallDenyBeforeBusiness() {
        for (FieldAccessLevel level : List.of(FieldAccessLevel.READ, FieldAccessLevel.MASKED_READ, FieldAccessLevel.NONE)) {
            when(authorization.decideFields(any())).thenReturn(decision(Decision.ALLOW, Map.of("title", access(level))));
            Runnable business = mock(Runnable.class);
            assertThatThrownBy(() -> { guard.requireWritableFields(command(Set.of("title"))); business.run(); })
                    .isInstanceOf(org.springframework.security.authorization.AuthorizationDeniedException.class)
                    .hasMessage("FIELD_ACCESS_DENIED");
            verifyNoInteractions(business);
        }
        when(authorization.decideFields(any())).thenReturn(decision(Decision.ALLOW, Map.of()));
        assertThatThrownBy(() -> guard.requireWritableFields(command(Set.of("title"))))
                .isInstanceOf(org.springframework.security.authorization.AuthorizationDeniedException.class);
        when(authorization.decideFields(any())).thenReturn(decision(Decision.DENY, Map.of("title", access(FieldAccessLevel.WRITE))));
        assertThatThrownBy(() -> guard.requireWritableFields(command(Set.of("title"))))
                .isInstanceOf(org.springframework.security.authorization.AuthorizationDeniedException.class);
    }

    @Test void explicitNullRemainsSubmittedAndOmissionIsNotInvented() {
        when(authorization.decideFields(any())).thenReturn(decision(Decision.ALLOW, Map.of("title", access(FieldAccessLevel.WRITE))));
        Map<String, Object> submitted = new java.util.LinkedHashMap<>(); submitted.put("title", null);
        assertThat(guard.requireWritableFields(command(submitted.keySet()))).isNotNull();
        submitted.put("secret", null);
        assertThatThrownBy(() -> guard.requireWritableFields(command(submitted.keySet())))
                .isInstanceOf(org.springframework.security.authorization.AuthorizationDeniedException.class);
        assertThat(command(Set.of("title")).getSubmittedFields()).doesNotContain("secret");
    }

    @Test void failsClosedForUnavailableAndFencedDecisions() {
        when(authorization.decideFields(any())).thenReturn(decision(Decision.INDETERMINATE, Map.of()));
        assertThatThrownBy(() -> guard.requireWritableFields(command(Set.of("title"))))
                .isInstanceOf(AuthorizationService.RuntimeUnavailableException.class);
        when(authorization.decideFields(any())).thenReturn(new FieldPolicyDecision(Decision.DENY, "AUTHORIZATION_FENCED", "1001", "2001",
                "article:edit", "cms", "article", Map.of(), 1, 2, List.of(), NOW));
        assertThatThrownBy(() -> guard.requireWritableFields(command(Set.of("title"))))
                .isInstanceOf(org.springframework.security.authorization.AuthorizationDeniedException.class);
    }

    @Test void schemaFailureDoesNotCallPdpAndDoesNotTrimCodes() {
        for (FieldWriteAuthorizationCommand input : List.of(command(Set.of()), command(Set.of("title")).setPermissionCode(" article:edit"),
                command(Set.of("bad/name")), command(Set.of("x".repeat(129))))) {
            assertThatThrownBy(() -> guard.requireWritableFields(input)).isInstanceOf(ConstraintViolationException.class);
        }
        Set<String> tooMany = java.util.stream.IntStream.range(0, 101).mapToObj(i -> "field"+i).collect(java.util.stream.Collectors.toSet());
        assertThatThrownBy(() -> guard.requireWritableFields(command(tooMany))).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(authorization);
    }

    @Test void noUserAndServicePrincipalCannotEnterPdp() {
        Rbac3FieldWriteAuthorizationComponent realContext = new Rbac3FieldWriteAuthorizationComponent(authorization,
                new CurrentRbac3User(), converter, validator);
        assertThatThrownBy(() -> realContext.requireWritableFields(command(Set.of("title"))))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(mock(ServiceIdentityPrincipal.class), "", "SERVICE"));
        assertThatThrownBy(() -> realContext.requireWritableFields(command(Set.of("title"))))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verifyNoInteractions(authorization);
    }

    @Test void generatedMapperOnlyProjectsTheExistingThreeKeysAndCollectionsAreCopied() {
        Set<String> fields = new java.util.HashSet<>(Set.of("title"));
        FieldWriteAuthorizationCommand command = command(fields); fields.add("secret");
        assertThat(command.getSubmittedFields()).containsExactly("title");
        assertThatThrownBy(() -> command.getSubmittedFields().add("secret")).isInstanceOf(UnsupportedOperationException.class);
        assertThat(converter.toTarget(command)).isEqualTo(new AuthorizationService.FieldPolicyRequest("article:edit", "cms", "article"));
        assertThat(validator.validate(converter.toTarget(command))).isEmpty();
        assertThat(validator.validate(decision(Decision.ALLOW, Map.of("title", access(FieldAccessLevel.WRITE))))).isEmpty();
    }

    @Test void namedAutoConfigurationRegistersRealMapperAndValidatedGuard() {
        when(authorization.decideFields(any())).thenReturn(decision(Decision.ALLOW, Map.of("title", access(FieldAccessLevel.WRITE))));
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class,
                        top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterAutoConfiguration.class))
                .withPropertyValues("egon.cola.platform.tianquan.jianshen.enabled=true", "egon.cola.platform.tianquan.jianshen.system-code=cms")
                .withBean("authorizationService", AuthorizationService.class, () -> authorization)
                .withBean("currentRbac3User", CurrentRbac3User.class, () -> currentUser)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("fieldWriteAuthorizationConverterImpl").hasBean("rbac3FieldWriteAuthorizationComponent");
                    Rbac3FieldWriteAuthorizationComponent bean = context.getBean("rbac3FieldWriteAuthorizationComponent", Rbac3FieldWriteAuthorizationComponent.class);
                    assertThat(bean.requireWritableFields(command(Set.of("title")))).isNotNull();
                    assertThatThrownBy(() -> bean.requireWritableFields(command(Set.of())))
                            .isInstanceOf(ConstraintViolationException.class);
                });
    }

    @Test void decisionWireShapeAndOriginalPdpMethodConstraintsStayCompatible() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        var tree = mapper.valueToTree(decision(Decision.ALLOW, Map.of("title", access(FieldAccessLevel.WRITE))));
        assertThat(tree.size()).isEqualTo(12);
        assertThat(tree.get("fields").get("title").size()).isEqualTo(2);
        var pdp = new top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService(
                () -> { throw new IllegalStateException("not invoked"); }, request -> null, request -> null,
                java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC));
        var method = AuthorizationService.class.getMethod("decideFields", AuthorizationService.FieldPolicyRequest.class);
        assertThat(validator.forExecutables().validateParameters(pdp, method,
                new Object[]{converter.toTarget(command(Set.of("title")))})).isEmpty();
    }

    @Test void actualPdpSnapshotAndFenceRemainAuthoritative() {
        var policy = decision(Decision.ALLOW, Map.of("title", access(FieldAccessLevel.WRITE)));
        var snapshot = new top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot(
                "1001", "identity-1", "2001", "cms", 1, 2, List.of(), null, null,
                Set.of("article:edit"), Set.of("article"), Map.of(), Map.of("article:edit:cms:article", policy),
                "sha256:fixture", NOW, NOW.plusSeconds(300));
        for (boolean fenced : List.of(false, true)) {
            var pdp = new top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService(
                    () -> new AuthorizationService.RuntimeAuthorizationContext(identity(), snapshot, fenced),
                    request -> null, request -> null, java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC));
            var actual = new Rbac3FieldWriteAuthorizationComponent(pdp, currentUser, converter, validator);
            if (fenced) {
                assertThatThrownBy(() -> actual.requireWritableFields(command(Set.of("title"))))
                        .isInstanceOf(org.springframework.security.authorization.AuthorizationDeniedException.class);
            } else {
                assertThat(actual.requireWritableFields(command(Set.of("title")))).isSameAs(policy);
            }
        }
    }

    private static FieldWriteAuthorizationCommand command(Set<String> fields) {
        return FieldWriteAuthorizationCommand.builder().permissionCode("article:edit").applicationCode("cms")
                .resourceCode("article").submittedFields(fields).build();
    }
    private static FieldPolicyDecision.FieldAccess access(FieldAccessLevel level) {
        return new FieldPolicyDecision.FieldAccess(level, level == FieldAccessLevel.MASKED_READ ? "DEFAULT" : null);
    }
    private static FieldPolicyDecision decision(Decision decision, Map<String, FieldPolicyDecision.FieldAccess> fields) {
        return new FieldPolicyDecision(decision, decision == Decision.INDETERMINATE ? "PDP_UNAVAILABLE" : "ALLOW", "1001", "2001",
                "article:edit", "cms", "article", fields, 1, 2, List.of("policy"), NOW);
    }
    private static IdentityPrincipal identity() {
        return new IdentityPrincipal("identity-1", "1001", "jti-1", Set.of("business"), NOW.minusSeconds(1), NOW.plusSeconds(300),
                AuthenticationContext.of("PASSWORD", NOW));
    }
}
