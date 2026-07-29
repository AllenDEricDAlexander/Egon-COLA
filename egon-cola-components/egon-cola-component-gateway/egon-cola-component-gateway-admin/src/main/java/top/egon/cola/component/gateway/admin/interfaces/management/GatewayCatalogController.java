package top.egon.cola.component.gateway.admin.interfaces.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogService;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gateway/admin")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
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
    @PreAuthorize("hasAnyAuthority('CAP_gateway:catalog:write','CAP_*')")
    public ResourceCreated createInterfaceGroup(
            @PathVariable String applicationId,
            @Valid @RequestBody ManualInterfaceGroupRequest request,
            AdminActor actor) {
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
                actor,
                audit()
        );
        return new ResourceCreated(id);
    }

    @PostMapping("/interface-groups/{interfaceGroupId}/manual-operations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:catalog:write','CAP_*')")
    public GatewayCatalogService.OperationDetail createOperation(
            @PathVariable String interfaceGroupId,
            @Valid @RequestBody ManualOperationRequest request,
            AdminActor actor) {
        return service.createManualOperation(
                interfaceGroupId,
                request.command(),
                actor,
                audit()
        );
    }

    @GetMapping("/operations/{operationId}")
    public GatewayCatalogService.OperationDetail operation(
            @PathVariable String operationId) {
        return service.detail(operationId);
    }

    @PutMapping("/operations/{operationId}/metadata")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:catalog:write','CAP_*')")
    public GatewayCatalogService.OperationDetail updateMetadata(
            @PathVariable String operationId,
            @Valid @RequestBody ManualMetadataRequest request,
            AdminActor actor) {
        return service.updateMetadata(
                operationId,
                new GatewayCatalogService.ManualMetadata(
                        request.summary(),
                        request.tags(),
                        request.owner()
                ),
                actor,
                audit()
        );
    }

    @PutMapping("/operations/{operationId}/manual-definition")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:catalog:write','CAP_*')")
    public GatewayCatalogService.OperationDetail updateDefinition(
            @PathVariable String operationId,
            @Valid @RequestBody ManualDefinitionRequest request,
            AdminActor actor) {
        return service.updateManualDefinition(
                operationId,
                request.definition(),
                actor,
                audit()
        );
    }

    @PostMapping("/operations/{operationId}/deprecate")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:catalog:write','CAP_*')")
    public GatewayCatalogService.OperationDetail deprecate(
            @PathVariable String operationId,
            AdminActor actor) {
        return service.deprecate(
                operationId,
                actor,
                audit()
        );
    }

    private RequestAuditContext audit() {
        return RequestAuditContext.current();
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
