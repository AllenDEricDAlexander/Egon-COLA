package top.egon.cola.organization.facade;

import com.google.protobuf.Message;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeOrganizationRpcMappingTest {

    private static final List<String[]> MODELS = List.of(
            new String[]{"top.egon.cola.organization.facade.user.dto.CreateUserDTO", "CreateUserRpcRequest"},
            new String[]{"top.egon.cola.organization.facade.user.dto.AssignRoleDTO", "AssignRoleRpcRequest"},
            new String[]{"top.egon.cola.organization.facade.user.dto.GrantPermissionDTO", "GrantPermissionRpcRequest"},
            new String[]{"top.egon.cola.organization.facade.teaching.dto.CreateGradeDTO", "CreateGradeRpcRequest"},
            new String[]{"top.egon.cola.organization.facade.teaching.dto.CreateSchoolClassDTO", "CreateSchoolClassRpcRequest"},
            new String[]{"top.egon.cola.organization.facade.teaching.dto.AssignUserToClassDTO", "AssignUserRpcRequest"},
            new String[]{"top.egon.cola.organization.facade.user.dto.UserDetailDTO", "UserResponse"},
            new String[]{"top.egon.cola.organization.facade.user.dto.PermissionTreeDTO", "PermissionTreeResponse"},
            new String[]{"top.egon.cola.organization.facade.teaching.dto.GradeDetailDTO", "GradeResponse"},
            new String[]{"top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO", "SchoolClassResponse"}
    );

    @Test
    void generatesTheSharedBidirectionalConverter() {
        Object mapper = converter();
        assertTrue(mapper instanceof BaseConverter<?, ?>);
    }

    @TestFactory
    Stream<DynamicTest> preservesEveryRequestAndPayloadField() {
        return MODELS.stream().flatMap(pair -> Stream.of("full", "nullable", "zero")
                .map(variant -> DynamicTest.dynamicTest(pair[0] + " / " + variant, () -> {
                    Class<?> sourceType = Class.forName(pair[0]);
                    Class<?> protoType = Class.forName("top.egon.cola.organization.facade.rpc.proto." + pair[1]);
                    Object source = fixture(sourceType, variant, 0);
                    Object mapper = converter();
                    Message encoded = (Message) mapping(mapper, sourceType, protoType).invoke(mapper, source);
                    Message decoded = (Message) encoded.getParserForType().parseFrom(encoded.toByteArray());
                    assertFields(source, decoded);
                    Object restored = mapping(mapper, protoType, sourceType).invoke(mapper, decoded);
                    assertEquals(source, restored, "DTO semantics changed through the actual Protobuf bytes");
                })));
    }

    @Test
    void preservesNullObjects() throws Exception {
        Object mapper = converter();
        for (String[] pair : MODELS) {
            Class<?> sourceType = Class.forName(pair[0]);
            Class<?> protoType = Class.forName("top.egon.cola.organization.facade.rpc.proto." + pair[1]);
            assertNull(mapping(mapper, sourceType, protoType).invoke(mapper, new Object[]{null}));
            assertNull(mapping(mapper, protoType, sourceType).invoke(mapper, new Object[]{null}));
        }
    }

    private static Object converter() {
        return assertDoesNotThrow(() -> Class.forName("top.egon.cola.organization.facade.rpc.OrganizationRpcConverterImpl")
                .getDeclaredConstructor().newInstance(), "Native MapStruct converter has not been generated");
    }

    private static Method mapping(Object mapper, Class<?> source, Class<?> target) {
        return Arrays.stream(mapper.getClass().getMethods())
                .filter(method -> method.getParameterCount() == 1
                        && method.getParameterTypes()[0].equals(source)
                        && method.getReturnType().equals(target))
                .findFirst().orElseThrow(() -> new AssertionError("Missing mapping: " + source + " -> " + target));
    }

    private static Object fixture(Class<?> type, String variant, int depth) throws Exception {
        RecordComponent[] components = type.getRecordComponents();
        Object[] values = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        for (int index = 0; index < components.length; index++) {
            RecordComponent component = components[index];
            Class<?> fieldType = component.getType();
            types[index] = fieldType;
            if (fieldType == String.class) {
                values[index] = variant.equals("nullable") ? null
                        : variant.equals("zero") ? "" : "值-" + component.getName() + "-" + depth;
            } else if (fieldType == Long.class) {
                values[index] = variant.equals("nullable") ? null
                        : variant.equals("zero") ? 0L : 9_007_199_254_740_993L + index;
            } else if (fieldType == int.class) {
                values[index] = variant.equals("zero") ? 0 : 17 + index;
            } else if (fieldType == long.class) {
                values[index] = variant.equals("zero") ? 0L : 9_007_199_254_740_993L + index;
            } else if (fieldType == Instant.class) {
                values[index] = variant.equals("nullable") ? null : Instant.parse("2026-09-08T00:12:34.123456789Z");
            } else if (fieldType == LocalDateTime.class) {
                values[index] = variant.equals("nullable") ? null : LocalDateTime.parse("2026-09-08T08:12:34.123456789");
            } else if (fieldType == List.class) {
                Class<?> element = (Class<?>) ((ParameterizedType) component.getGenericType()).getActualTypeArguments()[0];
                if (!variant.equals("full") || depth > 0) {
                    values[index] = List.of();
                } else if (element == String.class) {
                    values[index] = List.of("第二项", "first");
                } else if (element == Long.class) {
                    values[index] = List.of(9_007_199_254_740_995L, 9_007_199_254_740_993L);
                } else {
                    values[index] = List.of(fixture(element, variant, depth + 1));
                }
            } else {
                throw new AssertionError("Add an explicit fixture for " + component);
            }
        }
        return type.getDeclaredConstructor(types).newInstance(values);
    }

    private static void assertFields(Object source, Message message) throws Exception {
        RecordComponent[] components = source.getClass().getRecordComponents();
        for (int index = 0; index < components.length; index++) {
            RecordComponent component = components[index];
            String wireName = component.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
            var field = message.getDescriptorForType().findFieldByName(wireName);
            assertNotNull(field, "Missing wire field for " + component.getName());
            assertEquals(index + 1, field.getNumber(), "Field number changed for " + wireName);
            Object expected = component.getAccessor().invoke(source);
            if (expected == null) {
                assertTrue(field.hasPresence(), wireName + " cannot represent a nullable DTO field");
                assertFalse(message.hasField(field), wireName + " lost null/absence semantics");
            } else if (expected instanceof Instant instant) {
                assertEquals(instant.toString(), message.getField(field));
            } else if (expected instanceof LocalDateTime local) {
                assertEquals(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(local), message.getField(field));
            } else if (expected instanceof List<?> list) {
                List<?> actual = (List<?>) message.getField(field);
                assertEquals(list.size(), actual.size(), wireName + " changed collection size");
                for (int item = 0; item < list.size(); item++) {
                    if (list.get(item).getClass().isRecord()) {
                        assertFields(list.get(item), (Message) actual.get(item));
                    } else {
                        assertEquals(list.get(item), actual.get(item), wireName + " changed order/value");
                    }
                }
            } else {
                assertEquals(expected, message.getField(field), wireName + " was renamed, narrowed or defaulted");
            }
        }
    }

    @Test
    void preservesOrganizationBusinessFailureAndVoidAcknowledgement() {
        var mapper = (top.egon.cola.organization.facade.rpc.OrganizationRpcConverter) converter();
        var failure = mapper.userFailure("USER_NOT_FOUND", "missing user", "trace-organization");
        assertFalse(failure.getSuccess());
        assertEquals("USER_NOT_FOUND", failure.getCode());
        assertEquals("trace-organization", failure.getTraceId());
        assertFalse(failure.hasData());
        var empty = mapper.userFailure(null, null, null);
        assertFalse(empty.hasCode());
        assertFalse(empty.hasMessage());
        assertFalse(empty.hasTraceId());
        var acknowledgement = mapper.response(true, "SUCCESS", "success", null);
        assertTrue(acknowledgement.getSuccess());
        assertEquals("SUCCESS", acknowledgement.getCode());
    }

}
