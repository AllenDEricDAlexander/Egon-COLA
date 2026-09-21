package top.egon.cola.archetype.source.service.contract;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Frozen ownership contract of the service-owned native facade.
 *
 * <p>Every value asserted here is read from the pre-migration shared artifact: the Protobuf wire
 * names, the Egon RPC group/version/idempotence and the merged provider bean names must survive the
 * move into {@code egon-cola-source-service-facade} unchanged.
 */
class OwnedFacadeContractTest {

    private static final String WIRE_PACKAGE = "top.egon.cola.evaluation.facade.rpc.v1.";
    private static final String FACADE_ROOT = "top.egon.cola.archetype.source.service.facade.";
    private static final String PROTO_ROOT = FACADE_ROOT + "proto.";
    private static final String ADAPTER_ROOT = "top.egon.cola.archetype.source.service.adapter.";
    private static final String RETIRED_SHARED_ROOT = "top.egon.cola.evaluation.facade.";

    private record Operation(String methodName, String rpcName, boolean idempotent,
                             String requestMessage, String responseMessage) {
    }

    private record Contract(String interfaceName, String implName, String beanName,
                            String grpcService, String group, List<Operation> operations) {
    }

    private static final List<Contract> CONTRACTS = List.of(
            new Contract("course.CourseFacade", "course.facade.impl.CourseFacadeImpl", "courseFacadeImpl",
                    "CourseServiceGrpc", "course", List.of(
                    new Operation("createCourse", "CreateCourse", false, "CreateCourseRpcRequest", "CourseRpcResponse"),
                    new Operation("scheduleCourse", "ScheduleCourse", false, "ScheduleCourseRpcRequest", "CourseScheduleRpcResponse"),
                    new Operation("getCourse", "GetCourse", true, "GetCourseRpcRequest", "CourseRpcResponse"),
                    new Operation("pageCourses", "PageCourses", true, "PageCourseRpcRequest", "PageCourseRpcResponse"))),
            new Contract("exam.ExamFacade", "exam.facade.impl.ExamFacadeImpl", "examFacadeImpl",
                    "ExamServiceGrpc", "exam", List.of(
                    new Operation("createExam", "CreateExam", false, "CreateExamRpcRequest", "ExamRpcResponse"),
                    new Operation("attachPaper", "AttachPaper", false, "AttachExamPaperRpcRequest", "ExamPaperRpcResponse"),
                    new Operation("publishExam", "PublishExam", false, "PublishExamRpcRequest", "ExamRpcResponse"),
                    new Operation("getExam", "GetExam", true, "GetExamRpcRequest", "ExamRpcResponse"))),
            new Contract("exam.ScoreFacade", "exam.facade.impl.ScoreFacadeImpl", "scoreFacadeImpl",
                    "ScoreServiceGrpc", "score", List.of(
                    new Operation("recordScore", "RecordScore", false, "RecordScoreRpcRequest", "ScoreRpcResponse"),
                    new Operation("getScore", "GetScore", true, "GetScoreRpcRequest", "ScoreRpcResponse"),
                    new Operation("pageScores", "PageScores", true, "PageScoreRpcRequest", "PageScoreRpcResponse"))));

    @Test
    void declares_one_owned_facade_per_native_contract_with_frozen_rpc_metadata() {
        for (Contract contract : CONTRACTS) {
            Class<?> facade = load(FACADE_ROOT + contract.interfaceName());
            assertThat(facade.isInterface()).as("%s stays an interface", facade).isTrue();

            Annotation service = annotation(facade, "EgonRpcService");
            assertThat(attribute(service, "group")).as("%s group", facade).isEqualTo(contract.group());
            assertThat(attribute(service, "version")).as("%s version", facade).isEqualTo("1.0.0");
            assertThat(attribute(service, "retries")).as("%s retries", facade).isEqualTo(0);
            assertThat(((Class<?>) attribute(service, "grpcClass")).getName())
                    .as("%s grpc owner", facade).isEqualTo(PROTO_ROOT + contract.grpcService());

            Set<String> declared = new LinkedHashSet<>();
            for (Method method : facade.getDeclaredMethods()) {
                Annotation rpcMethod = annotation(method, "EgonRpcMethod");
                declared.add(String.join("|", method.getName(),
                        String.valueOf(attribute(rpcMethod, "name")),
                        String.valueOf(attribute(rpcMethod, "idempotent")),
                        method.getParameterTypes()[0].getName(),
                        method.getReturnType().getName()));
                assertThat(method.getParameterCount()).as("%s arity", method).isEqualTo(1);
            }
            Set<String> expected = new LinkedHashSet<>();
            for (Operation operation : contract.operations()) {
                expected.add(String.join("|", operation.methodName(), operation.rpcName(),
                        String.valueOf(operation.idempotent()),
                        PROTO_ROOT + operation.requestMessage(), PROTO_ROOT + operation.responseMessage()));
            }
            assertThat(declared).as("%s operations", facade).containsExactlyInAnyOrderElementsOf(expected);
        }
    }

    @Test
    void keeps_every_frozen_protobuf_wire_name_inside_the_owned_java_package() {
        List<String> messages = new ArrayList<>();
        for (Contract contract : CONTRACTS) {
            for (Operation operation : contract.operations()) {
                messages.add(operation.requestMessage());
                messages.add(operation.responseMessage());
            }
        }
        Set<String> inspected = new LinkedHashSet<>(messages);
        for (String message : inspected) {
            Class<?> type = load(PROTO_ROOT + message);
            Object descriptor = staticNoArg(type, "getDescriptor");
            assertThat(invoke(descriptor, "getFullName"))
                    .as("%s wire name", message).isEqualTo(WIRE_PACKAGE + message);
        }
        for (Contract contract : CONTRACTS) {
            Class<?> grpcOwner = load(PROTO_ROOT + contract.grpcService());
            Object serviceDescriptor = staticNoArg(grpcOwner, "getServiceDescriptor");
            assertThat(invoke(serviceDescriptor, "getName"))
                    .as("%s service wire name", grpcOwner)
                    .isEqualTo(WIRE_PACKAGE + contract.grpcService().replace("Grpc", ""));
        }
    }

    @Test
    void merges_the_split_provider_and_dto_facade_into_one_named_provider_component() {
        for (Contract contract : CONTRACTS) {
            Class<?> facade = load(FACADE_ROOT + contract.interfaceName());
            Class<?> impl = load(ADAPTER_ROOT + contract.implName());
            assertThat(facade.isAssignableFrom(impl)).as("%s implements %s", impl, facade).isTrue();
            annotation(impl, "EgonRpcProvider");
            assertThat(springBeanName(impl)).as("%s bean name", impl).isEqualTo(contract.beanName());
            assertThat(impl.getInterfaces()).as("%s owns exactly one facade", impl).containsExactly(facade);
        }
    }

    @Test
    void retires_the_separate_rpc_providers_and_the_shared_evaluation_artifact() {
        for (String retired : List.of("course.rpc.CourseRpcProvider", "exam.rpc.ExamRpcProvider",
                "exam.rpc.ScoreRpcProvider")) {
            assertAbsent(ADAPTER_ROOT + retired);
        }
        for (String retired : List.of("rpc.CourseRpcService", "rpc.ExamRpcService", "rpc.ScoreRpcService",
                "rpc.EvaluationRpcConverter", "course.CourseFacade", "exam.ExamFacade", "exam.ScoreFacade",
                "dto.SingleResponse", "dto.PageResponse", "dto.Response",
                "course.dto.CreateCourseRequest", "exceptions.EvaluationFacadeException",
                "enums.EvaluationFacadeErrorCode", "utils.EvaluationFacadeAssert")) {
            assertAbsent(RETIRED_SHARED_ROOT + retired);
        }
    }

    @Test
    void keeps_the_facade_free_of_the_retired_transport_carriers() {
        assertThatNoException().isThrownBy(() -> load(FACADE_ROOT + "course.CourseFacade"));
        assertThat(List.of(load(FACADE_ROOT + "course.CourseFacade").getMethods()))
                .allSatisfy(method -> assertThat(method.getParameterTypes()[0].getName()).startsWith(PROTO_ROOT));
    }

    private static void assertAbsent(String typeName) {
        assertThatExceptionOfType(ClassNotFoundException.class)
                .as("%s must no longer resolve", typeName)
                .isThrownBy(() -> Class.forName(typeName));
    }

    private static Class<?> load(String typeName) {
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Missing owned facade contract: " + typeName, exception);
        }
    }

    private static Annotation annotation(java.lang.reflect.AnnotatedElement element, String simpleName) {
        for (Annotation candidate : element.getAnnotations()) {
            if (candidate.annotationType().getSimpleName().equals(simpleName)) {
                return candidate;
            }
        }
        throw new AssertionError(element + " must be annotated with @" + simpleName);
    }

    private static Object attribute(Annotation annotation, String name) {
        return invoke(annotation, name);
    }

    private static String springBeanName(Class<?> type) {
        for (Annotation candidate : type.getAnnotations()) {
            String simpleName = candidate.annotationType().getSimpleName();
            if (simpleName.equals("Component") || simpleName.equals("Service")) {
                return String.valueOf(attribute(candidate, "value"));
            }
        }
        throw new AssertionError(type + " must be a named Spring component");
    }

    private static Object staticNoArg(Class<?> type, String name) {
        try {
            return type.getMethod(name).invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(type + " must expose static " + name + "()", exception);
        }
    }

    private static Object invoke(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(target.getClass() + " must expose " + name + "()", exception);
        }
    }
}
