package top.egon.cola.component.agentflow.config;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.agentflow.autoconfigure.AgentFlowProperties;
import top.egon.cola.component.agentflow.exception.AgentFlowConfigurationException;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Validates field constraints and the single-root Agent Flow graph before ADK construction. */
@RequiredArgsConstructor
public class AgentFlowConfigValidator {

    private static final Pattern ADK_NAME_PATTERN =
            Pattern.compile("^_?[a-zA-Z0-9]*([. _-][a-zA-Z0-9]+)*$");
    private static final int MAX_FLOWS = 128;
    private static final int MAX_NODES = 128;
    private static final int MAX_CHILDREN = 128;
    private static final int MAX_LOOP_ITERATIONS = 100;

    @Qualifier("agentFlowValidationUtils")
    private final ValidationUtils validationUtils;

    public AgentFlowProperties validate(AgentFlowProperties properties) {
        try {
            validationUtils.validate(properties);
        } catch (ConstraintViolationException failure) {
            String property = failure.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .sorted()
                    .findFirst()
                    .orElse("properties");
            throw configuration("properties", property, "constraint violation", failure);
        }
        validateDuration(properties.executionTimeout(), Duration.ofHours(1), "executionTimeout");
        validateDuration(properties.shutdownTimeout(), Duration.ofMinutes(1), "shutdownTimeout");
        if (!properties.enabled()) {
            return properties;
        }
        if (properties.flows().isEmpty() || properties.flows().size() > MAX_FLOWS) {
            throw configuration("properties", "flows", "flow count must be between 1 and 128", null);
        }
        properties.flows().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(java.util.Comparator.nullsFirst(String::compareTo)))
                .forEach(entry -> validateFlow(entry.getKey(), entry.getValue()));
        return properties;
    }

    private void validateFlow(String flowId, AgentFlowConfigDTO flow) {
        requireName(flowId, "flow id", flowId);
        if (flow == null) {
            throw configuration(flowId, "flow", "flow configuration is required", null);
        }
        if (flow.agents().size() > MAX_NODES || flow.workflows().size() > MAX_NODES
                || flow.agents().size() + flow.workflows().size() > MAX_NODES) {
            throw configuration(flowId, "flow", "node count exceeds 128", null);
        }
        Map<String, AgentConfigDTO> agents = new LinkedHashMap<>();
        for (AgentConfigDTO agent : flow.agents()) {
            requireName(flowId, agent == null ? "agent" : agent.name(), "agent name");
            if (agents.putIfAbsent(agent.name(), agent) != null) {
                throw configuration(flowId, agent.name(), "duplicate node name", null);
            }
        }
        Map<String, AgentWorkflowConfigDTO> workflows = new LinkedHashMap<>();
        for (AgentWorkflowConfigDTO workflow : flow.workflows()) {
            if (workflow == null) {
                throw configuration(flowId, "workflow", "workflow is required", null);
            }
            requireName(flowId, workflow.name(), "workflow name");
            if (agents.containsKey(workflow.name()) || workflows.putIfAbsent(workflow.name(), workflow) != null) {
                throw configuration(flowId, workflow.name(), "duplicate node name", null);
            }
            if (workflow.subAgentNames().size() > MAX_CHILDREN) {
                throw configuration(flowId, workflow.name(), "child count exceeds 128", null);
            }
            if (workflow.type() == AgentWorkflowTypeEnum.LOOP
                    && (workflow.maxIterations() == null || workflow.maxIterations() < 1
                    || workflow.maxIterations() > MAX_LOOP_ITERATIONS)) {
                throw configuration(flowId, workflow.name(), "loop maxIterations must be between 1 and 100", null);
            }
            if (workflow.type() != AgentWorkflowTypeEnum.LOOP && workflow.maxIterations() != null) {
                throw configuration(flowId, workflow.name(), "maxIterations is only valid for LOOP", null);
            }
        }

        Map<String, String> outputKeys = new HashMap<>();
        agents.values().forEach(agent -> {
            if (agent.outputKey() != null && outputKeys.putIfAbsent(agent.outputKey(), agent.name()) != null) {
                throw configuration(flowId, agent.name(), "duplicate outputKey", null);
            }
        });

        Map<String, Integer> parents = new LinkedHashMap<>();
        Map<String, List<String>> children = new LinkedHashMap<>();
        agents.keySet().forEach(name -> parents.put(name, 0));
        workflows.keySet().forEach(name -> parents.put(name, 0));
        for (AgentWorkflowConfigDTO workflow : workflows.values()) {
            Set<String> localChildren = new LinkedHashSet<>();
            for (String child : workflow.subAgentNames()) {
                if (!localChildren.add(child)) {
                    throw configuration(flowId, workflow.name(), "duplicate child reference", null);
                }
                if (!parents.containsKey(child)) {
                    throw configuration(flowId, workflow.name(), "missing child reference", null);
                }
                parents.computeIfPresent(child, (ignored, count) -> count + 1);
            }
            children.put(workflow.name(), List.copyOf(localChildren));
        }
        if (!parents.containsKey(flow.rootAgentName())) {
            throw configuration(flowId, flow.rootAgentName(), "root node does not exist", null);
        }
        if (parents.get(flow.rootAgentName()) != 0) {
            throw configuration(flowId, flow.rootAgentName(), "root node cannot have a parent", null);
        }
        parents.forEach((name, count) -> {
            if (!name.equals(flow.rootAgentName()) && count != 1) {
                throw configuration(flowId, name, "node must have exactly one parent", null);
            }
        });
        visitAndRejectCycles(flowId, flow.rootAgentName(), children);
        Set<String> reachable = reachable(flow.rootAgentName(), children);
        if (reachable.size() != parents.size()) {
            String unreachable = parents.keySet().stream().filter(name -> !reachable.contains(name)).findFirst()
                    .orElse("flow");
            throw configuration(flowId, unreachable, "node is unreachable from root", null);
        }
    }

    private static void visitAndRejectCycles(String flowId, String node, Map<String, List<String>> children) {
        Map<String, Integer> colors = new HashMap<>();
        ArrayDeque<String> stack = new ArrayDeque<>();
        visit(flowId, node, children, colors, stack);
    }

    private static void visit(String flowId, String node, Map<String, List<String>> children,
                              Map<String, Integer> colors, ArrayDeque<String> stack) {
        int color = colors.getOrDefault(node, 0);
        if (color == 1) {
            throw configuration(flowId, node, "cycle detected", null);
        }
        if (color == 2) {
            return;
        }
        colors.put(node, 1);
        stack.push(node);
        for (String child : children.getOrDefault(node, List.of())) {
            visit(flowId, child, children, colors, stack);
        }
        stack.pop();
        colors.put(node, 2);
    }

    private static Set<String> reachable(String root, Map<String, List<String>> children) {
        Set<String> seen = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (!seen.add(current)) {
                continue;
            }
            pending.addAll(children.getOrDefault(current, List.of()));
        }
        return seen;
    }

    private static void requireName(String flowId, String value, String label) {
        if (value == null || value.length() > 128 || !ADK_NAME_PATTERN.matcher(value).matches()) {
            throw configuration(flowId, value == null ? label : value, label + " does not match ADK name rules", null);
        }
    }

    private static void validateDuration(Duration value, Duration max, String field) {
        if (value == null || value.isZero() || value.isNegative() || value.compareTo(max) > 0) {
            throw configuration("properties", field, "duration is outside the allowed range", null);
        }
    }

    private static AgentFlowConfigurationException configuration(String flowId, String node, String reason,
                                                                 Throwable cause) {
        return cause == null
                ? new AgentFlowConfigurationException(flowId, node, reason)
                : new AgentFlowConfigurationException(flowId, node, reason, cause);
    }
}
