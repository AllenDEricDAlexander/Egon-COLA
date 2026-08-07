package top.egon.cola.component.gateway.starter.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewaySchemaAnnotationContractTest {

    @Test
    void requestSchemaFieldHasTheApprovedDefaults() throws Exception {
        assertEquals(
                RetentionPolicy.RUNTIME,
                GatewayRequestSchemaField.class
                        .getAnnotation(Retention.class)
                        .value()
        );
        assertArrayEquals(
                new ElementType[0],
                GatewayRequestSchemaField.class
                        .getAnnotation(Target.class)
                        .value()
        );
        GatewayRequestSchemaField annotation = Defaults.class
                .getAnnotation(Holder.class)
                .request();
        assertNull(GatewayRequestSchemaField.class
                .getDeclaredMethod("location")
                .getDefaultValue());
        assertNull(GatewayRequestSchemaField.class
                .getDeclaredMethod("schema")
                .getDefaultValue());
        assertEquals(GatewayRequestLocation.PATH, annotation.location());
        assertEquals(String.class, annotation.schema());
        assertEquals("", annotation.name());
        assertEquals(GatewaySchemaShape.AUTO, annotation.shape());
        assertFalse(annotation.expanded());
    }

    @Test
    void responseSchemaUsesVoidSentinels() {
        assertEquals(
                RetentionPolicy.RUNTIME,
                GatewayResponseSchema.class
                        .getAnnotation(Retention.class)
                        .value()
        );
        assertArrayEquals(
                new ElementType[0],
                GatewayResponseSchema.class
                        .getAnnotation(Target.class)
                        .value()
        );
        GatewayResponseSchema annotation = Defaults.class
                .getAnnotation(Holder.class)
                .response();
        assertEquals(Void.class, annotation.wrapper());
        assertEquals("", annotation.payloadField());
        assertEquals(Void.class, annotation.schema());
        assertEquals(GatewaySchemaShape.AUTO, annotation.shape());
    }

    @Test
    void schemaEnumsExposeTheApprovedMembers() {
        assertArrayEquals(
                new GatewayRequestLocation[]{
                        GatewayRequestLocation.PATH,
                        GatewayRequestLocation.QUERY,
                        GatewayRequestLocation.HEADER,
                        GatewayRequestLocation.COOKIE,
                        GatewayRequestLocation.BODY,
                        GatewayRequestLocation.PART
                },
                GatewayRequestLocation.values()
        );
        assertArrayEquals(
                new GatewaySchemaShape[]{
                        GatewaySchemaShape.AUTO,
                        GatewaySchemaShape.VALUE,
                        GatewaySchemaShape.OBJECT,
                        GatewaySchemaShape.LIST,
                        GatewaySchemaShape.MAP,
                        GatewaySchemaShape.VOID
                },
                GatewaySchemaShape.values()
        );
        assertArrayEquals(
                new GatewaySchemaType[]{
                        GatewaySchemaType.AUTO,
                        GatewaySchemaType.STRING,
                        GatewaySchemaType.INTEGER,
                        GatewaySchemaType.NUMBER,
                        GatewaySchemaType.BOOLEAN,
                        GatewaySchemaType.OBJECT,
                        GatewaySchemaType.ARRAY,
                        GatewaySchemaType.MAP
                },
                GatewaySchemaType.values()
        );
        assertArrayEquals(
                new GatewaySchemaRequired[]{
                        GatewaySchemaRequired.AUTO,
                        GatewaySchemaRequired.REQUIRED,
                        GatewaySchemaRequired.OPTIONAL
                },
                GatewaySchemaRequired.values()
        );
    }

    @Test
    void newAnnotationsAreRuntimeDocumented() {
        assertTrue(GatewayRequestSchemaField.class.isAnnotationPresent(
                java.lang.annotation.Documented.class
        ));
        assertTrue(GatewayResponseSchema.class.isAnnotationPresent(
                java.lang.annotation.Documented.class
        ));
    }

    @Test
    void operationAndFieldExposeOnlyTheApprovedSchemaContract()
            throws Exception {
        assertTrue(GatewayOperation.class.isAnnotationPresent(Documented.class));
        assertTrue(GatewayInterfaceGroup.class.isAnnotationPresent(
                Documented.class
        ));
        assertTrue(GatewaySchemaField.class.isAnnotationPresent(Documented.class));
        assertArrayEquals(
                new ElementType[]{
                        ElementType.FIELD,
                        ElementType.RECORD_COMPONENT,
                        ElementType.METHOD,
                        ElementType.PARAMETER
                },
                GatewaySchemaField.class.getAnnotation(Target.class).value()
        );
        assertEquals(
                GatewayRequestSchemaField[].class,
                GatewayOperation.class.getDeclaredMethod("requestSchemaFields")
                        .getReturnType()
        );
        assertEquals(
                GatewayResponseSchema.class,
                GatewayOperation.class.getDeclaredMethod("responseSchema")
                        .getReturnType()
        );
        assertThatMethodIsAbsent(
                GatewayOperation.class,
                "responseSchema" + "Fields"
        );
        assertThatMethodIsAbsent(GatewaySchemaField.class, "path");
        assertEquals("", GatewaySchemaField.class
                .getDeclaredMethod("description").getDefaultValue());
        assertEquals(GatewaySchemaType.AUTO, GatewaySchemaField.class
                .getDeclaredMethod("type").getDefaultValue());
        assertEquals("", GatewaySchemaField.class
                .getDeclaredMethod("format").getDefaultValue());
        assertEquals(GatewaySchemaRequired.AUTO, GatewaySchemaField.class
                .getDeclaredMethod("required").getDefaultValue());
        assertEquals("", GatewaySchemaField.class
                .getDeclaredMethod("example").getDefaultValue());
        assertEquals(Void.class, GatewaySchemaField.class
                .getDeclaredMethod("implementation").getDefaultValue());
    }

    private void assertThatMethodIsAbsent(Class<?> type, String name) {
        assertFalse(java.util.Arrays.stream(type.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals(name)));
    }

    @java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
    private @interface Holder {

        GatewayRequestSchemaField request() default @GatewayRequestSchemaField(
                location = GatewayRequestLocation.PATH,
                schema = String.class
        );

        GatewayResponseSchema response() default @GatewayResponseSchema;
    }

    @Holder
    private static final class Defaults {
    }
}
