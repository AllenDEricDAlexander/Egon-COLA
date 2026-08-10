package top.egon.cola.component.ddc.admin.service.publish;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishTaskQueryRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcPublishTaskQueryServiceTest {

    @Test
    void normalizesFiltersAndUsesStableNewestFirstPaging() {
        DdcPublishTaskRepository repository = mock(DdcPublishTaskRepository.class);
        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setId("task-1");
        when(repository.search(
                eq("infra"), eq("prod"), eq("gateway"), eq("FAILED"),
                eq("019"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(task), PageRequest.of(0, 10), 1));
        DdcPublishTaskQueryService service =
                new DdcPublishTaskQueryService(repository);
        DdcPublishTaskQueryRequest request = new DdcPublishTaskQueryRequest();
        request.setBizCode(" infra ");
        request.setEnv(" prod ");
        request.setAppCode(" gateway ");
        request.setStatus(" FAILED ");
        request.setChangeId(" 019 ");

        var page = service.page(request, new PageQuery(1, 10));

        assertThat(page.getContent()).containsExactly(task);
        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(
                eq("infra"), eq("prod"), eq("gateway"), eq("FAILED"),
                eq("019"), pageable.capture());
        assertThat(pageable.getValue().getSort().toString())
                .isEqualTo("createdAt: DESC,id: DESC");
    }
}
