#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.client.evaluation;

import ${package}.domain.client.ExternalDependencyException;
import ${package}.domain.client.ExternalDependencyFailure;
import ${package}.domain.client.evaluation.EvaluationCourse;
import ${package}.domain.client.evaluation.EvaluationExam;
import ${package}.domain.client.evaluation.EvaluationQueryPort;
import ${package}.domain.client.evaluation.EvaluationScore;
import ${package}.infrastructure.config.OrganizationIntegrationProperties;
import com.google.protobuf.Message;
import ${package}.facade.evaluation.v1.Course;
import ${package}.facade.evaluation.v1.Exam;
import ${package}.facade.evaluation.v1.GetCourseRequest;
import ${package}.facade.evaluation.v1.GetExamRequest;
import ${package}.facade.evaluation.v1.GetScoreRequest;
import ${package}.facade.evaluation.v1.Score;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Standard gRPC client for the three read-only Evaluation Triple methods. */
@Component
@Profile({"dev", "prod"})
public class GrpcEvaluationQueryClient implements EvaluationQueryPort {

    private static final String COURSE_SERVICE = "egon.evaluation.v1.CourseService";
    private static final String EXAM_SERVICE = "egon.evaluation.v1.ExamService";
    private static final String SCORE_SERVICE = "egon.evaluation.v1.ScoreService";

    private final ManagedChannel channel;
    private final Duration deadline;

    public GrpcEvaluationQueryClient(OrganizationIntegrationProperties properties) {
        this(ManagedChannelBuilder.forTarget(properties.getIntegrations().getEvaluation().getGrpcTarget())
                        .usePlaintext()
                        .build(),
                properties.getIntegrations().getEvaluation().getGrpcDeadline());
    }

    GrpcEvaluationQueryClient(ManagedChannel channel, Duration deadline) {
        this.channel = channel;
        this.deadline = deadline;
    }

    @Override
    public EvaluationCourse getCourse(Long courseId) {
        long id = positiveId(courseId, "courseId");
        try {
            Course response = ClientCalls.blockingUnaryCall(
                    channel, getCourseMethod(), callOptions(), GetCourseRequest.newBuilder()
                            .setCourseId(id)
                            .build());
            if (response == null || response.getId() <= 0) {
                throw EvaluationClientFailureMapper.incompatible("getCourse");
            }
            return new EvaluationCourse(response.getId(), response.getCode(), response.getName(),
                    response.getCredit(), response.getStatus());
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw EvaluationClientFailureMapper.map(failure);
        }
    }

    @Override
    public EvaluationExam getExam(Long examId) {
        long id = positiveId(examId, "examId");
        try {
            Exam response = ClientCalls.blockingUnaryCall(
                    channel, getExamMethod(), callOptions(), GetExamRequest.newBuilder()
                            .setExamId(id)
                            .build());
            if (response == null || response.getId() <= 0) {
                throw EvaluationClientFailureMapper.incompatible("getExam");
            }
            return new EvaluationExam(response.getId(), response.getCourseId(),
                    response.getTitle(), timestamp(response.getStartsAt().getSeconds(), response.getStartsAt().getNanos()),
                    timestamp(response.getEndsAt().getSeconds(), response.getEndsAt().getNanos()), response.getStatus());
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw EvaluationClientFailureMapper.map(failure);
        }
    }

    @Override
    public EvaluationScore getScore(Long examId, Long scoreId) {
        long exam = positiveId(examId, "examId");
        long score = positiveId(scoreId, "scoreId");
        try {
            Score response = ClientCalls.blockingUnaryCall(
                    channel, getScoreMethod(), callOptions(), GetScoreRequest.newBuilder()
                            .setExamId(exam)
                            .setScoreId(score)
                            .build());
            if (response == null || response.getId() <= 0) {
                throw EvaluationClientFailureMapper.incompatible("getScore");
            }
            return new EvaluationScore(response.getId(), response.getExamId(),
                    response.getCourseId(), response.getStudentId(),
                    response.getPoints(), response.getStatus());
        } catch (ExternalDependencyException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw EvaluationClientFailureMapper.map(failure);
        }
    }

    @PreDestroy
    public void close() {
        channel.shutdown();
    }

    private CallOptions callOptions() {
        return CallOptions.DEFAULT.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
    }

    private static MethodDescriptor<GetCourseRequest, Course> getCourseMethod() {
        return unary(COURSE_SERVICE, "GetCourse", GetCourseRequest.getDefaultInstance(), Course.getDefaultInstance());
    }

    private static MethodDescriptor<GetExamRequest, Exam> getExamMethod() {
        return unary(EXAM_SERVICE, "GetExam", GetExamRequest.getDefaultInstance(), Exam.getDefaultInstance());
    }

    private static MethodDescriptor<GetScoreRequest, Score> getScoreMethod() {
        return unary(SCORE_SERVICE, "GetScore", GetScoreRequest.getDefaultInstance(), Score.getDefaultInstance());
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

    private static long positiveId(Long raw, String field) {
        if (raw == null || raw <= 0) {
            throw new ExternalDependencyException(
                    "evaluation", ExternalDependencyFailure.VALIDATION_FAILED, "INVALID_ID",
                    field + " must be a positive Long", null);
        }
        return raw;
    }

    private static Instant timestamp(long seconds, int nanos) {
        return Instant.ofEpochSecond(seconds, nanos);
    }
}
