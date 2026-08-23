package ${package}.application.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class LocalTransactionBoundaryTest {

    @Test
    void shouldUseTransactionsForCommandsButNotQueries() throws Exception {
        assertCommand(
                ${package}.application.course.manage.impl.CourseManageImpl.class,
                "create",
                ${package}.application.course.command.CreateCourseCommand.class);
        assertCommand(
                ${package}.application.course.manage.impl.CourseManageImpl.class,
                "schedule",
                ${package}.application.course.command.ScheduleCourseCommand.class);
        assertCommand(
                ${package}.application.exam.manage.impl.ExamManageImpl.class,
                "create",
                ${package}.application.exam.command.CreateExamCommand.class);
        assertCommand(
                ${package}.application.exam.manage.impl.ExamManageImpl.class,
                "attachPaper",
                ${package}.application.exam.command.AttachExamPaperCommand.class);
        assertCommand(
                ${package}.application.exam.manage.impl.ExamManageImpl.class,
                "publish",
                ${package}.application.exam.command.PublishExamCommand.class);
        assertCommand(
                ${package}.application.exam.manage.impl.ScoreManageImpl.class,
                "record",
                ${package}.application.exam.command.RecordScoreCommand.class);

        assertQuery(
                ${package}.application.course.manage.impl.CourseManageImpl.class,
                "get",
                ${package}.application.course.query.GetCourseQuery.class);
        assertQuery(
                ${package}.application.course.manage.impl.CourseManageImpl.class,
                "page",
                ${package}.application.course.query.PageCourseQuery.class);
        assertQuery(
                ${package}.application.exam.manage.impl.ExamManageImpl.class,
                "get",
                ${package}.application.exam.query.GetExamQuery.class);
        assertQuery(
                ${package}.application.exam.manage.impl.ScoreManageImpl.class,
                "get",
                ${package}.application.exam.query.GetScoreQuery.class);
        assertQuery(
                ${package}.application.exam.manage.impl.ScoreManageImpl.class,
                "page",
                ${package}.application.exam.query.PageScoreQuery.class);
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
