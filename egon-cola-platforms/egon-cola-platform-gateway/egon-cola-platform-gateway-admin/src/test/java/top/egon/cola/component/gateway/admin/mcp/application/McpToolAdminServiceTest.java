package top.egon.cola.component.gateway.admin.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.shared.repository.IdempotencyRepository;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.routing.domain.po.GatewayDraftPO;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftJpaRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpManagedToolOverrideRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpRemoteProviderRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpRemoteToolDraftRepository;
import top.egon.cola.component.gateway.admin.mcp.domain.po.McpServerPO;
import top.egon.cola.component.gateway.admin.mcp.repository.McpServerRepository;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpToolAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-06T00:00:00Z");

    private static final AdminActor ACTOR = new AdminActor(
            "admin-1",
            top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum.USER,
            Set.of(),
            Set.of()
    );

    private McpReleaseContentFactory contentFactory;

    private McpValidationService validation;

    private JdbcMcpManagedToolOverrideRepository managedOverrides;

    private JdbcMcpRemoteToolDraftRepository remoteTools;

    private JdbcMcpRemoteProviderRepository remote;

    private McpServerRepository servers;

    private GatewayDraftJpaRepository drafts;

    private IdempotencyRepository idempotency;

    private McpToolAdminService service;

    @BeforeEach
    void setUp() {
        contentFactory = mock(McpReleaseContentFactory.class);
        validation = mock(McpValidationService.class);
        managedOverrides = mock(JdbcMcpManagedToolOverrideRepository.class);
        remoteTools = mock(JdbcMcpRemoteToolDraftRepository.class);
        remote = mock(JdbcMcpRemoteProviderRepository.class);
        servers = mock(McpServerRepository.class);
        drafts = mock(GatewayDraftJpaRepository.class);
        idempotency = mock(IdempotencyRepository.class);
        service = new McpToolAdminService(
                contentFactory,
                validation,
                managedOverrides,
                remoteTools,
                remote,
                servers,
                drafts,
                idempotency,
                mock(GatewayAuditLogRepository.class),
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createRemoteToolReplaysWithTheOriginalGeneratedId() {
        AtomicReference<top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO> saved =
                new AtomicReference<>();
        when(idempotency.find(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(saved.get()));
        doAnswer(invocation -> {
            saved.set(invocation.getArgument(0));
            return null;
        }).when(idempotency).save(any());
        prepareRemoteToolDependencies("server-1");
        when(remoteTools.save(
                any(top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteToolDraftPO.class),
                eq(0L),
                eq(ACTOR),
                eq(NOW)
        )).thenAnswer(invocation -> {
            top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteToolDraftPO draft =
                    invocation.getArgument(0);
            return new top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolDraftMutationDTO(
                    draft.id(),
                    0
            );
        });
        top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolMutationDTO command = mutation();

        top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO created = service.putRemoteTool(
                null,
                command,
                "create-remote-tool-1",
                ACTOR,
                new RequestAuditContext("request-1", "trace-1")
        );
        top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO replayed = service.putRemoteTool(
                null,
                command,
                "create-remote-tool-1",
                ACTOR,
                new RequestAuditContext("request-2", "trace-2")
        );

        assertThat(created.replayed()).isFalse();
        assertThat(replayed.replayed()).isTrue();
        assertThat(replayed.resourceId()).isEqualTo(created.resourceId());
        assertThat(replayed.resourceRevision())
                .isEqualTo(created.resourceRevision());
        verify(remoteTools).save(
                any(top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteToolDraftPO.class),
                eq(0L),
                eq(ACTOR),
                eq(NOW)
        );
    }

    @Test
    void rejectsRemoteMountFromAnotherServerBeforeWriting() {
        prepareRemoteToolDependencies("server-2");
        when(idempotency.find(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.putRemoteTool(
                null,
                mutation(),
                "create-remote-tool-2",
                ACTOR,
                new RequestAuditContext("request-1", "trace-1")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("remote MCP Tool mount belongs to another Server");

        verify(remoteTools, never()).save(any(), any(Long.class), any(), any());
        verify(drafts, never()).findById(anyString());
    }

    @Test
    void rejectsRemovingExistingManagedToolPermissions() {
        prepareManagedTool(Set.of("mcp:orders:audit"), "HIGH", true);

        assertThatThrownBy(() -> service.putOverride(
                "tool-1",
                override(Set.of(), "HIGH", null),
                "managed-tool-permissions",
                ACTOR,
                new RequestAuditContext("request-1", "trace-1")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot remove existing permissions");

        verify(managedOverrides, never()).save(any(), any(Long.class), any(), any());
    }

    @Test
    void rejectsLoweringExistingManagedToolRisk() {
        prepareManagedTool(Set.of(), "HIGH", true);

        assertThatThrownBy(() -> service.putOverride(
                "tool-1",
                override(Set.of(), "MEDIUM", null),
                "managed-tool-risk",
                ACTOR,
                new RequestAuditContext("request-1", "trace-1")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot lower existing minimum risk");

        verify(managedOverrides, never()).save(any(), any(Long.class), any(), any());
    }

    @Test
    void rejectsReenablingManagedToolWithoutDeletingOverride() {
        prepareManagedTool(Set.of(), null, false);

        assertThatThrownBy(() -> service.putOverride(
                "tool-1",
                override(Set.of(), null, null),
                "managed-tool-enabled",
                ACTOR,
                new RequestAuditContext("request-1", "trace-1")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only be restored by deleting")
                .hasMessageContaining("override");

        verify(managedOverrides, never()).save(any(), any(Long.class), any(), any());
    }

    private void prepareRemoteToolDependencies(String mountServerId) {
        McpServerPO server = new McpServerPO(
                "server-1",
                "group-1",
                "orders",
                "Orders",
                null,
                null,
                Set.of("STABLE_2025_11_25"),
                "https://resource.egon.top/gateway-mcp",
                30,
                ACTOR,
                NOW
        );
        when(servers.findByIdAndDeletedFalse("server-1"))
                .thenReturn(Optional.of(server));
        when(remote.mounts("group-1")).thenReturn(List.of(
                new top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO(
                        "mount-1",
                        "group-1",
                        mountServerId,
                        "provider-1",
                        "billing",
                        "fingerprint-1",
                        Map.of(),
                        true,
                        0
                )
        ));
        when(drafts.findById("group-1")).thenReturn(Optional.of(
                new GatewayDraftPO("group-1", ACTOR.actorId(), NOW)
        ));
    }

    private void prepareManagedTool(
            Set<String> additionalPermissions,
            String minimumRisk,
            boolean enabled) {
        when(idempotency.find(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        McpRuntimeTool tool = new McpRuntimeTool(
                "tool-1",
                "orders",
                "orders.get",
                "Get one order",
                "LOCAL_OPERATION",
                "operation-1",
                "HTTP",
                null,
                "{\"type\":\"object\"}",
                "{\"type\":\"object\"}",
                Map.of(),
                Set.of("mcp:orders:read"),
                minimumRisk == null ? "LOW" : minimumRisk,
                true,
                enabled
        );
        when(contentFactory.managedTools("group-1")).thenReturn(List.of(
                new top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolProjectionVO(
                        "group-1",
                        "http:orders:GET:/orders/{id}",
                        "server-1",
                        "orders",
                        "server-1",
                        Set.of("mcp:orders:read"),
                        additionalPermissions,
                        "LOW",
                        minimumRisk,
                        3,
                        tool
                )
        ));
    }

    private top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideMutationDTO override(
            Set<String> permissions,
            String minimumRisk,
            Boolean enabled) {
        return new top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideMutationDTO(
                "group-1",
                enabled,
                null,
                permissions,
                minimumRisk,
                3,
                0,
                "tighten managed Tool"
        );
    }

    private top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolMutationDTO mutation() {
        return new top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolMutationDTO(
                "group-1",
                "server-1",
                "remote.orders.get",
                "Get one remote order",
                "mount-1",
                Map.of("type", "object"),
                Map.of("type", "object"),
                Map.of("readOnlyHint", "true"),
                Set.of("mcp:orders:read"),
                "LOW",
                true,
                true,
                0,
                0,
                "add remote Tool"
        );
    }
}
