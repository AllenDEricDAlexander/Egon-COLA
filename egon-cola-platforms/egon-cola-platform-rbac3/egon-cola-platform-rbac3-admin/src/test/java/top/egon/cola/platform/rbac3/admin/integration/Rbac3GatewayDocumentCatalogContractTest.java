package top.egon.cola.platform.rbac3.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.discovery.MvcGatewayDefinitionContributor;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.assignment.application.AssignmentFacade;
import top.egon.cola.platform.rbac3.admin.audit.application.AuditQueryService;
import top.egon.cola.platform.rbac3.admin.auth.application.AuthenticationFacade;
import top.egon.cola.platform.rbac3.admin.auth.application.JwtKeyRingService;
import top.egon.cola.platform.rbac3.admin.auth.application.RefreshFacade;
import top.egon.cola.platform.rbac3.admin.auth.application.StepUpFacade;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.constraint.application.ConstraintFacade;
import top.egon.cola.platform.rbac3.admin.identity.application.IdentityMappingFacade;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.interfaces.http.AssignmentController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.SessionController;
import top.egon.cola.platform.rbac3.admin.interfaces.http.TenantUserDirectoryController;
import top.egon.cola.platform.rbac3.admin.management.application.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.participation.application.ParticipationFacade;
import top.egon.cola.platform.rbac3.admin.resource.application.ApplicationResourceFacade;
import top.egon.cola.platform.rbac3.admin.resource.application.ManifestFacade;
import top.egon.cola.platform.rbac3.admin.role.application.RoleFacade;
import top.egon.cola.platform.rbac3.admin.runtime.application.IdempotencyService;
import top.egon.cola.platform.rbac3.admin.runtime.application.RuntimeQueryService;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.admin.simulation.application.AuthorizationSimulationService;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationBootstrapService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
class Rbac3GatewayDocumentCatalogContractTest {

    private static final String HTTP_PACKAGE =
            "top.egon.cola.platform.rbac3.admin.interfaces.http";
    private static final Pattern VERSIONED_OPERATION =
            Pattern.compile("rbac3(?:-[a-z0-9]+)+-v\\d+");
    private static final Pattern RAW_SECRET = Pattern.compile(
            "(?i)(eyJ[a-z0-9_-]{20,}\\.[a-z0-9_-]{10,}|"
                    + "-----BEGIN [A-Z ]*PRIVATE KEY-----|"
                    + "\\$2[aby]\\$[0-9]{2}\\$|\\b[0-9a-f]{64,}\\b)");
    private static final Set<String> SENSITIVE_NAMES = Set.of(
            "password", "refreshtoken", "credential", "privatekey", "secret", "hash");

    @Autowired
    private RequestMappingHandlerMapping handlerMappings;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApplicationResourceFacade applicationResourceFacade;

    @MockitoBean
    private AssignmentFacade assignmentFacade;

    @MockitoBean
    private AuditQueryService auditQueryService;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private AuthorizationDecisionService authorizationDecisionService;

    @MockitoBean
    private AuthorizationSimulationService authorizationSimulationService;

    @MockitoBean
    private BootstrapQueryService bootstrapQueryService;

    @MockitoBean
    private ConstraintFacade constraintFacade;

    @MockitoBean
    private DatabaseClock databaseClock;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @MockitoBean
    private IdentityMappingFacade identityMappingFacade;

    @MockitoBean
    private JwtKeyRingService jwtKeyRingService;

    @MockitoBean
    private LongIdGenerator idGenerator;

    @MockitoBean
    private ManagementPolicyFacade managementPolicyFacade;

    @MockitoBean
    private ManifestFacade manifestFacade;

    @MockitoBean
    private ParticipationFacade participationFacade;

    @MockitoBean
    private RefreshFacade refreshFacade;

    @MockitoBean
    private RoleActivationCandidateService roleActivationCandidateService;

    @MockitoBean
    private RoleActivationFacade roleActivationFacade;

    @MockitoBean
    private RoleFacade roleFacade;

    @MockitoBean
    private RuntimeQueryService runtimeQueryService;

    @MockitoBean
    private SessionFacade sessionFacade;

    @MockitoBean
    private StepUpFacade stepUpFacade;

    @MockitoBean
    private SystemAuthorizationSnapshotService systemAuthorizationSnapshotService;

    @MockitoBean
    private AuthorizationBootstrapService authorizationBootstrapService;

    @MockitoBean
    private AssignmentController.SessionStrengthPort sessionStrengthPort;

    @MockitoBean
    private SessionController.SessionManagementPort sessionManagementPort;

    @MockitoBean
    private TenantUserDirectoryController.DirectoryCommandPort directoryCommandPort;

    @MockitoBean
    private TenantUserDirectoryController.DirectoryQueryPort directoryQueryPort;

    @Test
    void catalogExactlyDescribesEveryRbac3MvcOperationWithoutSensitiveExamples() {
        Map<String, HandlerContract> actual = actualMappings();
        var groups = new MvcGatewayDefinitionContributor(
                handlerMappings, reportingProperties(), objectMapper).discover();
        List<GatewayInterfaceDefinitionReport.Operation> operations = groups.stream()
                .flatMap(group -> group.interfaceGroup().operations().stream())
                .toList();
        Map<String, GatewayInterfaceDefinitionReport.Operation> catalog = operations.stream()
                .collect(Collectors.toMap(
                        operation -> normalizeIdentity(operation.methodIdentity()),
                        operation -> operation,
                        (left, right) -> {
                            throw new IllegalStateException(
                                    "duplicate catalog operation " + left.methodIdentity());
                        },
                        LinkedHashMap::new));

        assertThat(catalog.keySet()).containsExactlyInAnyOrderElementsOf(actual.keySet());
        assertThat(groups).allSatisfy(group -> {
            assertThat(group.interfaceGroup().sourceType()).isEqualTo("STARTER");
            assertThat(group.interfaceGroup().protocol()).isEqualTo("HTTP");
            assertThat(group.interfaceGroup().className()).startsWith(HTTP_PACKAGE);
        });

        List<String> operationNames = new ArrayList<>();
        catalog.forEach((identity, operation) -> {
            HandlerContract handler = actual.get(identity);
            GatewayOperation annotation = handler.operation();
            operationNames.add(operation.name());

            assertThat(operation.name()).isEqualTo(annotation.name())
                    .matches(VERSIONED_OPERATION);
            assertThat(operation.summary()).isEqualTo(annotation.summary()).isNotBlank();
            assertThat(operation.description()).isEqualTo(annotation.description());
            assertThat(operation.tags()).contains("rbac3")
                    .containsExactlyInAnyOrder(annotation.tags());
            assertThat(operation.tags().stream().anyMatch(tag -> !"rbac3".equals(tag)))
                    .as("capability-domain tag for %s", identity)
                    .isTrue();
            assertThat(operation.externalAccessible())
                    .isEqualTo(annotation.externalAccessible());
            assertThat(operation.attributes())
                    .containsEntry("httpMethod", handler.method())
                    .containsEntry("path", handler.path())
                    .containsEntry("responseMode", "TRANSPARENT");
            assertThat(stringValues(operation.attributes().get("consumes")))
                    .containsExactlyInAnyOrderElementsOf(handler.consumes());
            assertThat(stringValues(operation.attributes().get("produces")))
                    .containsExactlyInAnyOrderElementsOf(handler.produces());
            assertProviderIdentity(operation.providerService());
            assertThat(operation.parameters()).isNotNull();
            assertSensitiveSchemaSafe(operation.parameters(), false);
            assertSensitiveSchemaSafe(operation.requestSchema(), false);
            assertSensitiveSchemaSafe(operation.responseSchema(), false);
            assertNoRawSecret(operation.summary());
            assertNoRawSecret(operation.description());
        });
        assertThat(operationNames).doesNotHaveDuplicates();
        System.out.printf("RBAC3 Gateway document catalog operations: %d%n", operations.size());
    }

    private Map<String, HandlerContract> actualMappings() {
        Map<String, HandlerContract> result = new LinkedHashMap<>();
        handlerMappings.getHandlerMethods().forEach((mapping, handler) -> {
            if (!handler.getBeanType().getPackageName().equals(HTTP_PACKAGE)
                    || AnnotatedElementUtils.findMergedAnnotation(
                    handler.getBeanType(), RestController.class) == null) {
                return;
            }
            assertControllerAnnotations(handler);
            GatewayOperation operation = AnnotatedElementUtils.findMergedAnnotation(
                    handler.getMethod(), GatewayOperation.class);
            assertThat(operation)
                    .as("@GatewayOperation on %s", handler)
                    .isNotNull();
            paths(mapping).forEach(path -> methods(mapping).forEach(method -> {
                String identity = normalizeIdentity(method + " " + path);
                HandlerContract previous = result.put(identity, new HandlerContract(
                        method, normalizePath(path), media(mapping, true),
                        media(mapping, false), operation));
                assertThat(previous).as("unique MVC mapping %s", identity).isNull();
            }));
        });
        return result;
    }

    private void assertControllerAnnotations(HandlerMethod handler) {
        Class<?> type = handler.getBeanType();
        assertThat(AnnotatedElementUtils.findMergedAnnotation(type, RestController.class))
                .as("@RestController on %s", type.getName())
                .isNotNull();
        assertThat(AnnotatedElementUtils.findMergedAnnotation(type, EgonHttpService.class))
                .as("@EgonHttpService on %s", type.getName())
                .isNotNull();
        assertThat(AnnotatedElementUtils.findMergedAnnotation(type, GatewayInterfaceGroup.class))
                .as("@GatewayInterfaceGroup on %s", type.getName())
                .isNotNull();
    }

    private Set<String> paths(RequestMappingInfo mapping) {
        return mapping.getPatternValues().isEmpty()
                ? Set.of("/") : mapping.getPatternValues();
    }

    private Set<String> methods(RequestMappingInfo mapping) {
        Set<String> methods = mapping.getMethodsCondition().getMethods().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        return methods.isEmpty() ? Set.of("ANY") : methods;
    }

    private Set<String> media(RequestMappingInfo mapping, boolean consumes) {
        return (consumes
                ? mapping.getConsumesCondition().getConsumableMediaTypes()
                : mapping.getProducesCondition().getProducibleMediaTypes()).stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private String normalizeIdentity(String identity) {
        int separator = identity.indexOf(' ');
        return identity.substring(0, separator).toUpperCase(Locale.ROOT)
                + " " + normalizePath(identity.substring(separator + 1));
    }

    private String normalizePath(String path) {
        String normalized = path.replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.length() > 1 && normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    private GatewayReportingProperties reportingProperties() {
        GatewayReportingProperties properties = new GatewayReportingProperties();
        properties.setBizCode("rbac3");
        properties.setApplicationCode("rbac3-admin");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("5.3.2");
        return properties;
    }

    private List<String> stringValues(Object values) {
        return ((Collection<?>) values).stream().map(Object::toString).toList();
    }

    private void assertProviderIdentity(
            GatewayInterfaceDefinitionReport.ProviderService provider) {
        assertThat(provider.bizCode()).isEqualTo("rbac3");
        assertThat(provider.appCode()).isEqualTo("rbac3-admin");
        assertThat(provider.env()).isEqualTo("test");
        assertThat(provider.namespace()).isEqualTo("default");
        assertThat(provider.protocol()).isEqualTo("HTTP");
        assertThat(provider.serviceName()).isEqualTo("rbac3-admin");
        assertThat(provider.group()).isEqualTo("default");
        assertThat(provider.version()).isEqualTo("5.3.2");
        assertThat(provider.transport()).isEqualTo("HTTP");
    }

    private void assertSensitiveSchemaSafe(Object value, boolean sensitiveNode) {
        if (value instanceof Map<?, ?> map) {
            if (sensitiveNode) {
                assertThat(map.keySet().stream().map(Object::toString))
                        .doesNotContain("example", "default");
            }
            map.forEach((key, nested) -> {
                String name = key.toString();
                if ("description".equalsIgnoreCase(name) && nested != null) {
                    assertNoRawSecret(nested.toString());
                }
                assertSensitiveSchemaSafe(
                        nested, sensitiveNode || sensitiveName(name));
            });
            return;
        }
        if (value instanceof Iterable<?> values) {
            values.forEach(nested -> assertSensitiveSchemaSafe(nested, sensitiveNode));
            return;
        }
        if (sensitiveNode && value != null) {
            assertNoRawSecret(value.toString());
        }
    }

    private boolean sensitiveName(String name) {
        String normalized = name.replaceAll("[^A-Za-z]", "")
                .toLowerCase(Locale.ROOT);
        return SENSITIVE_NAMES.stream().anyMatch(normalized::contains);
    }

    private void assertNoRawSecret(String value) {
        assertThat(value).doesNotMatch(RAW_SECRET);
    }

    private record HandlerContract(
            String method,
            String path,
            Set<String> consumes,
            Set<String> produces,
            GatewayOperation operation) {
    }
}
