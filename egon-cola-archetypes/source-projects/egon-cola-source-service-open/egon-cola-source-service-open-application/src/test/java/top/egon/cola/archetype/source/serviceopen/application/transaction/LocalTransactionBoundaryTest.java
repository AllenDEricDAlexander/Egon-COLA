package top.egon.cola.archetype.source.serviceopen.application.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class LocalTransactionBoundaryTest {

    @Test
    void shouldUseTransactionsForCommandsButNotQueries() throws Exception {
        assertCommand(
                top.egon.cola.archetype.source.serviceopen.application.course.manage.impl.CourseManageImpl.class,
                "create",
                top.egon.cola.archetype.source.serviceopen.application.course.command.CreateCourseCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.serviceopen.application.course.manage.impl.CourseManageImpl.class,
                "schedule",
                top.egon.cola.archetype.source.serviceopen.application.course.command.ScheduleCourseCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.serviceopen.application.exam.manage.impl.ExamManageImpl.class,
                "create",
                top.egon.cola.archetype.source.serviceopen.application.exam.command.CreateExamCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.serviceopen.application.exam.manage.impl.ExamManageImpl.class,
                "attachPaper",
                top.egon.cola.archetype.source.serviceopen.application.exam.command.AttachExamPaperCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.serviceopen.application.exam.manage.impl.ExamManageImpl.class,
                "publish",
                top.egon.cola.archetype.source.serviceopen.application.exam.command.PublishExamCommand.class);
        assertCommand(
                top.egon.cola.archetype.source.serviceopen.application.exam.manage.impl.ScoreManageImpl.class,
                "record",
                top.egon.cola.archetype.source.serviceopen.application.exam.command.RecordScoreCommand.class);

        assertQuery(
                top.egon.cola.archetype.source.serviceopen.application.course.manage.impl.CourseManageImpl.class,
                "get",
                top.egon.cola.archetype.source.serviceopen.application.course.query.GetCourseQuery.class);
        assertQuery(
                top.egon.cola.archetype.source.serviceopen.application.course.manage.impl.CourseManageImpl.class,
                "page",
                top.egon.cola.archetype.source.serviceopen.application.course.query.PageCourseQuery.class);
        assertQuery(
                top.egon.cola.archetype.source.serviceopen.application.exam.manage.impl.ExamManageImpl.class,
                "get",
                top.egon.cola.archetype.source.serviceopen.application.exam.query.GetExamQuery.class);
        assertQuery(
                top.egon.cola.archetype.source.serviceopen.application.exam.manage.impl.ScoreManageImpl.class,
                "get",
                top.egon.cola.archetype.source.serviceopen.application.exam.query.GetScoreQuery.class);
        assertQuery(
                top.egon.cola.archetype.source.serviceopen.application.exam.manage.impl.ScoreManageImpl.class,
                "page",
                top.egon.cola.archetype.source.serviceopen.application.exam.query.PageScoreQuery.class);
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
