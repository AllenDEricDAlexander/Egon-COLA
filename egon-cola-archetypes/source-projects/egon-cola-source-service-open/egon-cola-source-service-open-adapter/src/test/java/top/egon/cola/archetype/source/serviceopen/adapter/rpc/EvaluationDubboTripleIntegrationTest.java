package top.egon.cola.archetype.source.serviceopen.adapter.rpc;

import top.egon.cola.archetype.source.serviceopen.adapter.course.converter.CourseFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.course.facade.impl.CourseFacadeImpl;
import top.egon.cola.archetype.source.serviceopen.adapter.course.validators.CourseFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.converter.ExamFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.converter.ScoreFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.facade.impl.ExamFacadeImpl;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.facade.impl.ScoreFacadeImpl;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.validators.ExamFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.exam.validators.ScoreFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.course.manage.CourseManage;
import top.egon.cola.archetype.source.serviceopen.application.course.result.CourseResult;
import top.egon.cola.archetype.source.serviceopen.application.course.result.CourseScheduleResult;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ExamManage;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ScoreManage;
import top.egon.cola.archetype.source.serviceopen.application.exam.result.ExamDetailResult;
import top.egon.cola.archetype.source.serviceopen.application.exam.result.ExamPaperResult;
import top.egon.cola.archetype.source.serviceopen.application.exam.result.ScoreResult;
import top.egon.cola.archetype.source.serviceopen.application.result.PageResult;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.AttachExamPaperRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Course;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CourseSchedule;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CourseService;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateCourseRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Exam;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ExamService;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetCourseRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageCourseResponse;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageCoursesRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageScoreResponse;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageScoresRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PublishExamRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.RecordScoreRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ScheduleCourseRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Score;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ScoreService;
import com.google.protobuf.Timestamp;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvaluationDubboTripleIntegrationTest {

    @Test
    void shouldInvokeAllEvaluationMethodsThroughOneTriplePort() throws Exception {
        int port = freePort();
        CourseManage courseManage = mock(CourseManage.class);
        when(courseManage.create(any())).thenReturn(new CourseResult(1001L, "MATH-101", "Math", 3, "ACTIVE"));
        when(courseManage.schedule(any())).thenReturn(new CourseScheduleResult(
                1101L, 1001L, 2001L, Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "SCHEDULED"));
        when(courseManage.get(any())).thenReturn(new CourseResult(1001L, "MATH-101", "Math", 3, "ACTIVE"));
        when(courseManage.page(any())).thenReturn(PageResult.of(
                java.util.List.of(new CourseResult(1001L, "MATH-101", "Math", 3, "ACTIVE")),
                1, 1, 20, 1));

        ExamManage examManage = mock(ExamManage.class);
        when(examManage.create(any())).thenReturn(new ExamDetailResult(
                1002L, 1001L, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "DRAFT"));
        when(examManage.attachPaper(any())).thenReturn(new ExamPaperResult(
                1201L, 1002L, "Paper", 100, "ATTACHED"));
        when(examManage.publish(any())).thenReturn(new ExamDetailResult(
                1002L, 1001L, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "PUBLISHED"));
        when(examManage.get(any())).thenReturn(new ExamDetailResult(
                1002L, 1001L, "Midterm", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "DRAFT"));

        ScoreManage scoreManage = mock(ScoreManage.class);
        when(scoreManage.record(any())).thenReturn(new ScoreResult(1301L, 1002L, 1001L, 2001L, 95, "RECORDED"));
        when(scoreManage.get(any())).thenReturn(new ScoreResult(1301L, 1002L, 1001L, 2001L, 95, "RECORDED"));
        when(scoreManage.page(any())).thenReturn(PageResult.of(
                java.util.List.of(new ScoreResult(1301L, 1002L, 1001L, 2001L, 95, "RECORDED")),
                1, 1, 20, 1));

        GlobalFacadeExceptionHandler handler = new GlobalFacadeExceptionHandler(() -> 9001L);
        CourseFacadeImpl courseProvider = new CourseFacadeImpl(
                courseManage, new CourseFacadeConverter(), new CourseFacadeValidator(), handler);
        ExamFacadeImpl examProvider = new ExamFacadeImpl(
                examManage, new ExamFacadeConverter(), new ExamFacadeValidator(), handler);
        ScoreFacadeImpl scoreProvider = new ScoreFacadeImpl(
                scoreManage, new ScoreFacadeConverter(), new ScoreFacadeValidator(), handler);

        ServiceConfig<CourseService> courseService = service(CourseService.class, courseProvider, "course");
        ServiceConfig<ExamService> examService = service(ExamService.class, examProvider, "exam");
        ServiceConfig<ScoreService> scoreService = service(ScoreService.class, scoreProvider, "score");
        ReferenceConfig<CourseService> courseReference = reference(CourseService.class, "course", port);
        ReferenceConfig<ExamService> examReference = reference(ExamService.class, "exam", port);
        ReferenceConfig<ScoreService> scoreReference = reference(ScoreService.class, "score", port);

        DubboBootstrap bootstrap = DubboBootstrap.newInstance()
                .application(new ApplicationConfig("evaluation-triple-test"))
                .registry(new RegistryConfig("N/A"))
                .protocol(new ProtocolConfig("tri", port))
                .service(courseService)
                .service(examService)
                .service(scoreService)
                .reference(courseReference)
                .reference(examReference)
                .reference(scoreReference);
        try {
            bootstrap.start();
            Course course = courseReference.get().createCourse(
                    CreateCourseRequest.newBuilder().setCode("MATH-101").setName("Math").setCredit(3).build());
            assertEquals(1001L, course.getId());
            CourseSchedule schedule = courseReference.get().scheduleCourse(ScheduleCourseRequest.newBuilder()
                    .setCourseId(1001L).setClassId(2001L).setStartsAt(timestamp(Instant.EPOCH))
                    .setEndsAt(timestamp(Instant.EPOCH.plusSeconds(60))).build());
            assertEquals(1101L, schedule.getId());
            assertEquals(1001L, courseReference.get().getCourse(
                    GetCourseRequest.newBuilder().setCourseId(1001L).build()).getId());
            PageCourseResponse coursePage = courseReference.get().pageCourses(
                    PageCoursesRequest.newBuilder().setCurrentPage(1).setPageSize(20).build());
            assertEquals(1, coursePage.getRecordsCount());

            assertEquals(1002L, examReference.get().createExam(CreateExamRequest.newBuilder()
                    .setCourseId(1001L).setTitle("Midterm").setStartsAt(timestamp(Instant.EPOCH))
                    .setEndsAt(timestamp(Instant.EPOCH.plusSeconds(60))).build()).getId());
            assertEquals(1201L, examReference.get().attachPaper(AttachExamPaperRequest.newBuilder()
                    .setExamId(1002L).setTitle("Paper").setTotalPoints(100).build()).getId());
            assertEquals("PUBLISHED", examReference.get().publishExam(
                    PublishExamRequest.newBuilder().setExamId(1002L).build()).getStatus());
            assertEquals(1002L, examReference.get().getExam(
                    GetExamRequest.newBuilder().setExamId(1002L).build()).getId());

            assertEquals(1301L, scoreReference.get().recordScore(RecordScoreRequest.newBuilder()
                    .setExamId(1002L).setStudentId(2001L).setPoints(95).build()).getId());
            assertEquals(1301L, scoreReference.get().getScore(GetScoreRequest.newBuilder()
                    .setExamId(1002L).setScoreId(1301L).build()).getId());
            PageScoreResponse scorePage = scoreReference.get().pageScores(PageScoresRequest.newBuilder()
                    .setExamId(1002L).setCurrentPage(1).setPageSize(20).build());
            assertEquals(1, scorePage.getRecordsCount());

            assertStandardGrpcCreateCourse(port);
        } finally {
            bootstrap.destroy();
        }
    }

    private static void assertStandardGrpcCreateCourse(int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", port)
                .usePlaintext().build();
        try {
            MethodDescriptor<CreateCourseRequest, Course> method = MethodDescriptor.<CreateCourseRequest, Course>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(MethodDescriptor.generateFullMethodName(
                            CourseService.SERVICE_NAME, "CreateCourse"))
                    .setRequestMarshaller(ProtoUtils.marshaller(CreateCourseRequest.getDefaultInstance()))
                    .setResponseMarshaller(ProtoUtils.marshaller(Course.getDefaultInstance()))
                    .build();
            Course response = ClientCalls.blockingUnaryCall(channel, method, CallOptions.DEFAULT,
                    CreateCourseRequest.newBuilder().setCode("MATH-101").setName("Math").setCredit(3).build());
            assertNotNull(response);
            assertEquals(1001L, response.getId());
        } finally {
            channel.shutdownNow();
        }
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    private static <T> ServiceConfig<T> service(Class<T> type, T provider, String group) {
        ServiceConfig<T> service = new ServiceConfig<>();
        service.setInterface(type);
        service.setRef(provider);
        service.setGroup(group);
        service.setVersion("1.0.0");
        return service;
    }

    private static <T> ReferenceConfig<T> reference(Class<T> type, String group, int port) {
        ReferenceConfig<T> reference = new ReferenceConfig<>();
        reference.setInterface(type);
        reference.setGroup(group);
        reference.setVersion("1.0.0");
        reference.setUrl("tri://127.0.0.1:" + port);
        return reference;
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
