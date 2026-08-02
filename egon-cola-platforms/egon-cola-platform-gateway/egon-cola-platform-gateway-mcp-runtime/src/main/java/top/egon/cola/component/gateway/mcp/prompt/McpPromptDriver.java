package top.egon.cola.component.gateway.mcp.prompt;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Strategy for rendering one reviewed local prompt descriptor.
 */
public interface McpPromptDriver {

    Set<String> sourceTypes();

    Publisher<Result> render(
            McpRuntimePrompt prompt,
            Map<String, String> arguments,
            Map<String, Object> attributes
    );

    record Message(String role, String text) {

        public Message {
            role = required(role, "role");
            if (!Set.of("user", "assistant").contains(role)) {
                throw invalid("MCP prompt role is invalid");
            }
            text = Objects.requireNonNull(text, "text");
            if (text.length() > 512 * 1024) {
                throw invalid("MCP prompt message is too large");
            }
        }
    }

    record Result(String description, List<Message> messages) {

        public Result {
            description = description == null ? "" : description.trim();
            messages = List.copyOf(Objects.requireNonNull(
                    messages,
                    "messages"
            ));
            if (messages.isEmpty() || messages.size() > 64) {
                throw invalid("MCP prompt messages are invalid");
            }
        }
    }

    static McpProtocolException invalid(String message) {
        return new McpProtocolException(
                McpErrorCode.MCP_INVALID_PARAMS,
                message
        );
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid("MCP prompt " + field + " is required");
        }
        return value.trim();
    }
}
