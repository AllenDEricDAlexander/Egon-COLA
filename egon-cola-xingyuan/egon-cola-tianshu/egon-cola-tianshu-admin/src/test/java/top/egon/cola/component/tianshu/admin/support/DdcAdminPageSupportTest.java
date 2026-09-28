package top.egon.cola.component.tianshu.admin.support;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAdminPageSupportTest {

    @Test
    void slicesRequestedWindowAndBuildsPublicResult() {
        List<String> records = java.util.stream.Stream.concat(
                java.util.stream.IntStream.range(0, 20).mapToObj("record-%d"::formatted),
                java.util.stream.Stream.of("pay")
        ).toList();
        DdcPageResult<String> page = DdcAdminPageSupport.slice(records, new PageQuery(2, 20));
        PageResultRecord<String> result = DdcAdminPageSupport.result(page);

        assertThat(result.records()).containsExactly("pay");
        assertThat(result.page().total()).isEqualTo(21);
        assertThat(result.page().pageNo()).isEqualTo(2);
        assertThat(result.page().pageSize()).isEqualTo(20);
    }

    @Test
    void slicesOnlyAggregateRecords() {
        DdcPageResult<String> page = DdcAdminPageSupport.slice(
                List.of("a", "b", "c"), new PageQuery(2, 2)
        );

        assertThat(page.getRecords()).containsExactly("c");
        assertThat(page.getTotal()).isEqualTo(3);
    }
}
