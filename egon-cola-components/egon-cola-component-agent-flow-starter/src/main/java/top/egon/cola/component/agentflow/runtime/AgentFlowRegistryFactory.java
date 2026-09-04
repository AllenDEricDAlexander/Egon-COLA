package top.egon.cola.component.agentflow.runtime;

import com.google.adk.runner.InMemoryRunner;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.agentflow.autoconfigure.AgentFlowProperties;
import top.egon.cola.component.agentflow.config.AgentFlowConfigDTO;
import top.egon.cola.component.agentflow.exception.AgentFlowConfigurationException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compiles and publishes all configured flows atomically. */
@RequiredArgsConstructor
@Slf4j
public class AgentFlowRegistryFactory {

    @Qualifier("agentFlowFactory")
    private final AgentFlowFactory agentFlowFactory;

    @Qualifier("agentFlowClock")
    private final Clock clock;

    public AgentFlowRegistry create(AgentFlowProperties properties, Map<String, ChatModel> chatModels) {
        if (properties == null || chatModels == null) {
            throw new AgentFlowConfigurationException("properties", "flows", "registry inputs are required");
        }
        List<AgentFlowRuntimeBO> created = new ArrayList<>();
        Map<String, AgentFlowRuntimeBO> runtimes = new LinkedHashMap<>();
        try {
            properties.flows().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.nullsFirst(String::compareTo)))
                    .forEach(entry -> {
                        String flowId = entry.getKey();
                        AgentFlowConfigDTO flow = entry.getValue();
                        if (flowId == null || flowId.isBlank() || flow == null) {
                            throw new AgentFlowConfigurationException(
                                    flowId == null ? "flow" : flowId, "flow", "flow configuration is required");
                        }
                        AgentFlowFactory.AgentFlowCompilationBO compilation =
                                agentFlowFactory.create(flowId, flow, chatModels);
                        AgentFlowRuntimeBO runtime = new AgentFlowRuntimeBO(
                                flowId, flow.rootAgentName(), flow.chatModelBeanName(), flow.modelName(),
                                compilation.rootAgent(), compilation.runner(), clock.instant());
                        if (runtimes.putIfAbsent(flowId, runtime) != null) {
                            throw new AgentFlowConfigurationException(flowId, "flow", "duplicate flow id");
                        }
                        created.add(runtime);
                    });
            return new DefaultAgentFlowRegistry(Map.copyOf(runtimes), properties.shutdownTimeout());
        } catch (RuntimeException failure) {
            closeCreated(created, failure);
            throw failure;
        }
    }

    private static void closeCreated(List<AgentFlowRuntimeBO> runtimes, RuntimeException failure) {
        List<Completable> closes = runtimes.reversed().stream()
                .map(AgentFlowRuntimeBO::runner)
                .map(InMemoryRunner::close)
                .toList();
        if (closes.isEmpty()) {
            return;
        }
        try {
            Completable.mergeDelayError(closes).blockingAwait();
        } catch (RuntimeException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }
}
