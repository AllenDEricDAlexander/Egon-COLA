package top.egon.cola.component.gateway.mcp.completion;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public final class DictionaryCompletionProvider
        implements McpCompletionProvider {

    private final Map<Key, List<String>> dictionaries;

    public DictionaryCompletionProvider(Map<Key, List<String>> dictionaries) {
        java.util.LinkedHashMap<Key, List<String>> copy =
                new java.util.LinkedHashMap<>();
        java.util.Objects.requireNonNull(
                dictionaries,
                "dictionaries"
        ).forEach((key, value) -> copy.put(key, List.copyOf(value)));
        this.dictionaries = Map.copyOf(copy);
    }

    @Override
    public String sourceType() {
        return "LOCAL_DICTIONARY";
    }

    @Override
    public Mono<Result> complete(Request request) {
        if (McpCompletionProvider.sensitiveArgumentName(
                request.argumentName()
        )) {
            return Mono.just(new Result(List.of(), 0, false));
        }
        List<String> values = dictionaries.getOrDefault(
                        new Key(
                                request.referenceType(),
                                request.referenceName(),
                                request.argumentName()
                        ),
                        List.of()
                ).stream()
                .filter(value -> value != null && value.length() <= 256)
                .filter(value -> value.startsWith(request.valuePrefix()))
                .filter(value -> !McpCompletionProvider.sensitiveValue(value))
                .distinct()
                .sorted()
                .toList();
        return Mono.just(new Result(
                values.stream().limit(100).toList(),
                values.size(),
                values.size() > 100
        ));
    }

    public record Key(
            String referenceType,
            String referenceName,
            String argumentName
    ) {
    }
}
