#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.rpc;

import ${package}.adapter.course.converter.CourseFacadeConverter;
import ${package}.adapter.course.facade.impl.CourseFacadeImpl;
import ${package}.adapter.exam.converter.ExamFacadeConverter;
import ${package}.adapter.exam.facade.impl.ExamFacadeImpl;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.adapter.course.validators.CourseFacadeValidator;
import ${package}.adapter.exam.validators.ExamFacadeValidator;
import ${package}.application.course.command.CreateCourseCommand;
import ${package}.application.course.manage.CourseManage;
import ${package}.application.exam.manage.ExamManage;
import ${package}.application.course.result.CourseResult;
import ${package}.application.exam.result.ExamDetailResult;
import top.egon.cola.evaluation.facade.course.CourseFacade;
import top.egon.cola.evaluation.facade.exam.ExamFacade;
import top.egon.cola.evaluation.facade.course.dto.CreateCourseRequest;
import top.egon.cola.evaluation.facade.exam.dto.CreateExamRequest;
import java.net.ServerSocket;
import java.time.Instant;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvaluationDubboTripleIntegrationTest {

    @Test
    void shouldInvokeCourseProviderThroughTripleProxy() throws Exception {
        int port = freePort();
        CourseManage manage = mock(CourseManage.class);
        when(manage.create(any(CreateCourseCommand.class))).thenReturn(
                new CourseResult("course-1", "MATH-101", "Math", 3, "ACTIVE"));
        CourseFacade provider = new CourseFacadeImpl(
                manage, new CourseFacadeConverter(), new CourseFacadeValidator(),
                new GlobalFacadeExceptionHandler());
        ExamManage examManage = mock(ExamManage.class);
        when(examManage.create(any())).thenReturn(new ExamDetailResult(
                "exam-1", "course-1", "Midterm",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "DRAFT"));
        ExamFacade examProvider = new ExamFacadeImpl(
                examManage, new ExamFacadeConverter(), new ExamFacadeValidator(),
                new GlobalFacadeExceptionHandler());

        ServiceConfig<CourseFacade> service = new ServiceConfig<>();
        service.setInterface(CourseFacade.class);
        service.setRef(provider);
        service.setGroup("course");
        service.setVersion("1.0.0");
        ServiceConfig<ExamFacade> examService = new ServiceConfig<>();
        examService.setInterface(ExamFacade.class);
        examService.setRef(examProvider);
        examService.setGroup("exam");
        examService.setVersion("1.0.0");
        ReferenceConfig<CourseFacade> reference = new ReferenceConfig<>();
        reference.setInterface(CourseFacade.class);
        reference.setGroup("course");
        reference.setVersion("1.0.0");
        reference.setUrl("tri://127.0.0.1:" + port);
        ReferenceConfig<ExamFacade> examReference = new ReferenceConfig<>();
        examReference.setInterface(ExamFacade.class);
        examReference.setGroup("exam");
        examReference.setVersion("1.0.0");
        examReference.setUrl("tri://127.0.0.1:" + port);

        DubboBootstrap bootstrap = DubboBootstrap.newInstance()
                .application(new ApplicationConfig("evaluation-triple-test"))
                .registry(new RegistryConfig("N/A"))
                .protocol(new ProtocolConfig("tri", port))
                .service(service)
                .service(examService)
                .reference(reference)
                .reference(examReference);
        try {
            bootstrap.start();
            var response = reference.get().create(new CreateCourseRequest("MATH-101", "Math", 3));
            assertTrue(response.isSuccess());
            assertEquals("course-1", response.getData().id());
            var examResponse = examReference.get().createExam(new CreateExamRequest(
                    "course-1", "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60)));
            assertTrue(examResponse.isSuccess());
            assertEquals("exam-1", examResponse.getData().id());
        } finally {
            bootstrap.destroy();
        }
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
