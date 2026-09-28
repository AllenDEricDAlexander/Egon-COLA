package top.egon.cola.component.tianshu.admin.service.publish;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcPublishTaskQueryRequest;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcPublishTaskQueryServiceTest {

    @Test
    void normalizesFiltersAndUsesRepositoryPageQuery() {
        DdcPublishTaskRepository repository = mock(DdcPublishTaskRepository.class);
        DdcPublishTaskBO task = DdcPublishTaskBO.builder().changeId("change-1").build();
        when(repository.search(
                eq("infra"), eq("prod"), eq("yuheng"), eq("FAILED"),
                eq("019"), any(PageQuery.class))).thenReturn(DdcPageResult.<DdcPublishTaskBO>builder()
                .records(List.of(task)).total(1).pageNo(1).pageSize(10).build());
        DdcPublishTaskQueryService service =
                new DdcPublishTaskQueryService(repository);
        DdcPublishTaskQueryRequest request = new DdcPublishTaskQueryRequest();
        request.setBizCode(" infra ");
        request.setEnv(" prod ");
        request.setAppCode(" yuheng ");
        request.setStatus(" FAILED ");
        request.setChangeId(" 019 ");

        var page = service.page(request, new PageQuery(1, 10));

        assertThat(page.getRecords()).containsExactly(task);
        var pageQuery = org.mockito.ArgumentCaptor.forClass(PageQuery.class);
        verify(repository).search(
                eq("infra"), eq("prod"), eq("yuheng"), eq("FAILED"),
                eq("019"), pageQuery.capture());
        assertThat(pageQuery.getValue().pageNo()).isEqualTo(1);
        assertThat(pageQuery.getValue().pageSize()).isEqualTo(10);
    }
}
