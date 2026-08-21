package top.egon.cola.component.common.mybatis.integration;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessMapper;
import top.egon.cola.component.common.mybatis.support.TestBusinessService;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;
import top.egon.cola.component.common.mybatis.support.TestUserIdProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ActiveRecord dispatch and lifecycle proof over the same H2/MyBatis chain.
 */
class EgonColaActiveRecordIntegrationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EgonColaMybatisPlusAutoConfiguration.class,
                    MybatisPlusInnerInterceptorAutoConfiguration.class,
                    MybatisPlusAutoConfiguration.class))
            .withUserConfiguration(EgonColaTenantIdSqlIntegrationTest.H2Configuration.class,
                    ArMapperConfiguration.class)
            .withBean(TestTenantIdProvider.class, TestTenantIdProvider::new)
            .withBean(TestUserIdProvider.class, TestUserIdProvider::new)
            .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
            .withBean(Clock.class, () -> Clock.fixed(
                    Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void allArFamiliesShareTenantFillLogicDeleteAndLifecycleHooks() {
        contextRunner.run(context -> {
            TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
            TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
            tenant.set(31L);
            user.set("ar-user");

            HookedBusinessModel model = new HookedBusinessModel().values("ar-title", "payload");
            model.setVersion(1L);
            assertThat(model.insert()).isTrue();
            assertThat(model.events).containsExactly("beforeInsert", "afterInsert:true");
            assertThat(model.getTenantId()).isEqualTo(31L);
            assertThat(model.selectAll()).hasSize(1);
            assertThat(model.selectById(model.getId())).isNotNull();
            assertThat(model.selectById()).isNotNull();
            assertThat(model.selectList(Wrappers.emptyWrapper())).hasSize(1);
            assertThat(model.selectOne(Wrappers.<HookedBusinessModel>query().eq("title", "ar-title")))
                    .isNotNull();
            assertThat(model.selectCount(Wrappers.emptyWrapper())).isEqualTo(1L);
            assertThat(model.selectPage(new Page<>(1, 10), Wrappers.emptyWrapper()).getRecords())
                    .hasSize(1);

            model.setTitle("ar-updated");
            assertThat(model.updateById()).isTrue();
            assertThat(model.events).contains("beforeUpdate", "afterUpdate:true");

            tenant.set(32L);
            assertThat(model.selectById(model.getId())).isNull();
            tenant.set(31L);
            assertThat(model.deleteById()).isTrue();
            assertThat(model.selectById()).isNull();

            HookedBusinessModel upsert = new HookedBusinessModel().values("upsert", null);
            assertThat(upsert.insertOrUpdate()).isTrue();
            upsert.setTitle("upsert-updated");
            assertThat(upsert.insertOrUpdate()).isTrue();
            assertThat(upsert.events).containsExactly(
                    "beforeInsert", "afterInsert:true", "beforeUpdate", "afterUpdate:true");
        });
    }

    @Test
    void arWritesFailClosedForMissingContextInvalidModelAndWideWrapper() {
        contextRunner.run(context -> {
            TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
            TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
            HookedBusinessModel invalid = new HookedBusinessModel().values("", null);
            tenant.set(31L);
            user.set("ar-user");
            assertThatThrownBy(invalid::insert).hasMessageContaining("title");
            assertThat(invalid.events).containsExactly("beforeInsert");

            tenant.clear();
            user.set("ar-user");
            assertThatThrownBy(() -> new HookedBusinessModel().values("valid", null).insert())
                    .hasMessageContaining("TENANT_CONTEXT_MISSING");

            tenant.set(31L);
            user.set("ar-user");
            HookedBusinessModel model = new HookedBusinessModel().values("valid", null);
            assertThat(model.insert()).isTrue();
            assertThatThrownBy(() -> model.update(Wrappers.emptyWrapper()))
                    .hasMessageContaining("Prohibition");
            assertThatThrownBy(() -> model.delete(Wrappers.emptyWrapper()))
                    .hasMessageContaining("Prohibition");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @MapperScan(basePackageClasses = HookedBusinessMapper.class)
    static class ArMapperConfiguration {
    }

    @Mapper
    interface HookedBusinessMapper extends EgonColaMapper<HookedBusinessModel> {
    }

    @TableName("test_business_record")
    public static class HookedBusinessModel extends EgonModel<HookedBusinessModel> {

        @jakarta.validation.constraints.NotBlank
        private String title;
        private String payload;
        @Version
        private Long version;
        @TableField(exist = false)
        private final List<String> events = new ArrayList<>();

        public HookedBusinessModel values(String title, String payload) {
            this.title = title;
            this.payload = payload;
            return this;
        }

        @Override
        protected void beforeInsert() {
            events.add("beforeInsert");
        }

        @Override
        protected void afterInsert(boolean result) {
            events.add("afterInsert:" + result);
        }

        @Override
        protected void beforeUpdate() {
            events.add("beforeUpdate");
        }

        @Override
        protected void afterUpdate(boolean result) {
            events.add("afterUpdate:" + result);
        }

        @Override
        protected void beforeDelete() {
            events.add("beforeDelete");
        }

        @Override
        protected void afterDelete(boolean result) {
            events.add("afterDelete:" + result);
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getPayload() {
            return payload;
        }

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }
    }
}
