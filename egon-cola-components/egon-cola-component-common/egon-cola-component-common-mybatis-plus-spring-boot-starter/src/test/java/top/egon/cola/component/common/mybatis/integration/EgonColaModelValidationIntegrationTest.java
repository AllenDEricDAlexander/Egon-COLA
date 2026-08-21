package top.egon.cola.component.common.mybatis.integration;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.support.TestBusinessDTO;
import top.egon.cola.component.common.mybatis.support.TestBusinessPO;
import top.egon.cola.component.common.mybatis.support.TestBusinessConverters;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessService;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;
import top.egon.cola.component.common.mybatis.support.TestUserIdProvider;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DTO -> PO -> Model ownership and global repository validation proof.
 */
class EgonColaModelValidationIntegrationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void dtoValidationStopsBeforeControllerBodyAndConverter() throws Exception {
        TestController controller = new TestController();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setConstraintValidatorFactory(
                VALIDATOR_FACTORY.getConstraintValidatorFactory());
        validator.afterPropertiesSet();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();

        mockMvc.perform(post("/test-business")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"payload\":\"p\"}"))
                .andExpect(status().isBadRequest());
        assertThat(controller.calls).isZero();
        validator.destroy();
    }

    @Test
    void convertersReuseBaseConverterAndNeverCopyTechnicalFields() {
        TestBusinessConverters converters = new TestBusinessConverters();
        TestBusinessDTO dto = new TestBusinessDTO("title", "payload");
        TestBusinessPO po = converters.dtoToPo().toTarget(dto);
        TestBusinessModel model = converters.poToModel().toTarget(po);
        model.setId(99L);
        model.setTenantId(7L);
        model.setCreateUserId("creator");
        model.setUpdateUserId("updater");
        model.setIsDeleted(true);

        assertThat(po.getTitle()).isEqualTo("title");
        assertThat(model.getTitle()).isEqualTo("title");
        TestBusinessPO reverse = converters.poToModel().toSource(model);
        TestBusinessDTO response = converters.dtoToPo().toSource(po);
        assertThat(reverse.getTitle()).isEqualTo("title");
        assertThat(response.getTitle()).isEqualTo("title");
        assertThat(converters.dtoToPo().toTargetList(java.util.List.of(dto))).hasSize(1);
        assertThat(converters.dtoToPo().toTargetList(null)).isEmpty();
    }

    @Test
    void poValidationAndComplexRuleStayInBusinessService() {
        TestBusinessConverters converters = new TestBusinessConverters();
        Validator validator = VALIDATOR_FACTORY.getValidator();
        ValidationUtils validationUtils = new ValidationUtils(validator);
        TestBusinessPO invalid = new TestBusinessPO("", null, "normal");
        assertThatThrownBy(() -> validationUtils.validate(invalid))
                .isInstanceOf(ConstraintViolationException.class);

        TestBusinessPO duplicate = new TestBusinessPO("duplicate", null, "normal");
        assertThatThrownBy(() -> new TestBusinessRuleService().create(duplicate, converters))
                .hasMessageContaining("BUSINESS_RULE_REJECTED");
        assertThat(new TestBusinessRuleService().create(
                new TestBusinessPO("valid", null, "normal"), converters).getTitle())
                .isEqualTo("valid");
    }

    @Test
    void repositoryPluginRejectsDirtyLoadedModel() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        EgonColaMybatisPlusAutoConfiguration.class,
                        MybatisPlusInnerInterceptorAutoConfiguration.class,
                        MybatisPlusAutoConfiguration.class))
                .withUserConfiguration(EgonColaTenantIdSqlIntegrationTest.H2Configuration.class)
                .withBean(TestTenantIdProvider.class, TestTenantIdProvider::new)
                .withBean(TestUserIdProvider.class, TestUserIdProvider::new)
                .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
                .withBean(Clock.class, () -> Clock.fixed(
                        Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC))
                .run(context -> {
                    TestTenantIdProvider tenant = context.getBean(TestTenantIdProvider.class);
                    TestUserIdProvider user = context.getBean(TestUserIdProvider.class);
                    TestBusinessService service = context.getBean(TestBusinessService.class);
                    tenant.set(61L);
                    user.set("validator-user");
                    assertThatThrownBy(() -> service.save(
                            new TestBusinessModel().businessValues("", null)))
                            .isInstanceOf(ConstraintViolationException.class);
                });
    }

    @RestController
    static class TestController {
        private int calls;

        @PostMapping("/test-business")
        TestBusinessDTO create(@Valid @RequestBody TestBusinessDTO dto) {
            calls++;
            return dto;
        }
    }

    static final class TestBusinessRuleService {
        TestBusinessModel create(TestBusinessPO po, TestBusinessConverters converters) {
            if ("duplicate".equals(po.getTitle())) {
                throw new IllegalStateException("BUSINESS_RULE_REJECTED");
            }
            return converters.poToModel().toTarget(po);
        }
    }
}
