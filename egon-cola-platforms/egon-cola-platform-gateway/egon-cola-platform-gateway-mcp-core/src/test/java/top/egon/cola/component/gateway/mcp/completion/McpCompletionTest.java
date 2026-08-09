package top.egon.cola.component.gateway.mcp.completion;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class McpCompletionTest {

    @Test
    void completionIsStableDeduplicatedSafeAndLimited() {
        ArrayList<String> configured = new ArrayList<>();
        for (int index = 149; index >= 0; index--) {
            configured.add("value-" + String.format("%03d", index));
        }
        configured.add("value-001");
        configured.add("secret://database-password");
        DictionaryCompletionProvider provider =
                new DictionaryCompletionProvider(Map.of(
                        new DictionaryCompletionProvider.Key(
                                "ref/prompt",
                                "review",
                                "subject"
                        ),
                        configured
                ));

        McpCompletionProvider.Result result = Mono.from(provider.complete(
                new McpCompletionProvider.Request(
                        "finance",
                        "ref/prompt",
                        "review",
                        "subject",
                        "value-",
                        null,
                        Map.of()
                )
        )).block();

        assertEquals(100, result.values().size());
        assertEquals(
                result.values().stream().sorted().toList(),
                result.values()
        );
        assertEquals(150, result.total());
        assertEquals(true, result.hasMore());
        assertFalse(result.values().stream().anyMatch(
                value -> value.contains("secret")
        ));
    }

    @Test
    void sensitiveArgumentNamesAreNeverEnumerated() {
        DictionaryCompletionProvider provider =
                new DictionaryCompletionProvider(Map.of(
                        new DictionaryCompletionProvider.Key(
                                "ref/prompt",
                                "review",
                                "apiToken"
                        ),
                        List.of("token-value")
                ));

        McpCompletionProvider.Result result = Mono.from(provider.complete(
                new McpCompletionProvider.Request(
                        "finance",
                        "ref/prompt",
                        "review",
                        "apiToken",
                        "",
                        null,
                        Map.of()
                )
        )).block();

        assertEquals(List.of(), result.values());
    }
}
