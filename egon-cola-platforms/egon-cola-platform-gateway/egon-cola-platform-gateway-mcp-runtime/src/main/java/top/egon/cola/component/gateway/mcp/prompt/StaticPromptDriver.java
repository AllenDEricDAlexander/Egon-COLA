package top.egon.cola.component.gateway.mcp.prompt;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class StaticPromptDriver implements McpPromptDriver {

    private static final Set<String> SOURCE_TYPES = Set.of(
            "LOCAL_TEMPLATE",
            "STATIC_TEMPLATE",
            "STRICT_TEMPLATE"
    );

    private final StrictPromptTemplate template;

    public StaticPromptDriver(StrictPromptTemplate template) {
        this.template = Objects.requireNonNull(template, "template");
    }

    @Override
    public Set<String> sourceTypes() {
        return SOURCE_TYPES;
    }

    @Override
    public Mono<Result> render(
            McpRuntimePrompt prompt,
            Map<String, String> arguments,
            Map<String, Object> attributes) {
        if (prompt.template() == null) {
            throw McpPromptDriver.invalid(
                    "MCP prompt template is not configured"
            );
        }
        String rendered = template.render(
                prompt.template(),
                prompt.arguments(),
                arguments
        );
        return Mono.just(new Result(
                prompt.description(),
                List.of(new Message("user", rendered))
        ));
    }
}
