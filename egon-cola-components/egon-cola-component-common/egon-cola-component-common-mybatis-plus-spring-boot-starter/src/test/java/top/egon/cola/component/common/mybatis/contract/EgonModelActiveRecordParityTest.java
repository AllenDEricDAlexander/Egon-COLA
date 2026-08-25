package top.egon.cola.component.common.mybatis.contract;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.support.TestBusinessModel;
import top.egon.cola.component.common.mybatis.support.TestBusinessService;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EgonModelActiveRecordParityTest {

    private static final String EGON_MODEL = "top.egon.cola.component.common.mybatis.model.EgonModel";

    @Test
    void concreteBusinessModelUsesTheEgonModelActiveRecordBase() throws Exception {
        assertEquals(EGON_MODEL, TestBusinessModel.class.getSuperclass().getName());
        assertFalse(Arrays.stream(TestBusinessModel.class.getDeclaredFields())
                .anyMatch(field -> Set.of("businessId", "userName", "deleted").contains(field.getName())));
        assertTrue(TestBusinessModel.class.getDeclaredField("title").isAnnotationPresent(NotBlank.class));
    }

    @Test
    void concreteBusinessModelPublishesPublicNoArgAllArgAndBusinessBuilderConstruction()
            throws Exception {
        assertTrue(Arrays.stream(TestBusinessModel.class.getDeclaredConstructors())
                .anyMatch(constructor -> constructor.getParameterCount() == 0
                        && Modifier.isPublic(constructor.getModifiers())));
        assertTrue(Arrays.stream(TestBusinessModel.class.getDeclaredConstructors())
                .anyMatch(constructor -> constructor.getParameterCount() == 3
                        && Arrays.equals(constructor.getParameterTypes(),
                        new Class<?>[]{String.class, String.class, Long.class})));

        Object builder = TestBusinessModel.builder();
        assertTrue(builder.getClass().getMethod("title", String.class) != null);
        assertTrue(builder.getClass().getMethod("version", Long.class) != null);
        assertThrows(NoSuchMethodException.class,
                () -> builder.getClass().getMethod("id", Long.class));
        assertThrows(NoSuchMethodException.class,
                () -> builder.getClass().getMethod("tenantId", Long.class));
    }

    @Test
    void allAbstractModelMethodsRemainVisibleAndSixMutationRootsAreFinal() throws Exception {
        Class<?> egonModel = Class.forName(EGON_MODEL);
        Class<?> model = Class.forName("com.baomidou.mybatisplus.extension.activerecord.Model");
        Class<?> abstractModel = model.getSuperclass();
        assertEquals(model, egonModel.getSuperclass());
        Set<MethodKey> upstream = Arrays.stream(abstractModel.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !"pkVal".equals(method.getName()))
                .map(MethodKey::of)
                .collect(Collectors.toSet());
        Set<MethodKey> effective = Arrays.stream(egonModel.getMethods())
                .map(MethodKey::of)
                .collect(Collectors.toSet());

        assertTrue(effective.containsAll(upstream));
        assertEquals(14, upstream.size());
        assertTrue(Arrays.stream(egonModel.getDeclaredMethods())
                .filter(method -> Set.of("insert", "deleteById", "delete", "updateById", "update")
                        .contains(method.getName()))
                .filter(method -> !"pkVal".equals(method.getName()))
                .allMatch(method -> Modifier.isFinal(method.getModifiers())));
    }

    @Test
    void serviceSubclassUsesLombokConstructorAndBaseUsesProtectedCollaboratorSeams()
            throws Exception {
        Class<?> service = Class.forName(
                "top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl");
        assertTrue(Modifier.isAbstract(service.getModifiers()));
        assertTrue(Arrays.stream(service.getDeclaredConstructors())
                .noneMatch(constructor -> constructor.getParameterCount() == 3));
        assertProtectedAbstractGetter(service, "getModelValidationUtils");
        assertProtectedAbstractGetter(service, "getTenantIdProvider");
        assertProtectedAbstractGetter(service, "getProperties");
        assertTrue(Arrays.stream(TestBusinessService.class.getDeclaredConstructors())
                .anyMatch(constructor -> constructor.getParameterCount() == 3));
    }

    private static void assertProtectedAbstractGetter(Class<?> type, String methodName)
            throws NoSuchMethodException {
        Method method = type.getDeclaredMethod(methodName);
        assertTrue(Modifier.isProtected(method.getModifiers()));
        assertTrue(Modifier.isAbstract(method.getModifiers()));
    }

    @Test
    void commonFieldsHaveExactPersistenceAnnotations() throws Exception {
        Class<?> egonModel = Class.forName(EGON_MODEL);
        Set<String> fieldNames = Arrays.stream(egonModel.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("id", "tenantId", "createUserId", "createTime", "updateUserId",
                "updateTime", "isDeleted"), fieldNames);

        TableId tableId = egonModel.getDeclaredField("id").getAnnotation(TableId.class);
        assertEquals("id", tableId.value());
        assertEquals(IdType.ASSIGN_ID, tableId.type());
        assertTrue(egonModel.getDeclaredField("id").isAnnotationPresent(TableId.class));
        assertPersisted(egonModel.getDeclaredField("id"));
        assertTenantField(egonModel.getDeclaredField("tenantId"));
        assertInsertField(egonModel.getDeclaredField("createUserId"), "create_user_id");
        assertInsertField(egonModel.getDeclaredField("createTime"), "create_time");
        assertInsertUpdateField(egonModel.getDeclaredField("updateUserId"), "update_user_id");
        assertInsertUpdateField(egonModel.getDeclaredField("updateTime"), "update_time");
        Field deleted = egonModel.getDeclaredField("isDeleted");
        assertEquals("is_deleted", deleted.getAnnotation(TableField.class).value());
        assertEquals(FieldFill.INSERT, deleted.getAnnotation(TableField.class).fill());
        assertEquals("0", deleted.getAnnotation(TableLogic.class).value());
        assertEquals("1", deleted.getAnnotation(TableLogic.class).delval());
        assertPersisted(deleted);
    }

    private static void assertPersisted(Field field) {
        NotNull notNull = field.getAnnotation(NotNull.class);
        assertTrue(notNull != null && Arrays.asList(notNull.groups())
                .contains(top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups.Persisted.class));
    }

    private static void assertTenantField(Field field) {
        TableField tableField = field.getAnnotation(TableField.class);
        assertEquals("tenant_id", tableField.value());
        assertEquals(FieldFill.INSERT_UPDATE, tableField.fill());
        assertEquals(FieldStrategy.NEVER, tableField.updateStrategy());
        assertPersisted(field);
    }

    private static void assertInsertField(Field field, String column) {
        TableField tableField = field.getAnnotation(TableField.class);
        assertEquals(column, tableField.value());
        assertEquals(FieldFill.INSERT, tableField.fill());
        assertEquals(FieldStrategy.NEVER, tableField.updateStrategy());
        assertPersisted(field);
    }

    private static void assertInsertUpdateField(Field field, String column) {
        TableField tableField = field.getAnnotation(TableField.class);
        assertEquals(column, tableField.value());
        assertEquals(FieldFill.INSERT_UPDATE, tableField.fill());
        assertPersisted(field);
    }

    private record MethodKey(String name, String returnType, ListKey parameters) {

        private static MethodKey of(Method method) {
            return new MethodKey(method.getName(), method.getReturnType().getName(),
                    new ListKey(Arrays.stream(method.getParameterTypes())
                            .map(Class::getName).toList()));
        }
    }

    private record ListKey(java.util.List<String> values) {
    }
}
