package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcMcpControlPlaneStoreTest {

    private static final Instant NOW = Instant.parse(
            "2026-08-02T00:00:00Z"
    );

    private final AdminActor actor = new AdminActor(
            "admin-1",
            AdminActor.ActorType.USER,
            Set.of(),
            Set.of()
    );

    @Test
    void managedOverrideAndRemoteToolFenceEveryMutableRevision() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(any(String.class), any(Object[].class)))
                .thenReturn(1);
        JdbcMcpManagedToolOverrideStore managed =
                new JdbcMcpManagedToolOverrideStore(jdbc, new ObjectMapper());
        JdbcMcpRemoteToolDraftStore remoteTools =
                new JdbcMcpRemoteToolDraftStore(jdbc, new ObjectMapper());
        JdbcMcpRemoteProviderStore remote =
                new JdbcMcpRemoteProviderStore(jdbc, new ObjectMapper());

        var mutation = managed.save(
                new JdbcMcpManagedToolOverrideStore.ManagedToolOverride(
                        "tool-1",
                        "group-1",
                        "operation-1",
                        "server-1",
                        Set.of("mcp:orders:admin"),
                        "HIGH",
                        false,
                        3
                ),
                3,
                actor,
                NOW
        );
        remoteTools.save(
                new JdbcMcpRemoteToolDraftStore.RemoteToolDraft(
                        "remote-tool-1",
                        "group-1",
                        "server-1",
                        "remote.orders",
                        "mount-1",
                        Map.of("riskLevel", "LOW"),
                        true,
                        2
                ),
                2,
                actor,
                NOW
        );
        remote.saveProvider(
                new JdbcMcpRemoteProviderStore.RemoteProviderDraft(
                        "provider-1",
                        "group-1",
                        "billing",
                        Map.of(
                                "displayName", "Billing",
                                "dialect", "STABLE_2025_11_25",
                                "transportType", "STREAMABLE_HTTP",
                                "endpointReference", "endpoint:billing"
                        ),
                        true,
                        5
                ),
                5,
                actor,
                NOW
        );

        assertEquals(4, mutation.revision());
        verify(jdbc).update(
                contains("gateway_mcp_managed_tool_override"),
                any(Object[].class)
        );
        verify(jdbc).update(
                contains("gateway_mcp_remote_tool_draft"),
                any(Object[].class)
        );
        verify(jdbc).update(
                contains("gateway_mcp_remote_provider"),
                any(Object[].class)
        );
    }

    @Test
    void artifactsAreInsertOnlyAndApprovalConsumptionIsOneTime() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(any(String.class), any(Object[].class)))
                .thenReturn(1);
        JdbcMcpArtifactMetadataStore artifacts =
                new JdbcMcpArtifactMetadataStore(jdbc, new ObjectMapper());
        JdbcMcpApprovalStore approvals = new JdbcMcpApprovalStore(jdbc);

        artifacts.save(new JdbcMcpArtifactMetadataStore.ArtifactMetadata(
                "artifact-1",
                "group-1",
                "billing-ui",
                "1.0.0",
                "Billing UI",
                "ui://billing/index.html",
                "artifact:billing-ui:1.0.0",
                "a".repeat(64),
                1024,
                "text/html;profile=mcp-app",
                "default-src 'none'",
                Set.of("mcp:billing:read"),
                Set.of(),
                "admin-1",
                NOW
        ));
        boolean consumed = approvals.consume(
                "b".repeat(64),
                "subject-1",
                "tenant-1",
                "client-1",
                "billing",
                "invoice.approve",
                "c".repeat(64),
                NOW
        );

        assertTrue(consumed);
        verify(jdbc).update(
                contains("INSERT INTO gateway_mcp_app_artifact"),
                any(Object[].class)
        );
        verify(jdbc).update(
                contains("status = 'PENDING'"),
                any(Object[].class)
        );
    }

    @Test
    void taskClaimRequiresWorkingStateAndAnExpiredLease() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(any(String.class), any(Object[].class)))
                .thenReturn(1);
        JdbcMcpTaskStore tasks = new JdbcMcpTaskStore(
                jdbc,
                new ObjectMapper()
        );

        boolean claimed = tasks.claim(
                "task-1",
                "worker-1",
                NOW,
                NOW.plusSeconds(30),
                2
        );

        assertTrue(claimed);
        verify(jdbc).update(
                contains("state = 'WORKING'"),
                any(Object[].class)
        );
    }
}
