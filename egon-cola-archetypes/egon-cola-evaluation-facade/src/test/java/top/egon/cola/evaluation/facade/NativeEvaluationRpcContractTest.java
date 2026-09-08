package top.egon.cola.evaluation.facade;

import io.grpc.MethodDescriptor.MethodType;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.exception.EgonRpcException;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeEvaluationRpcContractTest {

    private static final Map<String, Set<String>> OPERATIONS = Map.of(
            "Course", Set.of("CreateCourse", "ScheduleCourse", "GetCourse", "PageCourses"),
            "Exam", Set.of("CreateExam", "AttachPaper", "PublishExam", "GetExam"),
            "Score", Set.of("RecordScore", "GetScore", "PageScores")
    );

    @Test
    void validatesEveryNativeOperation() {
        RpcContractValidator validator = new RpcContractValidator();
        OPERATIONS.forEach((service, methods) -> {
            Class<?> contract = assertDoesNotThrow(
                    () -> Class.forName("top.egon.cola.evaluation.facade.rpc." + service + "RpcService"),
                    "Native production RPC contract is missing: " + service);
            var descriptor = validator.validate(contract);
            assertEquals(methods, descriptor.methods().stream()
                    .map(method -> method.methodName()).collect(Collectors.toSet()));
            assertEquals("1.0.0", descriptor.version());
            assertTrue(descriptor.methods().stream()
                    .allMatch(method -> method.grpcMethod().getType() == MethodType.UNARY));
        });
    }

    @Test
    void rejectsContractsWithoutNativeAnnotations() {
        assertThrows(EgonRpcException.class,
                () -> new RpcContractValidator().validate(UnannotatedRpcService.class));
        assertThrows(EgonRpcException.class,
                () -> new RpcContractValidator().validate(String.class));
    }

    interface UnannotatedRpcService {
        String get(String request);
    }

    @Test
    void rejectsDescriptorMismatchAndOverloadedContracts() {
        RpcContractValidator validator = new RpcContractValidator();
        assertEquals(top.egon.cola.component.rpc.exception.EgonRpcErrorCode.RPC_INVALID_CONTRACT,
                assertThrows(EgonRpcException.class, () -> validator.validate(MismatchedRpcService.class)).getCode());
        assertThrows(EgonRpcException.class, () -> validator.validate(OverloadedRpcService.class));
    }

    @Test
    void requiresNonNullRequestsAndKeepsMutationRetriesDisabled() throws Exception {
        for (String service : OPERATIONS.keySet()) {
            Class<?> contract = Class.forName("top.egon.cola.evaluation.facade.rpc." + service + "RpcService");
            var annotation = contract.getAnnotation(top.egon.cola.component.rpc.annotation.EgonRpcService.class);
            assertEquals(0, annotation.retries());
            for (var descriptor : new RpcContractValidator().validate(contract).methods()) {
                assertTrue(java.util.Arrays.stream(descriptor.javaMethod().getParameterAnnotations()[0])
                        .anyMatch(a -> a.annotationType() == jakarta.validation.constraints.NotNull.class));
                boolean query = descriptor.methodName().startsWith("Get") || descriptor.methodName().startsWith("Page");
                assertEquals(query, descriptor.idempotent());
            }
        }
    }

    @top.egon.cola.component.rpc.annotation.EgonRpcService(grpcClass = top.egon.cola.evaluation.facade.rpc.proto.CourseServiceGrpc.class)
    interface MismatchedRpcService {
        @top.egon.cola.component.rpc.annotation.EgonRpcMethod(name = "GetCourse")
        com.google.protobuf.Empty get(com.google.protobuf.Empty request);
    }

    @top.egon.cola.component.rpc.annotation.EgonRpcService(grpcClass = top.egon.cola.evaluation.facade.rpc.proto.CourseServiceGrpc.class)
    interface OverloadedRpcService {
        @top.egon.cola.component.rpc.annotation.EgonRpcMethod(name = "GetCourse")
        com.google.protobuf.Empty get(com.google.protobuf.Empty request);

        @top.egon.cola.component.rpc.annotation.EgonRpcMethod(name = "GetCourse")
        com.google.protobuf.Empty get(top.egon.cola.evaluation.facade.rpc.proto.GetCourseRpcRequest request);
    }
}
