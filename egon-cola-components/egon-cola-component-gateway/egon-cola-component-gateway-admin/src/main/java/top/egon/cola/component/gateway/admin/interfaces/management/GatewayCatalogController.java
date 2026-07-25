package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogService;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/gateway/admin")
public class GatewayCatalogController {

    private final GatewayCatalogService service;

    public GatewayCatalogController(GatewayCatalogService service) {
        this.service = service;
    }

    @GetMapping("/applications/{applicationId}/catalog")
    public GatewayCatalogStore.CatalogTree catalog(
            @PathVariable String applicationId) {
        return service.catalog(applicationId);
    }

    @PostMapping("/applications/{applicationId}/manual-interface-groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceCreated createInterfaceGroup(
            @PathVariable String applicationId,
            @Valid @RequestBody ManualInterfaceGroupRequest request,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId) {
        String id = service.createManualInterfaceGroup(
                applicationId,
                new GatewayCatalogStore.ManualHierarchy(
                        request.businessCode(),
                        request.businessName(),
                        request.entityCode(),
                        request.entityName(),
                        request.interfaceGroupCode(),
                        request.interfaceGroupName(),
                        request.className(),
                        request.description()
                ),
                actor(actorId),
                audit()
        );
        return new ResourceCreated(id);
    }

    @PostMapping("/interface-groups/{interfaceGroupId}/manual-operations")
    @ResponseStatus(HttpStatus.CREATED)
    public GatewayCatalogService.OperationDetail createOperation(
            @PathVariable String interfaceGroupId,
            @Valid @RequestBody ManualOperationRequest request,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId) {
        return service.createManualOperation(
                interfaceGroupId,
                request.command(),
                actor(actorId),
                audit()
        );
    }

    @GetMapping("/operations/{operationId}")
    public GatewayCatalogService.OperationDetail operation(
            @PathVariable String operationId) {
        return service.detail(operationId);
    }

    @PutMapping("/operations/{operationId}/metadata")
    public GatewayCatalogService.OperationDetail updateMetadata(
            @PathVariable String operationId,
            @Valid @RequestBody ManualMetadataRequest request,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId) {
        return service.updateMetadata(
                operationId,
                new GatewayCatalogService.ManualMetadata(
                        request.summary(),
                        request.tags(),
                        request.owner()
                ),
                actor(actorId),
                audit()
        );
    }

    @PutMapping("/operations/{operationId}/manual-definition")
    public GatewayCatalogService.OperationDetail updateDefinition(
            @PathVariable String operationId,
            @Valid @RequestBody ManualDefinitionRequest request,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId) {
        return service.updateManualDefinition(
                operationId,
                request.definition(),
                actor(actorId),
                audit()
        );
    }

    @PostMapping("/operations/{operationId}/deprecate")
    public GatewayCatalogService.OperationDetail deprecate(
            @PathVariable String operationId,
            @RequestHeader(value = "X-Admin-Actor-Id",
                    defaultValue = "local-admin") String actorId) {
        return service.deprecate(
                operationId,
                actor(actorId),
                audit()
        );
    }

    private AdminActor actor(String id) {
        return new AdminActor(
                id,
                AdminActor.ActorType.USER,
                Set.of("*"),
                Set.of("GATEWAY_ADMIN")
        );
    }

    private RequestAuditContext audit() {
        return new RequestAuditContext(
                UuidV7.simpleString(),
                UuidV7.simpleString()
        );
    }

    public record ResourceCreated(String id) {
    }

    public record ManualInterfaceGroupRequest(
            @NotBlank String businessCode,
            @NotBlank String businessName,
            @NotBlank String entityCode,
            @NotBlank String entityName,
            @NotBlank String interfaceGroupCode,
            @NotBlank String interfaceGroupName,
            String className,
            String description
    ) {
    }

    public record ManualOperationRequest(
            @NotNull GatewayCatalogService.Protocol protocol,
            String httpMethod,
            String path,
            String serviceName,
            String fullMethodName,
            @NotBlank String providerServiceName,
            String group,
            String version,
            String transport,
            boolean externalAccessible,
            @NotNull ManualDefinitionRequest definition
    ) {

        private GatewayCatalogService.ManualOperation command() {
            return new GatewayCatalogService.ManualOperation(
                    protocol,
                    httpMethod,
                    path,
                    serviceName,
                    fullMethodName,
                    providerServiceName,
                    group,
                    version,
                    transport,
                    externalAccessible,
                    definition.definition()
            );
        }
    }

    public record ManualDefinitionRequest(
            String summary,
            @NotNull List<String> tags,
            @NotNull Map<String, Object> requestSchema,
            @NotNull Map<String, Object> responseSchema,
            @NotNull List<Map<String, Object>> errorSchema,
            Map<String, Object> descriptorSnapshot,
            @NotNull Map<String, Object> attributes,
            boolean externalAccessible
    ) {

        private GatewayCatalogService.ManualDefinition definition() {
            return new GatewayCatalogService.ManualDefinition(
                    summary,
                    tags,
                    requestSchema,
                    responseSchema,
                    errorSchema,
                    descriptorSnapshot,
                    attributes,
                    externalAccessible
            );
        }
    }

    public record ManualMetadataRequest(
            String summary,
            @NotNull List<String> tags,
            String owner
    ) {
    }
}
