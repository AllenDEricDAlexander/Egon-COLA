package top.egon.cola.component.common.core.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.BaseRequest;
import top.egon.cola.component.common.core.pojo.OperatorContext;
import top.egon.cola.component.common.core.pojo.PageMetaRecord;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.PageSlice;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.common.core.pojo.SortQuery;
import top.egon.cola.component.common.core.pojo.TreeNode;
import top.egon.cola.component.common.core.pojo.TreeOptions;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Freezes the shared carrier, one-way conversion and validation contracts that every governed
 * archetype and component builds on.
 *
 * <p>The not-yet-existing contract types are resolved reflectively on purpose: the first run must
 * fail because the contract is missing, not because this fixture stopped compiling.</p>
 */
class CommonContractFoundationTest {

    private static final String POJO_PACKAGE = "top.egon.cola.component.common.core.pojo";

    private static final String CONVERTER_PACKAGE = "top.egon.cola.component.common.core.converter";

    private static final String VALIDATION_PACKAGE = "top.egon.cola.component.common.core.validation";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void basePojoIsAFieldFreeSerializableContract() {
        Class<?> basePojo = load(POJO_PACKAGE, "BasePojo");

        assertTrue(basePojo.isInterface(), "BasePojo must be an interface so records can implement it");
        assertTrue(Serializable.class.isAssignableFrom(basePojo), "BasePojo must extend Serializable");
        assertEquals(0, basePojo.getDeclaredFields().length, "BasePojo must not declare business fields");
        assertEquals(0, basePojo.getDeclaredMethods().length, "BasePojo must not declare behaviour");
    }

    @Test
    void everyCommonCarrierImplementsBasePojo() {
        Class<?> basePojo = load(POJO_PACKAGE, "BasePojo");

        for (Class<?> carrier : List.of(BaseRequest.class, OperatorContext.class, PageMetaRecord.class,
                PageQuery.class, PageResultRecord.class, PageSlice.class, ResultRecord.class, SortQuery.class,
                TreeNode.class, TreeOptions.class)) {
            assertTrue(basePojo.isAssignableFrom(carrier), carrier.getSimpleName() + " must implement BasePojo");
            assertTrue(Serializable.class.isAssignableFrom(carrier),
                    carrier.getSimpleName() + " must stay Serializable");
        }
    }

    @Test
    void implementingBasePojoAddsNoWireFieldToPublicRecords() {
        assertJsonFields(new BaseRequest(new OperatorContext("u-1", "operator", "t-1")), "operator");
        assertJsonFields(new OperatorContext("u-1", "operator", "t-1"), "userId", "userName", "tenantId");
        assertJsonFields(PageMetaRecord.of(21, 2, 10), "total", "pageNo", "pageSize", "pages", "hasNext",
                "hasPrevious");
        assertJsonFields(new PageQuery(3, 20), "pageNo", "pageSize");
        assertJsonFields(PageResultRecord.success(List.of("a", "b"), 21, 2, 10), "success", "code", "status",
                "message", "records", "page", "traceId", "timestamp");
        assertJsonFields(PageSlice.of(List.of("a"), true), "records", "hasNext");
        assertJsonFields(ResultRecord.success("payload"), "success", "code", "status", "message", "data", "traceId",
                "timestamp");
        assertJsonFields(new SortQuery("name", "desc"), "sortBy", "sortDirection");
    }

    @Test
    void baseForwardConverterExposesOnlyTheForwardProjection() {
        Class<?> forward = load(CONVERTER_PACKAGE, "BaseForwardConverter");

        assertTrue(forward.isInterface(), "BaseForwardConverter must be an interface");
        assertEquals(Set.of("toTarget", "toTargetList"), declaredNames(forward),
                "the one-way contract must not expose reverse methods");
    }

    @Test
    void baseForwardConverterMapsListsElementWiseAndNeverFakesReverseProjection() throws Throwable {
        Class<?> forward = load(CONVERTER_PACKAGE, "BaseForwardConverter");
        Object projection = projection(forward, false);

        assertEquals(List.of(), invoke(projection, forward, "toTargetList", null));
        assertEquals(List.of(), invoke(projection, forward, "toTargetList", List.of()));
        assertEquals(List.of("mapped:a", "mapped:b"), invoke(projection, forward, "toTargetList", List.of("a", "b")));
        assertEquals("mapped:a", invoke(projection, forward, "toTarget", "a"));
        assertNull(invoke(projection, forward, "toTarget", null));
        assertFalse(hasMethod(forward, "toSource"), "a projection must not advertise a reverse method");
    }

    @Test
    void baseConverterExtendsTheForwardContractAndKeepsItsBidirectionalAbi() throws Throwable {
        Class<?> forward = load(CONVERTER_PACKAGE, "BaseForwardConverter");
        Class<?> bidirectional = load(CONVERTER_PACKAGE, "BaseConverter");

        assertTrue(forward.isAssignableFrom(bidirectional), "BaseConverter must extend BaseForwardConverter");
        assertEquals(Set.of("toTarget", "toSource", "toSourceList"), declaredNames(bidirectional),
                "extending the forward contract must not shrink the bidirectional method set");

        Object converter = projection(bidirectional, true);
        assertEquals(List.of(), invoke(converter, bidirectional, "toSourceList", null));
        assertEquals(List.of("reverse:a", "reverse:b"),
                invoke(converter, bidirectional, "toSourceList", List.of("a", "b")));
    }

    @Test
    void baseConverterNoLongerCarriesLegacyUtilDateCodec() {
        Class<?> bidirectional = load(CONVERTER_PACKAGE, "BaseConverter");

        for (Method method : bidirectional.getDeclaredMethods()) {
            assertNoDateType(method.getReturnType(), method.getName());
            Arrays.stream(method.getParameterTypes())
                    .forEach(type -> assertNoDateType(type, method.getName()));
        }
        assertFalse(hasMethod(bidirectional, "map"), "the Date/String overloads must be gone, not hidden");
    }

    @Test
    void baseConverterStaysFreeOfImplicitTimeCodecsThatCouldShadowMapperConversions() {
        Class<?> bidirectional = load(CONVERTER_PACKAGE, "BaseConverter");

        for (Method method : bidirectional.getDeclaredMethods()) {
            assertFalse(isTimeConversion(method),
                    "protocol time formats belong to the owning mapper: " + method.getName());
        }
    }

    @Test
    void baseValidatorIsAFinalDelegationPointWithoutSideEffects() {
        Class<?> validator = load(VALIDATION_PACKAGE, "BaseValidator");

        assertTrue(Modifier.isAbstract(validator.getModifiers()), "BaseValidator is an extension base class");
        Method validateBean = requireDeclaredMethod(validator, "validateBean", Object.class, Class[].class);
        assertTrue(Modifier.isProtected(validateBean.getModifiers()), "validateBean is for subclasses only");
        assertTrue(Modifier.isFinal(validateBean.getModifiers()), "the shared guard must not be overridden");
        Method accessPoint = requireDeclaredMethod(validator, "getValidationUtils");
        assertTrue(Modifier.isProtected(accessPoint.getModifiers()), "subclasses expose the collaborator to the base");
        assertTrue(Modifier.isAbstract(accessPoint.getModifiers()), "subclasses supply the common collaborator");
        assertEquals(ValidationUtils.class, accessPoint.getReturnType());
        assertEquals(Set.of("getValidationUtils", "validateBean"), declaredNames(validator));

        Arrays.stream(validator.getDeclaredFields())
                .forEach(field -> assertNoSideEffectType(field.getType(), field.getName()));
        Arrays.stream(validator.getDeclaredMethods())
                .forEach(method -> assertNoSideEffectType(method.getReturnType(), method.getName()));
    }

    @Test
    void commonAssertEntryPointKeepsTheConcreteCommonFailure() throws Throwable {
        Method requireNotNull = ValidationUtils.class.getDeclaredMethod("requireNotNull", Object.class, Supplier.class);

        assertTrue(Modifier.isStatic(requireNotNull.getModifiers()),
                "facade assert must reach the common entry without an injected helper");
        assertSame(ResultCode.SUCCESS, invokeAssert(requireNotNull, ResultCode.SUCCESS,
                () -> new BusinessException(ResultCode.INVALID_PARAMS)));

        Supplier<CommonException> failure = () -> new BusinessException(ResultCode.MISSING_REQUIRED_PARAMS);
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> invokeAssert(requireNotNull, null, failure));
        assertEquals(ResultCode.MISSING_REQUIRED_PARAMS.getCode(), thrown.getCode());
        assertEquals(ResultCode.MISSING_REQUIRED_PARAMS.getStatus(), thrown.getStatus());
    }

    @Test
    void groupSelectionIsPassedThroughUntouchedAndDefaultsStayStable() {
        RecordingValidator validator = new RecordingValidator();

        new ValidationUtils(validator).validate(new PageQuery(1, 10));
        assertEquals(List.of(Default.class.getName()), validator.lastGroupNames());

        new ValidationUtils(validator).validate(new PageQuery(1, 10), Create.class, Default.class);
        assertEquals(List.of(Create.class.getName(), Default.class.getName()), validator.lastGroupNames());
    }

    private static void assertJsonFields(Object value, String... expectedFields) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(value));
            Set<String> actual = new LinkedHashSet<>();
            node.fieldNames().forEachRemaining(actual::add);
            assertEquals(Set.of(expectedFields), actual, value.getClass().getSimpleName() + " wire fields changed");
        } catch (Exception exception) {
            throw new AssertionError("cannot serialise " + value.getClass().getSimpleName(), exception);
        }
    }

    private static Class<?> load(String carrierPackage, String simpleName) {
        try {
            return Class.forName(carrierPackage + "." + simpleName);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("missing common contract type " + simpleName, exception);
        }
    }

    private static Set<String> declaredNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods()).map(Method::getName).collect(Collectors.toSet());
    }

    private static boolean hasMethod(Class<?> type, String name) {
        return Arrays.stream(type.getMethods()).anyMatch(method -> method.getName().equals(name));
    }

    private static Method requireDeclaredMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(type.getSimpleName() + " must declare " + name, exception);
        }
    }

    private static Object projection(Class<?> contract, boolean bidirectional) {
        return Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[]{contract}, (proxy, method, args) -> {
            if (Object.class.equals(method.getDeclaringClass())) {
                return switch (method.getName()) {
                    case "toString" -> contract.getSimpleName() + "Projection";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new IllegalStateException(method.toString());
                };
            }
            if ("toTarget".equals(method.getName())) {
                return args[0] == null ? null : "mapped:" + args[0];
            }
            if (bidirectional && "toSource".equals(method.getName())) {
                return "reverse:" + args[0];
            }
            return InvocationHandler.invokeDefault(proxy, method, args);
        });
    }

    private static Object invoke(Object target, Class<?> contract, String name, Object argument) throws Throwable {
        Method method = Arrays.stream(contract.getMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .filter(candidate -> candidate.getParameterCount() == 1)
                .findFirst()
                .orElseThrow(() -> new AssertionError(contract.getSimpleName() + " must declare " + name));
        try {
            return method.invoke(target, argument);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static Object invokeAssert(Method requireNotNull, Object value, Supplier<CommonException> failure)
            throws Throwable {
        try {
            return requireNotNull.invoke(null, value, failure);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static void assertNoDateType(Class<?> type, String member) {
        assertFalse(Date.class.equals(type) || "java.text.SimpleDateFormat".equals(type.getName()),
                "Rule 10 forbids java.util date types on " + member);
    }

    private static boolean isTimeConversion(Method method) {
        if (method.getParameterCount() != 1) {
            return false;
        }
        boolean timeResult = method.getReturnType().getName().startsWith("java.time.");
        boolean timeSource = method.getParameterTypes()[0].getName().startsWith("java.time.");
        return (timeResult && String.class.equals(method.getParameterTypes()[0]))
                || (timeSource && String.class.equals(method.getReturnType()));
    }

    private static void assertNoSideEffectType(Class<?> type, String member) {
        String name = type.getName();
        assertFalse(name.contains(".cache.") || name.contains(".mq.") || name.contains("mybatis")
                        || name.contains(".dao.") || name.contains(".repository."),
                "validation must not claim side effects through " + member);
    }

    interface Create {
    }

    static final class RecordingValidator implements Validator {

        private List<Class<?>> lastGroups = List.of();

        @Override
        public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
            lastGroups = Arrays.asList(groups);
            return Set.of();
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
            return Set.of();
        }

        @Override
        public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
                                                            Class<?>... groups) {
            return Set.of();
        }

        @Override
        public jakarta.validation.metadata.BeanDescriptor getConstraintsForClass(Class<?> clazz) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public jakarta.validation.executable.ExecutableValidator forExecutables() {
            throw new UnsupportedOperationException();
        }

        private List<String> lastGroupNames() {
            return lastGroups.stream().map(group -> group.getName()).toList();
        }
    }
}
