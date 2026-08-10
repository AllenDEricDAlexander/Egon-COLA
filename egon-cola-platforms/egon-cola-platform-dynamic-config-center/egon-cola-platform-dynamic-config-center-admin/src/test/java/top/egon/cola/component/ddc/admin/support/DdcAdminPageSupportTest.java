package top.egon.cola.component.ddc.admin.support;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAdminPageSupportTest {

    @Test
    void normalizesPageRequestAndBuildsPublicResult() {
        Pageable pageable = DdcAdminPageSupport.pageable(
                new PageQuery(2, 20),
                Sort.by("bizCode").ascending()
        );
        Page<String> page = new PageImpl<>(
                List.of("pay"), pageable, 21
        );

        PageResultRecord<String> result = DdcAdminPageSupport.result(page);

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(result.records()).containsExactly("pay");
        assertThat(result.page().total()).isEqualTo(21);
        assertThat(result.page().pageNo()).isEqualTo(2);
        assertThat(result.page().pageSize()).isEqualTo(20);
    }

    @Test
    void slicesOnlyAggregateRecords() {
        Page<String> page = DdcAdminPageSupport.slice(
                List.of("a", "b", "c"), new PageQuery(2, 2)
        );

        assertThat(page.getContent()).containsExactly("c");
        assertThat(page.getTotalElements()).isEqualTo(3);
    }
}
