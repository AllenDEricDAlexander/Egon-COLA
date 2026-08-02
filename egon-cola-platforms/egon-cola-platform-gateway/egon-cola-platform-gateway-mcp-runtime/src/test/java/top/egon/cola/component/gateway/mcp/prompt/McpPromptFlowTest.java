package top.egon.cola.component.gateway.mcp.prompt;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpPromptFlowTest {

    private final StrictPromptTemplate template = new StrictPromptTemplate();

    @Test
    void strictTemplateDoesNotExecuteExpressionsOrUndeclaredVariables() {
        assertRejected(() -> template.render(
                "${T(java.lang.Runtime).getRuntime()}",
                List.of(),
                Map.of()
        ));
        assertRejected(() -> template.render(
                "Hello ${name}",
                List.of("approved"),
                Map.of("name", "Mario")
        ));
        assertRejected(() -> template.render(
                "Hello ${name}",
                List.of("name"),
                Map.of("name", "Mario", "secret", "hidden")
        ));
    }

    @Test
    void staticDriverRendersOnlyDeclaredLiteralValues() {
        StaticPromptDriver driver = new StaticPromptDriver(template);
        McpPromptDriver.Result result = Mono.from(driver.render(
                prompt("Review ${subject}: ${content}"),
                Map.of(
                        "subject", "invoice",
                        "content", "${notEvaluated}"
                ),
                Map.of()
        )).block();

        assertEquals(1, result.messages().size());
        assertEquals(
                "Review invoice: ${notEvaluated}",
                result.messages().getFirst().text()
        );
    }

    private McpRuntimePrompt prompt(String value) {
        return new McpRuntimePrompt(
                "prompt-1",
                "finance",
                "review",
                "Review a document",
                "STRICT_TEMPLATE",
                value,
                null,
                null,
                List.of("content", "subject"),
                Set.of("finance:review"),
                true
        );
    }

    private void assertRejected(org.junit.jupiter.api.function.Executable call) {
        McpProtocolException failure = assertThrows(
                McpProtocolException.class,
                call
        );
        assertEquals(McpErrorCode.MCP_INVALID_PARAMS, failure.code());
    }
}
