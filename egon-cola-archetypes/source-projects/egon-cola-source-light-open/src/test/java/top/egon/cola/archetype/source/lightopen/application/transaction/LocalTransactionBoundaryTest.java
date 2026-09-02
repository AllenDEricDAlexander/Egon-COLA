package top.egon.cola.archetype.source.lightopen.application.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class LocalTransactionBoundaryTest {

    @Test
    void shouldUseTransactionsForCommandsButNotQueries() throws Exception {
        assertCommand(
                top.egon.cola.archetype.source.lightopen.application.user.manage.impl.UserManageImpl.class,
                "create",
                top.egon.cola.archetype.source.lightopen.application.user.command.CreateUserCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.lightopen.application.user.manage.impl.RoleManageImpl.class,
                "assignRole",
                top.egon.cola.archetype.source.lightopen.application.user.command.AssignRoleCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.lightopen.application.user.manage.impl.PermissionManageImpl.class,
                "grantPermission",
                top.egon.cola.archetype.source.lightopen.application.user.command.GrantPermissionCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.lightopen.application.teaching.manage.impl.CourseManageImpl.class,
                "create",
                top.egon.cola.archetype.source.lightopen.application.teaching.command.CreateCourseCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.lightopen.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "create",
                top.egon.cola.archetype.source.lightopen.application.teaching.command.CreateSchoolClassCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.lightopen.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "schedule",
                top.egon.cola.archetype.source.lightopen.application.teaching.command.ScheduleCourseCommand.class);

        assertQuery(
                top.egon.cola.archetype.source.lightopen.application.user.manage.impl.UserManageImpl.class,
                "get",
                top.egon.cola.archetype.source.lightopen.application.user.query.GetUserQuery.class);
        assertQuery(
                top.egon.cola.archetype.source.lightopen.application.user.manage.impl.PermissionManageImpl.class,
                "getByUser",
                top.egon.cola.archetype.source.lightopen.application.user.query.GetUserPermissionsQuery.class);
        assertQuery(
                top.egon.cola.archetype.source.lightopen.application.teaching.manage.impl.CourseManageImpl.class,
                "get",
                top.egon.cola.archetype.source.lightopen.application.teaching.query.GetCourseQuery.class);
        assertQuery(
                top.egon.cola.archetype.source.lightopen.application.teaching.manage.impl.SchoolClassManageImpl.class,
                "get",
                top.egon.cola.archetype.source.lightopen.application.teaching.query.GetSchoolClassQuery.class);
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
