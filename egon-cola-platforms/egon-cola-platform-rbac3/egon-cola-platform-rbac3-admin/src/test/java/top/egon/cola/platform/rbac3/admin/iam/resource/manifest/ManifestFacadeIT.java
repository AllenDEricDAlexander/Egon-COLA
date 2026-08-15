package top.egon.cola.platform.rbac3.admin.iam.resource.manifest;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.service.ManifestFacade;
import top.egon.cola.platform.rbac3.contract.manifest.ManifestResource;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.repository.ResourceManifestRepository;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.dto.SubmitCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.vo.StoredManifestVO;
import top.egon.cola.platform.rbac3.admin.iam.resource.manifest.domain.enums.ManifestSubmissionOutcomeEnum;

class ManifestFacadeIT {

    @Test
    void treatsSameBuildAndChecksumAsIdempotentButRejectsIdentityDrift() {
        var store = new InMemoryStore();
        var facade = new ManifestFacade(store, componentKey -> true);
        var command = command("checksum-1");

        assertEquals(ManifestSubmissionOutcomeEnum.ACCEPTED,
                facade.submit(command).outcome());
        assertEquals(ManifestSubmissionOutcomeEnum.IDEMPOTENT,
                facade.submit(command).outcome());

        Rbac3RuleViolation conflict = assertThrows(Rbac3RuleViolation.class,
                () -> facade.submit(command("checksum-2")));
        assertEquals("RESOURCE_MANIFEST_CONFLICT", conflict.reasonCode());
    }

    @Test
    void rejectsUnknownParentsIncompleteApisAndUnknownFieldResources() {
        var facade = new ManifestFacade(new InMemoryStore(), componentKey -> true);
        ManifestResource route = resource(
                "route-a", "missing-menu", "/route-a", "app.routeA", null, null);
        ResourceManifest unknownParent = manifest(
                List.of(route), List.of(), List.of());
        assertReason("RESOURCE_MANIFEST_INVALID",
                () -> facade.submit(command(unknownParent)));

        ManifestResource api = resource(
                "api-a", null, null, null, "operation-a", null);
        ResourceManifest incompleteApi = manifest(
                List.of(), List.of(api), List.of());
        assertReason("RESOURCE_MANIFEST_INVALID",
                () -> facade.submit(command(incompleteApi)));

        ResourceManifest unknownFieldResource = manifest(
                List.of(), List.of(), List.of(new ResourceManifest.FieldDefinition(
                        "missing-resource", "amount", "$.amount", "NUMBER",
                        "CONFIDENTIAL", "NONE", null, false, false)));
        assertReason("RESOURCE_MANIFEST_INVALID",
                () -> facade.submit(command(unknownFieldResource)));
    }

    private SubmitCommandDTO command(String checksum) {
        ResourceManifest manifest = new ResourceManifest(
                "1", "finance", "Finance", "5.3.2", "build-1", 3,
                Instant.parse("2026-07-30T10:00:00Z"), checksum,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        return new SubmitCommandDTO(
                "tenant-1", "application-1", "manifest-1", "definition-set-1", manifest);
    }

    private SubmitCommandDTO command(ResourceManifest manifest) {
        return new SubmitCommandDTO(
                "tenant-1", "application-1", "manifest-2", "definition-set-1", manifest);
    }

    private ResourceManifest manifest(
            List<ManifestResource> routes,
            List<ManifestResource> apis,
            List<ResourceManifest.FieldDefinition> fields) {
        return new ResourceManifest(
                "1", "finance", "Finance", "5.3.2", "build-2", 4,
                Instant.parse("2026-07-30T10:00:00Z"), "checksum-3",
                List.of(resource("finance", null, null, null, null, null)),
                List.of(), routes, List.of(), apis, fields);
    }

    private ManifestResource resource(
            String code,
            String parentCode,
            String path,
            String componentKey,
            String operationId,
            String requiredPermission) {
        return new ManifestResource(
                code, parentCode, code, 100, path, componentKey, requiredPermission,
                null, false, false, null, operationId,
                operationId == null ? null : "POST",
                operationId == null ? null : "/api/" + code,
                false,
                Map.of());
    }

    private void assertReason(String reason, org.junit.jupiter.api.function.Executable executable) {
        Rbac3RuleViolation violation = assertThrows(Rbac3RuleViolation.class, executable);
        assertEquals(reason, violation.reasonCode());
    }

    private static final class InMemoryStore implements ResourceManifestRepository {
        private final Map<String, StoredManifestVO> values = new HashMap<>();

        @Override
        public Optional<StoredManifestVO> findByBuild(
                String tenantId,
                String applicationId,
                String artifactVersion,
                String buildId) {
            return Optional.ofNullable(values.get(String.join(
                    "/", tenantId, applicationId, artifactVersion, buildId)));
        }

        @Override
        public void insert(StoredManifestVO manifest) {
            values.put(String.join("/", manifest.tenantId(), manifest.applicationId(),
                    manifest.artifactVersion(), manifest.buildId()), manifest);
        }
    }
}
