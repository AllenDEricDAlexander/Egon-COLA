package top.egon.cola.component.ddc.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.annotation.DdcValue;
import top.egon.cola.component.ddc.common.DdcValueConverter;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;

import static org.assertj.core.api.Assertions.assertThat;

class DdcFieldBindingServiceTest {

    static class SampleBean {

        @DdcValue("limit:1")
        private volatile Integer limit;
    }

    @Test
    void bindsAndAssignsAnnotatedField() {
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        DdcFieldBindingService service = new DdcFieldBindingService(repository, new DdcValueConverter());
        SampleBean bean = new SampleBean();

        service.bind(bean, SampleBean.class);
        service.apply("limit", "5", 2L);

        assertThat(bean.limit).isEqualTo(5);
        assertThat(repository.version("limit")).isEqualTo(2L);
        assertThat(repository.bindings("limit")).hasSize(1);
    }
}
