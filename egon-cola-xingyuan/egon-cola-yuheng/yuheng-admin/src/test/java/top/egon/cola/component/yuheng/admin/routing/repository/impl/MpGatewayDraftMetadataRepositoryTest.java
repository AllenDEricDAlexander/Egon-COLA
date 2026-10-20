package top.egon.cola.component.yuheng.admin.routing.repository.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.yuheng.admin.routing.converter.GatewayDraftPersistenceConverter;
import top.egon.cola.component.yuheng.admin.routing.domain.bo.GatewayDraftBO;
import top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayDraftRecordPO;
import top.egon.cola.component.yuheng.admin.routing.repository.GatewayDraftJpaRepository;
import top.egon.cola.component.yuheng.admin.routing.repository.mp.GatewayDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MpGatewayDraftMetadataRepositoryTest {

    private final GatewayDraftPersistenceRepository persistence = mock(GatewayDraftPersistenceRepository.class);
    private final GatewayDraftJpaRepository repository = new MpGatewayDraftMetadataRepository(
            persistence, new GatewayDraftPersistenceConverter());

    @Test
    void existingDraftPortUsesTheGuardedCasAndReturnsTheStoredRevision() {
        GatewayDraftRecordPO row = row();
        when(persistence.getOneOpt(any())).thenReturn(Optional.of(row));
        when(persistence.updateById(any(GatewayDraftRecordPO.class))).thenReturn(true);
        GatewayDraftBO draft = repository.findById("91").orElseThrow();
        draft.touch("route updated", "service:test", Instant.parse("2026-10-18T00:00:00Z"));

        GatewayDraftBO saved = repository.save(draft);

        assertThat(saved.getRevision()).isEqualTo(5L);
        assertThat(saved.getChangeSummary()).isEqualTo("route updated");
        ArgumentCaptor<GatewayDraftRecordPO> changed = ArgumentCaptor.forClass(GatewayDraftRecordPO.class);
        verify(persistence).updateById(changed.capture());
        assertThat(changed.getValue().getId()).isEqualTo(99L);
        assertThat(changed.getValue().getVersion()).isEqualTo(12L);
        assertThat(changed.getValue().getRevision()).isEqualTo(5L);
    }

    @Test
    void staleBusinessRevisionCannotReachTheWriteBoundary() {
        when(persistence.getOneOpt(any())).thenReturn(Optional.of(row()));
        GatewayDraftBO stale = new GatewayDraftBO("91", "service:test", Instant.EPOCH).setRevision(3L);

        assertThatThrownBy(() -> repository.save(stale)).isInstanceOf(GatewayAdminRevisionConflictException.class);
        verify(persistence, never()).updateById(any(GatewayDraftRecordPO.class));
    }

    @Test
    void lostPersistenceCasDoesNotReportSuccess() {
        when(persistence.getOneOpt(any())).thenReturn(Optional.of(row()));
        when(persistence.updateById(any(GatewayDraftRecordPO.class))).thenReturn(false);
        GatewayDraftBO current = repository.findById("91").orElseThrow();

        assertThatThrownBy(() -> repository.save(current)).isInstanceOf(GatewayAdminRevisionConflictException.class);
    }

    private GatewayDraftRecordPO row() {
        GatewayDraftRecordPO row = new GatewayDraftRecordPO();
        row.setId(99L);
        row.setVersion(12L);
        row.setGatewayGroupId(91L);
        row.setRevision(4L);
        row.setStatus("EDITABLE");
        return row;
    }
}
