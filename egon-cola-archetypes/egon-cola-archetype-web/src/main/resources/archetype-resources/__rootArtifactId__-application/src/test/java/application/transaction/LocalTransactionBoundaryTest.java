package ${package}.application.transaction;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class LocalTransactionBoundaryTest {

    @Test
    void shouldUseTransactionsForCommandsButNotQueries() throws Exception {
        assertCommand(
                ${package}.application.user.manage.impl.UserManageImpl.class,
                "createUser",
                ${package}.application.user.command.CreateUserCommand.class);
        assertCommand(
                ${package}.application.user.manage.impl.RoleManageImpl.class,
                "assignRole",
                ${package}.application.user.command.AssignRoleCommand.class);
        assertCommand(
                ${package}.application.user.manage.impl.PermissionManageImpl.class,
                "grantPermission",
                ${package}.application.user.command.GrantPermissionCommand.class);
        assertCommand(
                ${package}.application.teaching.manage.impl.GradeManageImpl.class,
                "createGrade",
                ${package}.application.teaching.command.CreateGradeCommand.class);
        assertCommand(
                ${package}.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "createSchoolClass",
                ${package}.application.teaching.command.CreateSchoolClassCommand.class);
        assertCommand(
                ${package}.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "assignUser",
                ${package}.application.teaching.command.AssignUserToClassCommand.class);

        assertQuery(
                ${package}.application.user.manage.impl.UserManageImpl.class,
                "getUser",
                ${package}.application.user.query.UserDetailQuery.class);
        assertQuery(
                ${package}.application.user.manage.impl.PermissionManageImpl.class,
                "getPermissionTree",
                ${package}.application.user.query.PermissionTreeQuery.class);
        assertQuery(
                ${package}.application.teaching.manage.impl.GradeManageImpl.class,
                "getGrade",
                ${package}.application.teaching.query.GradeDetailQuery.class);
        assertQuery(
                ${package}.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "getSchoolClass",
                ${package}.application.teaching.query.SchoolClassDetailQuery.class);
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
