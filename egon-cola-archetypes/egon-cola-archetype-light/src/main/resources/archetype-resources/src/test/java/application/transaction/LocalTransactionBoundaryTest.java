package ${package}.application.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class LocalTransactionBoundaryTest {

    @Test
    void shouldUseTransactionsForCommandsButNotQueries() throws Exception {
        assertCommand(
                ${package}.application.user.manage.impl.UserManageImpl.class,
                "create",
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
                ${package}.application.teaching.manage.impl.CourseManageImpl.class,
                "create",
                ${package}.application.teaching.command.CreateCourseCommand.class);
        assertCommand(
                ${package}.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "create",
                ${package}.application.teaching.command.CreateSchoolClassCommand.class);
        assertCommand(
                ${package}.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "schedule",
                ${package}.application.teaching.command.ScheduleCourseCommand.class);

        assertQuery(
                ${package}.application.user.manage.impl.UserManageImpl.class,
                "get",
                ${package}.application.user.query.GetUserQuery.class);
        assertQuery(
                ${package}.application.user.manage.impl.PermissionManageImpl.class,
                "getByUser",
                ${package}.application.user.query.GetUserPermissionsQuery.class);
        assertQuery(
                ${package}.application.teaching.manage.impl.CourseManageImpl.class,
                "get",
                ${package}.application.teaching.query.GetCourseQuery.class);
        assertQuery(
                ${package}.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "get",
                ${package}.application.teaching.query.GetSchoolClassQuery.class);
    }

    private static void assertCommand(
            Class<?> type,
            String methodName,
            Class<?> parameterType) throws NoSuchMethodException {
        Method method = type.getMethod(methodName, parameterType);
        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }

    private static void assertQuery(
            Class<?> type,
            String methodName,
            Class<?> parameterType) throws NoSuchMethodException {
        Method method = type.getMethod(methodName, parameterType);
        assertThat(method.getAnnotation(Transactional.class)).isNull();
    }
}
