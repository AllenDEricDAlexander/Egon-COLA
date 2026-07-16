package sample.bytecode.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import top.egon.cola.component.bytecode.api.observation.ObservationEvent;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.runtime.DefaultBytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.runtime.context.CompositeContextCarrier;
import top.egon.cola.component.bytecode.runtime.event.BoundedFailureStore;
import top.egon.cola.component.bytecode.runtime.event.RuntimeEventFanout;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorNameResolver;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorTaskDecorator;
import top.egon.cola.component.bytecode.runtime.executor.RuntimeTaskDetector;
import top.egon.cola.component.bytecode.runtime.observation.ObservationRuntime;
import top.egon.cola.component.bytecode.starter.methodextension.MethodExtensionRuntimeAdapter;
import top.egon.cola.component.bytecode.starter.methodextension.MethodMetadataResolver;
import top.egon.cola.component.methodextension.annotation.MethodExtension;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionNotReadyPolicy;
import top.egon.cola.component.methodextension.context.MethodExtensionContext;
import top.egon.cola.component.methodextension.event.MethodExtensionEvent;
import top.egon.cola.component.methodextension.execution.MethodExtensionExecutionService;
import top.egon.cola.component.methodextension.handler.MethodExtensionDecision;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public final class MethodExtensionAgentFixture {

    private MethodExtensionAgentFixture() {
    }

    public static void main(String[] args) throws Exception {
        List<MethodExtensionEvent> extensionEvents = new ArrayList<>();
        List<ObservationEvent> observationEvents = new ArrayList<>();
        DecisionHandler handler = new DecisionHandler();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("decisionHandler", handler);
        beanFactory.registerSingleton("objectMapper", new ObjectMapper());
        MethodExtensionExecutionService service = new MethodExtensionExecutionService(
                new MethodExtensionMethodResolver(),
                new MethodExtensionHandlerResolver(beanFactory),
                new MethodExtensionResponseResolver(
                        beanFactory.getBeanProvider(ObjectMapper.class)),
                extensionEvents::add
        );
        MethodExtensionRuntimeAdapter adapter = new MethodExtensionRuntimeAdapter(
                () -> service,
                new MethodMetadataResolver(),
                MethodExtensionNotReadyPolicy.PROCEED
        );
        adapter.markReady();
        BoundedFailureStore failures = new BoundedFailureStore(8);
        ObservationRuntime observationRuntime = new ObservationRuntime(
                true, 1.0, -1L, observationEvents::add, failures);
        DefaultBytecodeRuntimeDispatcher dispatcher = new DefaultBytecodeRuntimeDispatcher(
                taskDecorator(failures), false, observationRuntime, adapter);
        ClassLoader loader = MethodExtensionAgentFixture.class.getClassLoader();

        try (var registration = DispatcherRegistry.register(loader, "integration", dispatcher)) {
            Target target = new Target();
            Object identity = new Object();
            require(target.allowIdentity(identity) == identity, "allow result identity changed");
            CompletableFuture<String> future = new CompletableFuture<>();
            require(target.allowFuture(future) == future, "allow Future identity changed");
            require("direct-rejected".equals(target.rejectDirect("password=secret")),
                    "direct rejection changed");
            require(new Payload("json-rejected").equals(target.rejectJson()),
                    "JSON rejection changed");
            require("async-rejected".equals(target.rejectAsync().get()),
                    "async rejection changed");
            require("protected-rejected".equals(target.protectedValue()),
                    "protected method was not intercepted");
            require("package-rejected".equals(target.packageValue()),
                    "package method was not intercepted");
            require("private-rejected".equals(target.callPrivate()),
                    "private self-call was not intercepted");
            require("final-rejected".equals(target.finalSynchronized()),
                    "final synchronized method was not intercepted");
            require("interface-rejected".equals(target.interfaceValue("value")),
                    "interface annotation was not resolved");

            try {
                target.failure();
                throw new AssertionError("handler failure was not thrown");
            } catch (IllegalStateException failure) {
                require(failure == DecisionHandler.FAILURE,
                        "handler failure identity changed");
            }
            require("observed-rejected".equals(target.observedReject()),
                    "observed rejection changed");
            require("static-body".equals(Target.staticValue()),
                    "static method must remain excluded");

            require(handler.calls == 12, "Handler invocation count changed: " + handler.calls);
            require(extensionEvents.size() == handler.calls,
                    "Method Extension event count changed");
            require(observationEvents.stream().filter(event ->
                            event.className().equals(Target.class.getName())
                                    && event.methodName().equals("allowIdentity"))
                    .count() == 1L, "allowed method observation missing");
            require(observationEvents.stream().noneMatch(event ->
                            event.className().equals(Target.class.getName())
                                    && event.methodName().equals("observedReject")),
                    "rejected method reached Observation");
            require(extensionEvents.stream().noneMatch(event ->
                            event.toString().contains("password=secret")),
                    "Method Extension event leaked arguments");
            require(failures.failures().isEmpty(), "runtime failure diagnostics are not empty");
            System.out.println("METHOD_EXTENSION_AGENT_OK handlers=" + handler.calls
                    + " events=" + extensionEvents.size());
        }
    }

    private static ExecutorTaskDecorator taskDecorator(BoundedFailureStore failures) {
        return new ExecutorTaskDecorator(
                new CompositeContextCarrier(List.of()),
                new RuntimeEventFanout(List.of(), failures),
                new RuntimeTaskDetector(),
                new ExecutorNameResolver(List.of(), Map.of())
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    interface Contract {

        @MethodExtension(handler = DecisionHandler.class)
        String interfaceValue(String value);
    }

    static final class Target implements Contract {

        @MethodExtension(handler = DecisionHandler.class)
        Object allowIdentity(Object value) {
            return value;
        }

        @MethodExtension(handler = DecisionHandler.class)
        CompletableFuture<String> allowFuture(CompletableFuture<String> value) {
            return value;
        }

        @MethodExtension(handler = DecisionHandler.class)
        String rejectDirect(String sensitive) {
            return sensitive;
        }

        @MethodExtension(
                handler = DecisionHandler.class,
                returnJson = "{\"value\":\"json-rejected\"}"
        )
        Payload rejectJson() {
            return new Payload("body");
        }

        @MethodExtension(handler = DecisionHandler.class)
        Future<String> rejectAsync() {
            return CompletableFuture.completedFuture("body");
        }

        @MethodExtension(handler = DecisionHandler.class)
        protected String protectedValue() {
            return "body";
        }

        @MethodExtension(handler = DecisionHandler.class)
        String packageValue() {
            return "body";
        }

        @MethodExtension(handler = DecisionHandler.class)
        private String privateValue() {
            return "body";
        }

        String callPrivate() {
            return privateValue();
        }

        @MethodExtension(handler = DecisionHandler.class)
        final synchronized String finalSynchronized() {
            return "body";
        }

        @Override
        public String interfaceValue(String value) {
            return value;
        }

        @MethodExtension(handler = DecisionHandler.class)
        void failure() {
        }

        @MethodExtension(handler = DecisionHandler.class)
        String observedReject() {
            return "body";
        }

        @MethodExtension(handler = DecisionHandler.class)
        static String staticValue() {
            return "static-body";
        }
    }

    public static final class DecisionHandler implements MethodExtensionHandler {

        static final IllegalStateException FAILURE =
                new IllegalStateException("handler-sentinel");
        int calls;

        @Override
        public MethodExtensionDecision evaluate(MethodExtensionContext context) {
            calls++;
            return switch (context.method().getName()) {
                case "rejectDirect" -> MethodExtensionDecision.reject("direct-rejected");
                case "rejectJson" -> MethodExtensionDecision.reject();
                case "rejectAsync" -> MethodExtensionDecision.reject("async-rejected");
                case "protectedValue" -> MethodExtensionDecision.reject("protected-rejected");
                case "packageValue" -> MethodExtensionDecision.reject("package-rejected");
                case "privateValue" -> MethodExtensionDecision.reject("private-rejected");
                case "finalSynchronized" -> MethodExtensionDecision.reject("final-rejected");
                case "interfaceValue" -> MethodExtensionDecision.reject("interface-rejected");
                case "failure" -> throw FAILURE;
                case "observedReject" -> MethodExtensionDecision.reject("observed-rejected");
                default -> MethodExtensionDecision.allow();
            };
        }
    }

    record Payload(String value) {
    }
}
