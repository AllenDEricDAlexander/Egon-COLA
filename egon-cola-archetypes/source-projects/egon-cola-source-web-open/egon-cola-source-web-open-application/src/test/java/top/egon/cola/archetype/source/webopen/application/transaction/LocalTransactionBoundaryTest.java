package top.egon.cola.archetype.source.webopen.application.transaction;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class LocalTransactionBoundaryTest {

    @Test
    void shouldUseTransactionsForCommandsButNotQueries() throws Exception {
        assertCommand(
                top.egon.cola.archetype.source.webopen.application.user.manage.impl.UserManageImpl.class,
                "createUser",
                top.egon.cola.archetype.source.webopen.application.user.pojo.command.CreateUserCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.webopen.application.user.manage.impl.RoleManageImpl.class,
                "assignRole",
                top.egon.cola.archetype.source.webopen.application.user.pojo.command.AssignRoleCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.webopen.application.user.manage.impl.PermissionManageImpl.class,
                "grantPermission",
                top.egon.cola.archetype.source.webopen.application.user.pojo.command.GrantPermissionCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.webopen.application.teaching.manage.impl.GradeManageImpl.class,
                "createGrade",
                top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.CreateGradeCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.webopen.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "createSchoolClass",
                top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.CreateSchoolClassCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.webopen.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "assignUser",
                top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.AssignUserToClassCommand.class);

        assertQuery(
                top.egon.cola.archetype.source.webopen.application.user.manage.impl.UserManageImpl.class,
                "getUser",
                top.egon.cola.archetype.source.webopen.application.user.pojo.query.UserDetailQuery.class);
        assertQuery(
                top.egon.cola.archetype.source.webopen.application.user.manage.impl.PermissionManageImpl.class,
                "getPermissionTree",
                top.egon.cola.archetype.source.webopen.application.user.pojo.query.PermissionTreeQuery.class);
        assertQuery(
                top.egon.cola.archetype.source.webopen.application.teaching.manage.impl.GradeManageImpl.class,
                "getGrade",
                top.egon.cola.archetype.source.webopen.application.teaching.pojo.query.GradeDetailQuery.class);
        assertQuery(
                top.egon.cola.archetype.source.webopen.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "getSchoolClass",
                top.egon.cola.archetype.source.webopen.application.teaching.pojo.query.SchoolClassDetailQuery.class);
    }

    private static void assertCommand(
            Class<?> type,
            String methodName,
            Class<?> parameterType) throws NoSuchMethodException {
        Method method = type.getMethod(methodName, parameterType);
        assertNotNull(method.getAnnotation(Transactional.class));
    }

    private static void assertQuery(
            Class<?> type,
            String methodName,
            Class<?> parameterType) throws NoSuchMethodException {
        Method method = type.getMethod(methodName, parameterType);
        assertNull(method.getAnnotation(Transactional.class));
    }
}
