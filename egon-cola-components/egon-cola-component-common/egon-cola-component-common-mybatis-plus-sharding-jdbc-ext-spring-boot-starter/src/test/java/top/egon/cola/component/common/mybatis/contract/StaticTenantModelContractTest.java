package top.egon.cola.component.common.mybatis.contract;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdTenantLineHandler;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaModelValidationInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Pins the Step 3 static tenant and model-validation contract.
 *
 * <p>Every future static entry point is reached through reflection so this file compiles against the
 * pre-Step tree and fails because the contract is missing rather than because the fixture broke.</p>
 */
class StaticTenantModelContractTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final ValidationUtils CANONICAL = new ValidationUtils(VALIDATOR_FACTORY.getValidator());
    private static final String DEFAULT_MDC_KEY = "tenantId";
    private static final String CUSTOM_MDC_KEY = "requestTenantId";
    private static final Class<?>[] NO_PARAMETERS = new Class<?>[0];

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @BeforeEach
    void bindCanonicalModelValidator() {
        if (currentBinding() == null) {
            bind(CANONICAL, StaticTenantModelContractTest.class);
        }
    }

    @AfterEach
    void restoreTenantKey() {
        MDC.clear();
        initializeTenantKey(DEFAULT_MDC_KEY);
    }

    @Test
    void tenantAccessIsAStaticFacadeAndTheMdcProviderClassIsGone() {
        assertThat(Modifier.isFinal(EgonColaTenantIdProvider.class.getModifiers()))
                .as("EgonColaTenantIdProvider must be a final static facade")
                .isTrue();
        assertThat(EgonColaTenantIdProvider.class.getInterfaces()).isEmpty();
        assertThat(EgonColaTenantIdProvider.class.getDeclaredConstructors())
                .isNotEmpty()
                .allSatisfy(constructor ->
                        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue());
        assertThat(staticMethod(EgonColaTenantIdProvider.class, "currentTenantId", NO_PARAMETERS)
                .getReturnType()).isEqualTo(Long.class);
        assertThat(staticMethod(EgonColaTenantIdProvider.class, "initialize", new Class<?>[] {String.class})
                .getReturnType()).isEqualTo(void.class);
        assertThat(staticStringField(EgonColaTenantIdProvider.class, "DEFAULT_MDC_KEY"))
                .isEqualTo(DEFAULT_MDC_KEY);
        assertThatThrownBy(() -> Class.forName(
                "top.egon.cola.component.common.mybatis.business.EgonColaMdcTenantIdProvider"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void currentTenantIdReadsTheConfiguredKeyOnEveryCall() {
        initializeTenantKey(CUSTOM_MDC_KEY);
        MDC.put(DEFAULT_MDC_KEY, "101");
        MDC.put(CUSTOM_MDC_KEY, "202");
        assertThat(currentTenantId()).isEqualTo(202L);
        MDC.put(CUSTOM_MDC_KEY, "303");
        assertThat(currentTenantId()).isEqualTo(303L);
        MDC.put(CUSTOM_MDC_KEY, " 41 ");
        assertThat(currentTenantId()).isEqualTo(41L);
        MDC.remove(CUSTOM_MDC_KEY);
        assertThatThrownBy(StaticTenantModelContractTest::currentTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISSING");
    }

    @Test
    void missingAndMalformedContextsKeepStableStatusesAndAnyLongIsAccepted() {
        MDC.remove(DEFAULT_MDC_KEY);
        assertThatThrownBy(StaticTenantModelContractTest::currentTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISSING");
        MDC.put(DEFAULT_MDC_KEY, " ");
        assertThatThrownBy(StaticTenantModelContractTest::currentTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISSING");
        MDC.put(DEFAULT_MDC_KEY, "not-a-long");
        assertThatThrownBy(StaticTenantModelContractTest::currentTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MALFORMED")
                .hasRootCauseInstanceOf(NumberFormatException.class);
        MDC.put(DEFAULT_MDC_KEY, "0");
        assertThat(currentTenantId()).isZero();
        MDC.put(DEFAULT_MDC_KEY, "-7");
        assertThat(currentTenantId()).isEqualTo(-7L);
    }

    @Test
    void tenantNeverLeaksAcrossThreadsOrAReusedPooledThread() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Materialise the worker thread before any tenant context exists.
            executor.submit(() -> {
            }).get(5, TimeUnit.SECONDS);
            AtomicReference<Object> seen = new AtomicReference<>();
            MDC.put(DEFAULT_MDC_KEY, "555");
            executor.submit(() -> seen.set(readTenantQuietly())).get(5, TimeUnit.SECONDS);
            assertThat(seen.get()).isInstanceOf(IllegalStateException.class)
                    .hasFieldOrPropertyWithValue("message", "TENANT_CONTEXT_MISSING");
            executor.submit(() -> {
                MDC.put(DEFAULT_MDC_KEY, "777");
                seen.set(readTenantQuietly());
            }).get(5, TimeUnit.SECONDS);
            assertThat(seen.get()).isEqualTo(777L);
            assertThat(currentTenantId()).isEqualTo(555L);
            executor.submit(() -> {
                MDC.remove(DEFAULT_MDC_KEY);
                seen.set(readTenantQuietly());
            }).get(5, TimeUnit.SECONDS);
            assertThat(seen.get()).isInstanceOf(IllegalStateException.class)
                    .hasFieldOrPropertyWithValue("message", "TENANT_CONTEXT_MISSING");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void repositoryKeepsOnlyThePropertiesAccessorAndCollaboratorFieldsAreGone() {
        assertThat(Arrays.stream(EgonColaRepository.class.getDeclaredMethods())
                .map(Method::getName))
                .doesNotContain("getModelValidationUtils", "getTenantIdProvider")
                .contains("getProperties");
        for (Class<?> type : List.of(EgonColaMetaObjectHandler.class, EgonColaTenantIdTenantLineHandler.class,
                EgonColaTenantIdGuardInnerInterceptor.class, EgonColaModelValidationInterceptor.class)) {
            assertThat(Arrays.stream(type.getDeclaredFields()).map(Field::getType).toList())
                    .as("%s must not hold tenant or model-validation collaborators", type.getSimpleName())
                    .doesNotContain(EgonColaTenantIdProvider.class, EgonColaModelValidationUtils.class);
        }
        assertThat(Arrays.stream(EgonColaMybatisPlusProperties.class.getMethods())
                .map(Method::getName))
                .doesNotContain("getMetaFill", "setMetaFill");
    }

    @Test
    void metaObjectHandlerIsAlwaysWiredAndNoTenantOrModelValidationBeansExist() {
        runner().withPropertyValues("egon.cola.component.mybatis-plus.meta-fill.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .hasSingleBean(EgonColaMetaObjectHandler.class)
                            .hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context).doesNotHaveBean(EgonColaTenantIdProvider.class)
                            .doesNotHaveBean(EgonColaModelValidationUtils.class);
                });
    }

    @Test
    void twoLiveContextsShareOneStaticValidationBinding() {
        runner().run(first -> runner()
                .withPropertyValues("egon.cola.component.mybatis-plus.pagination.max-page-size=200")
                .run(second -> {
                    assertThat(first).hasNotFailed();
                    assertThat(second).hasNotFailed()
                            .hasSingleBean(EgonColaMetaObjectHandler.class);
                    assertThat(currentBinding()).isNotNull();
                }));
        assertThat(validationUtilsOf(currentBinding())).isSameAs(CANONICAL);
    }

    @Test
    void bindingADifferentValidationUtilsWhileAnOwnerIsLiveIsRejectedAndReported() {
        assertThat(validationUtilsOf(currentBinding())).isSameAs(CANONICAL);
        assertThatCode(() -> bind(CANONICAL, this))
                .as("repeated identical binding must be allowed")
                .doesNotThrowAnyException();
        Throwable rejected = catchThrowable(() -> bind(canonicalReplacement(), new Object()));
        assertThat(rejected).isInstanceOf(IllegalStateException.class)
                .hasMessage("MODEL_VALIDATION_BINDING_CONFLICT");
        assertThat(rejected.getCause())
                .hasMessageContaining("already bound")
                .hasMessageContaining("StaticTenantModelContractTest");
        assertThat(validationUtilsOf(currentBinding())).isSameAs(CANONICAL);
    }

    @Test
    void releasingTheOwningBindingAllowsTheNextContextToBind() throws Exception {
        Object binding = bind(CANONICAL, StaticTenantModelContractTest.class);
        ((AutoCloseable) binding).close();
        assertThat(currentBinding()).isNull();
        ValidationUtils replacement = canonicalReplacement();
        assertThat(validationUtilsOf(bind(replacement, this))).isSameAs(replacement);
        ((AutoCloseable) currentBinding()).close();
        bind(CANONICAL, StaticTenantModelContractTest.class);
        assertThat(validationUtilsOf(currentBinding())).isSameAs(CANONICAL);
    }

    @Test
    void staticValidateBusinessRejectsTechnicalShadowBeforeBusinessGroups() {
        assertThatThrownBy(() -> validateBusiness(new ShadowedTenantModel().businessValues("title", null),
                EgonColaModelValidationGroups.Operation.INSERT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("MODEL_TECHNICAL_FIELD_SHADOWED");
        assertThatThrownBy(() -> validateBusiness(new TestBusinessModel().businessValues(null, null),
                EgonColaModelValidationGroups.Operation.INSERT))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatCode(() -> validateBusiness(new TestBusinessModel().businessValues("title", "payload"),
                EgonColaModelValidationGroups.Operation.INSERT))
                .doesNotThrowAnyException();
    }

    @Test
    void staticValidateAppliesPersistedGroupsAndTheCurrentTenant() {
        MDC.put(DEFAULT_MDC_KEY, "7");
        assertThatThrownBy(() -> validate(new TestBusinessModel().businessValues("title", null),
                EgonColaModelValidationGroups.Operation.LOADED))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> validate(persisted(9L), EgonColaModelValidationGroups.Operation.UPDATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISMATCH");
        assertThat(validate(persisted(7L), EgonColaModelValidationGroups.Operation.UPDATE)).isNotNull();
    }

    private static TestBusinessModel persisted(long tenantId) {
        Instant now = Instant.parse("2026-09-20T06:00:00Z");
        TestBusinessModel model = new TestBusinessModel().businessValues("title", null);
        model.setId(1L);
        model.setTenantId(tenantId);
        model.setVersion(0L);
        model.setCreateUserId("creator");
        model.setCreateTime(now);
        model.setUpdateUserId("creator");
        model.setUpdateTime(now);
        return model;
    }

    private static ValidationUtils canonicalReplacement() {
        return new ValidationUtils(VALIDATOR_FACTORY.getValidator());
    }

    private static Long currentTenantId() {
        return (Long) invokeStatic(EgonColaTenantIdProvider.class, "currentTenantId", NO_PARAMETERS);
    }

    private static void initializeTenantKey(String mdcKey) {
        invokeStatic(EgonColaTenantIdProvider.class, "initialize", new Class<?>[] {String.class}, mdcKey);
    }

    private static Object readTenantQuietly() {
        try {
            return currentTenantId();
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private static Object bind(ValidationUtils validationUtils, Object owner) {
        return invokeStatic(EgonColaModelValidationUtils.class, "initialize",
                new Class<?>[] {ValidationUtils.class, Object.class}, validationUtils, owner);
    }

    private static Object currentBinding() {
        return invokeStatic(EgonColaModelValidationUtils.class, "current", NO_PARAMETERS);
    }

    private static Object validationUtilsOf(Object binding) {
        try {
            return binding.getClass().getMethod("validationUtils").invoke(binding);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Binding must expose validationUtils()", exception);
        }
    }

    private static void validateBusiness(EgonModel<?> model, EgonColaModelValidationGroups.Operation operation) {
        invokeStatic(EgonColaModelValidationUtils.class, "validateBusiness",
                new Class<?>[] {EgonModel.class, EgonColaModelValidationGroups.Operation.class}, model, operation);
    }

    private static Object validate(EgonModel<?> model, EgonColaModelValidationGroups.Operation operation) {
        return invokeStatic(EgonColaModelValidationUtils.class, "validate",
                new Class<?>[] {EgonModel.class, EgonColaModelValidationGroups.Operation.class}, model, operation);
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
                .withBean("egonColaRoutingProfiles", Map.class, Map::of)
                .withBean("egonColaWriteTargetResolver", EgonColaWriteTargetResolver.class,
                        () -> query -> {
                            throw new IllegalStateException("SHARDING_REQUIRED");
                        })
                .withUserConfiguration(OuterChainConfiguration.class)
                .withConfiguration(AutoConfigurations.of(EgonColaMybatisPlusAutoConfiguration.class))
                .withPropertyValues("egon.cola.component.mybatis-plus.enabled=true");
    }

    private static Method staticMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Method method;
        try {
            method = type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(type.getSimpleName() + "." + name + Arrays.toString(parameterTypes)
                    + " must be declared", exception);
        }
        assertThat(Modifier.isStatic(method.getModifiers()))
                .as("%s.%s must be a static entry point", type.getSimpleName(), name)
                .isTrue();
        return method;
    }

    private static String staticStringField(Class<?> type, String name) {
        try {
            Field field = type.getField(name);
            assertThat(Modifier.isStatic(field.getModifiers()))
                    .as("%s.%s must be static", type.getSimpleName(), name)
                    .isTrue();
            return (String) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(type.getSimpleName() + "." + name + " must be declared", exception);
        }
    }

    private static Object invokeStatic(Class<?> type, String name, Class<?>[] parameterTypes, Object... args) {
        Method method = staticMethod(type, name, parameterTypes);
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OuterChainConfiguration {

        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<InnerInterceptor> interceptors) {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.setInterceptors(interceptors.orderedStream().toList());
            return interceptor;
        }
    }

    /** Fixture that shadows a framework-owned column and must be rejected. */
    static final class ShadowedTenantModel extends TestBusinessModel {

        private Long tenantId;

        private Long shadowedTenantId() {
            return tenantId;
        }
    }
}
