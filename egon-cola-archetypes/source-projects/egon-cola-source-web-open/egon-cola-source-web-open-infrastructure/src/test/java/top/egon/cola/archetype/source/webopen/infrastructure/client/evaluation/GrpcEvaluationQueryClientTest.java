package top.egon.cola.archetype.source.webopen.infrastructure.client.evaluation;

import top.egon.cola.archetype.source.webopen.domain.client.ExternalDependencyException;
import top.egon.cola.archetype.source.webopen.facade.evaluation.v1.Course;
import top.egon.cola.archetype.source.webopen.facade.evaluation.v1.Exam;
import top.egon.cola.archetype.source.webopen.facade.evaluation.v1.GetCourseRequest;
import top.egon.cola.archetype.source.webopen.facade.evaluation.v1.GetExamRequest;
import top.egon.cola.archetype.source.webopen.facade.evaluation.v1.GetScoreRequest;
import top.egon.cola.archetype.source.webopen.facade.evaluation.v1.Score;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrpcEvaluationQueryClientTest {

    private Server server;
    private ManagedChannel channel;
    private GrpcEvaluationQueryClient client;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(courseService())
                .addService(examService())
                .addService(scoreService())
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        client = new GrpcEvaluationQueryClient(channel, Duration.ofSeconds(1));
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.shutdownNow();
    }

    @Test
    void callsCourseOverStandardGrpcUnaryMethod() {
        assertThat(client.getCourse(1001L))
                .extracting("id", "code", "name", "credit", "status")
                .containsExactly(1001L, "COURSE-1", "Course One", 3, "ACTIVE");
    }

    @Test
    void callsExamOverStandardGrpcUnaryMethod() {
        assertThat(client.getExam(2001L))
                .extracting("id", "courseId", "title", "status")
                .containsExactly(2001L, 1001L, "Exam One", "PUBLISHED");
    }

    @Test
    void callsScoreOverStandardGrpcUnaryMethod() {
        assertThat(client.getScore(2001L, 3001L))
                .extracting("id", "examId", "courseId", "studentId", "points", "status")
                .containsExactly(3001L, 2001L, 1001L, 4001L, 95, "RECORDED");
    }

    @Test
    void rejectsNonPositiveIdsBeforeOpeningAnRpcCall() {
        assertThatThrownBy(() -> client.getCourse(0L))
                .isInstanceOf(ExternalDependencyException.class)
                .hasMessageContaining("courseId");
        assertThatThrownBy(() -> client.getScore(1001L, 0L))
                .isInstanceOf(ExternalDependencyException.class)
                .hasMessageContaining("scoreId");
    }

    private static ServerServiceDefinition courseService() {
        return ServerServiceDefinition.builder("egon.evaluation.v1.CourseService")
                .addMethod(courseMethod(), ServerCalls.asyncUnaryCall(
                        (GetCourseRequest request, StreamObserver<Course> observer) -> {
                            observer.onNext(Course.newBuilder()
                                    .setId(request.getCourseId())
                                    .setCode("COURSE-1")
                                    .setName("Course One")
                                    .setCredit(3)
                                    .setStatus("ACTIVE")
                                    .build());
                            observer.onCompleted();
                        }))
                .build();
    }

    private static ServerServiceDefinition examService() {
        return ServerServiceDefinition.builder("egon.evaluation.v1.ExamService")
                .addMethod(examMethod(), ServerCalls.asyncUnaryCall(
                        (GetExamRequest request, StreamObserver<Exam> observer) -> {
                            observer.onNext(Exam.newBuilder()
                                    .setId(request.getExamId())
                                    .setCourseId(1001)
                                    .setTitle("Exam One")
                                    .setStartsAt(Timestamp.newBuilder().setSeconds(1).build())
                                    .setEndsAt(Timestamp.newBuilder().setSeconds(2).build())
                                    .setStatus("PUBLISHED")
                                    .build());
                            observer.onCompleted();
                        }))
                .build();
    }

    private static ServerServiceDefinition scoreService() {
        return ServerServiceDefinition.builder("egon.evaluation.v1.ScoreService")
                .addMethod(scoreMethod(), ServerCalls.asyncUnaryCall(
                        (GetScoreRequest request, StreamObserver<Score> observer) -> {
                            observer.onNext(Score.newBuilder()
                                    .setId(request.getScoreId())
                                    .setExamId(request.getExamId())
                                    .setCourseId(1001)
                                    .setStudentId(4001)
                                    .setPoints(95)
                                    .setStatus("RECORDED")
                                    .build());
                            observer.onCompleted();
                        }))
                .build();
    }

    private static MethodDescriptor<GetCourseRequest, Course> courseMethod() {
        return unary("egon.evaluation.v1.CourseService", "GetCourse",
                GetCourseRequest.getDefaultInstance(), Course.getDefaultInstance());
    }

    private static MethodDescriptor<GetExamRequest, Exam> examMethod() {
        return unary("egon.evaluation.v1.ExamService", "GetExam",
                GetExamRequest.getDefaultInstance(), Exam.getDefaultInstance());
    }

    private static MethodDescriptor<GetScoreRequest, Score> scoreMethod() {
        return unary("egon.evaluation.v1.ScoreService", "GetScore",
                GetScoreRequest.getDefaultInstance(), Score.getDefaultInstance());
    }

    private static <Req extends Message, Resp extends Message> MethodDescriptor<Req, Resp> unary(
            String service, String method, Req request, Resp response) {
        return MethodDescriptor.<Req, Resp>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(service, method))
                .setRequestMarshaller(ProtoUtils.marshaller(request))
                .setResponseMarshaller(ProtoUtils.marshaller(response))
                .build();
    }
}
