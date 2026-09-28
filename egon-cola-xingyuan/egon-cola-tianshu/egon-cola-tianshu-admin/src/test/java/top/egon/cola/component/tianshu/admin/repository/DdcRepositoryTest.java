package top.egon.cola.component.tianshu.admin.repository;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.po.DdcConfigItemPO;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRepositoryTest {

    @Test
    void configItemPersistenceBoundaryUsesLongTechnicalIdAndTenantScopedMp() throws Exception {
        assertThat(DdcConfigItemPO.class.getSuperclass()).isEqualTo(EgonModel.class);
        assertThat(DdcConfigItemPO.class.getAnnotation(TableName.class).value()).isEqualTo("ddc_config_item");
        assertThat(EgonModel.class.isAssignableFrom(DdcConfigItemBO.class)).isFalse();
        assertThat(EgonColaRepository.class.isAssignableFrom(
                Class.forName("top.egon.cola.component.tianshu.admin.repository.impl.DdcConfigItemRepositoryImpl")))
                .isTrue();
        assertThat(DdcConfigItemPO.class.getDeclaredField("deleted")).isNotNull();
        assertThat(DdcConfigItemPO.class.getSuperclass().getDeclaredField("deletedAt")).isNotNull();
    }
}
