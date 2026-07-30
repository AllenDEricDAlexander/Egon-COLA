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

    static class MultiFieldBean {

        @DdcValue("limit:1")
        private volatile Integer first;

        @DdcValue("limit:1")
        private volatile Integer second;
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

    @Test
    void conversionFailureDoesNotWriteOrAdvanceVersion() {
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        DdcFieldBindingService service = new DdcFieldBindingService(repository, new DdcValueConverter());
        SampleBean bean = new SampleBean();
        service.bind(bean, SampleBean.class);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.apply("limit", "bad", 2L))
                .isInstanceOf(Exception.class);

        assertThat(bean.limit).isEqualTo(1);
        assertThat(repository.version("limit")).isZero();
    }

    @Test
    void secondWriteFailureRestoresTheFirstFieldAndKeepsTheOldVersion() {
        DdcLocalConfigRepository repository = new DdcLocalConfigRepository();
        FailingSecondWriteService service =
                new FailingSecondWriteService(repository, new DdcValueConverter());
        MultiFieldBean bean = new MultiFieldBean();
        service.bind(bean, MultiFieldBean.class);
        service.arm();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.apply("limit", "5", 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("second write failed");

        assertThat(bean.first).isEqualTo(1);
        assertThat(bean.second).isEqualTo(1);
        assertThat(repository.version("limit")).isZero();
    }

    private static class FailingSecondWriteService extends DdcFieldBindingService {

        private int writes;

        private boolean armed;

        private FailingSecondWriteService(DdcLocalConfigRepository repository,
                                          DdcValueConverter converter) {
            super(repository, converter);
        }

        private void arm() {
            writes = 0;
            armed = true;
        }

        @Override
        protected void write(top.egon.cola.component.ddc.model.vo.DdcFieldBinding binding,
                             Object value) {
            if (armed && Integer.valueOf(5).equals(value) && writes++ == 1) {
                throw new IllegalStateException("second write failed");
            }
            super.write(binding, value);
        }
    }
}
