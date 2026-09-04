package top.egon.cola.component.agentflow.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.agentflow.autoconfigure.AgentFlowProperties;
import top.egon.cola.component.agentflow.exception.AgentFlowConfigurationException;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentFlowConfigValidatorTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static AgentFlowConfigValidator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = new AgentFlowConfigValidator(new ValidationUtils(validatorFactory.getValidator()));
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void accepts_leaf_and_three_workflow_root_shapes() {
        for (AgentFlowConfigDTO flow : List.of(
                validFlow("planner"),
                validFlow("sequence"),
                validFlow("parallel"),
                validFlow("loop"))) {
            validator.validate(properties(flow));
        }
    }

    @Test
    void rejects_duplicate_names_and_output_keys() {
        AgentFlowConfigDTO duplicateNames = new AgentFlowConfigDTO(
                "model", "model-name", "root",
                List.of(agent("root", "root-output"), agent("root", "other-output")), List.of());
        AgentFlowConfigDTO duplicateOutputs = new AgentFlowConfigDTO(
                "model", "model-name", "root",
                List.of(agent("root", "same"), agent("child", "same")),
                List.of(workflow(AgentWorkflowTypeEnum.SEQUENTIAL, "sequence", List.of("root", "child"), null)));

        assertConfigurationFailure(duplicateNames);
        assertConfigurationFailure(duplicateOutputs);
    }

    @Test
    void rejects_missing_references_cycles_multi_parent_and_unreachable_nodes() {
        AgentFlowConfigDTO missingReference = new AgentFlowConfigDTO(
                "model", "model-name", "root", List.of(agent("root", null)),
                List.of(workflow(AgentWorkflowTypeEnum.SEQUENTIAL, "sequence", List.of("missing"), null)));
        AgentFlowConfigDTO cycle = new AgentFlowConfigDTO(
                "model", "model-name", "sequence", List.of(agent("leaf", null)),
                List.of(
                        workflow(AgentWorkflowTypeEnum.SEQUENTIAL, "sequence", List.of("parallel"), null),
                        workflow(AgentWorkflowTypeEnum.PARALLEL, "parallel", List.of("sequence"), null)));
        AgentFlowConfigDTO multiParent = new AgentFlowConfigDTO(
                "model", "model-name", "root", List.of(agent("leaf", null)),
                List.of(
                        workflow(AgentWorkflowTypeEnum.SEQUENTIAL, "root", List.of("leaf"), null),
                        workflow(AgentWorkflowTypeEnum.PARALLEL, "other", List.of("leaf"), null)));
        AgentFlowConfigDTO unreachable = new AgentFlowConfigDTO(
                "model", "model-name", "root", List.of(agent("root", null), agent("orphan", null)), List.of());

        assertConfigurationFailure(missingReference);
        assertConfigurationFailure(cycle);
        assertConfigurationFailure(multiParent);
        assertConfigurationFailure(unreachable);
    }

    @Test
    void enforces_loop_bounds_and_type_specific_iterations() {
        AgentFlowConfigDTO invalidLoop = new AgentFlowConfigDTO(
                "model", "model-name", "loop", List.of(agent("leaf", null)),
                List.of(workflow(AgentWorkflowTypeEnum.LOOP, "loop", List.of("leaf"), 101)));
        AgentFlowConfigDTO invalidSequentialIterations = new AgentFlowConfigDTO(
                "model", "model-name", "sequence", List.of(agent("leaf", null)),
                List.of(workflow(AgentWorkflowTypeEnum.SEQUENTIAL, "sequence", List.of("leaf"), 3)));

        assertConfigurationFailure(invalidLoop);
        assertConfigurationFailure(invalidSequentialIterations);
    }

    @Test
    void does_not_include_instruction_content_in_configuration_errors() {
        String instruction = "private instruction that must never be copied to an error";
        AgentFlowConfigDTO invalid = new AgentFlowConfigDTO(
                "model", "model-name", "root",
                List.of(new AgentConfigDTO("root", null, instruction, null), agent("root", null)), List.of());

        assertThatThrownBy(() -> validator.validate(properties(invalid)))
                .isInstanceOf(AgentFlowConfigurationException.class)
                .hasMessageNotContaining(instruction);
    }

    @Test
    void rejects_invalid_names_and_durations() {
        AgentFlowConfigDTO invalidName = new AgentFlowConfigDTO(
                "model", "model-name", "bad/name", List.of(agent("bad/name", null)), List.of());
        assertConfigurationFailure(invalidName);

        AgentFlowProperties zeroTimeout = new AgentFlowProperties(true, Duration.ZERO, Duration.ofSeconds(10),
                Map.of("research-flow", validFlow("planner")));
        AgentFlowProperties longShutdown = new AgentFlowProperties(true, Duration.ofMinutes(2), Duration.ofMinutes(2),
                Map.of("research-flow", validFlow("planner")));

        assertThatThrownBy(() -> validator.validate(zeroTimeout))
                .isInstanceOf(AgentFlowConfigurationException.class);
        assertThatThrownBy(() -> validator.validate(longShutdown))
                .isInstanceOf(AgentFlowConfigurationException.class);
    }

    private static void assertConfigurationFailure(AgentFlowConfigDTO flow) {
        assertThatThrownBy(() -> validator.validate(properties(flow)))
                .isInstanceOf(AgentFlowConfigurationException.class);
    }

    private static AgentFlowProperties properties(AgentFlowConfigDTO flow) {
        return new AgentFlowProperties(true, Duration.ofMinutes(2), Duration.ofSeconds(10), Map.of("research-flow", flow));
    }

    private static AgentFlowConfigDTO validFlow(String root) {
        AgentConfigDTO planner = agent("planner", "plan");
        AgentConfigDTO writer = agent("writer", "answer");
        return switch (root) {
            case "planner" -> new AgentFlowConfigDTO("model", "model-name", root, List.of(planner), List.of());
            case "sequence" -> new AgentFlowConfigDTO("model", "model-name", root, List.of(planner, writer),
                    List.of(workflow(AgentWorkflowTypeEnum.SEQUENTIAL, root, List.of("planner", "writer"), null)));
            case "parallel" -> new AgentFlowConfigDTO("model", "model-name", root, List.of(planner, writer),
                    List.of(workflow(AgentWorkflowTypeEnum.PARALLEL, root, List.of("planner", "writer"), null)));
            case "loop" -> new AgentFlowConfigDTO("model", "model-name", root, List.of(planner),
                    List.of(workflow(AgentWorkflowTypeEnum.LOOP, root, List.of("planner"), 3)));
            default -> throw new IllegalArgumentException(root);
        };
    }

    private static AgentConfigDTO agent(String name, String outputKey) {
        return new AgentConfigDTO(name, name + " description", "Run " + name, outputKey);
    }

    private static AgentWorkflowConfigDTO workflow(AgentWorkflowTypeEnum type, String name,
                                                    List<String> children, Integer maxIterations) {
        return new AgentWorkflowConfigDTO(type, name, name + " workflow", children, maxIterations);
    }
}
