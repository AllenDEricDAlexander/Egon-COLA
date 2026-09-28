package top.egon.cola.component.tianshu.admin.service.publish;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublishFailureRecorderTest {

    @Test
    void insertsAFailedTaskThroughTheMpRepositoryWhenPrepareDidNotPersistOne() {
        DdcPublishTaskRepository repository = mock(DdcPublishTaskRepository.class);
        when(repository.findByChangeId("123")).thenReturn(Optional.empty());
        when(repository.insert(any(DdcPublishTaskBO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PublishFailureRecorder recorder = new PublishFailureRecorder(repository);

        recorder.recordFailure("123", "demo", "dev", "default", "switch", "database failed");

        var task = org.mockito.ArgumentCaptor.forClass(DdcPublishTaskBO.class);
        verify(repository).insert(task.capture());
        assertThat(task.getValue().getStatus()).isEqualTo(PublishStatus.FAILED.name());
        assertThat(task.getValue().getErrorMessage()).isEqualTo("database failed");
        assertThat(task.getValue().getChangeId()).isEqualTo("123");
        assertThat(task.getValue().getBizCode()).isEqualTo("demo");
    }

    @Test
    void updatesTheExistingFailedTaskWithoutReinsertingIt() {
        DdcPublishTaskRepository repository = mock(DdcPublishTaskRepository.class);
        DdcPublishTaskBO existing = DdcPublishTaskBO.builder().id(5L).version(2L)
                .changeId("123").status(PublishStatus.PENDING.name()).build();
        when(repository.findByChangeId("123")).thenReturn(Optional.of(existing));
        when(repository.update(any(DdcPublishTaskBO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PublishFailureRecorder recorder = new PublishFailureRecorder(repository);

        recorder.recordFailure("123", "demo", "dev", "default", "switch", "redis failed");

        verify(repository).update(existing);
        assertThat(existing.getStatus()).isEqualTo(PublishStatus.FAILED.name());
        assertThat(existing.getErrorMessage()).isEqualTo("redis failed");
    }
}
